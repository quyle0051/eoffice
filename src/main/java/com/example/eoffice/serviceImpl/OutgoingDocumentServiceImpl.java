package com.example.eoffice.serviceImpl;

import com.example.eoffice.dto.OutgoingDocumentRequest;
import com.example.eoffice.dto.OutgoingDocumentResponse;
import com.example.eoffice.dto.OutgoingSearchRequest;
import com.example.eoffice.dto.PageResponse;
import com.example.eoffice.entity.*;
import com.example.eoffice.repository.*;
import com.example.eoffice.search.OutgoingDocumentChangedEvent;
import com.example.eoffice.search.OutgoingSearchService;
import com.example.eoffice.service.OutgoingDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutgoingDocumentServiceImpl implements OutgoingDocumentService {

    private final OutgoingDocumentRepository outgoingRepository;
    private final DocumentBookRepository bookRepository;
    private final DocumentTypeRepository typeRepository;
    private final OrganRepository organRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher publisher;
    private final OutgoingSearchService searchService;

    private final Path uploadDir = Paths.get("uploads/outgoing");


    @Override
    @Transactional(readOnly = true)
    public PageResponse<OutgoingDocumentResponse> search(OutgoingSearchRequest req) {
        try {
            // Ưu tiên Elasticsearch: tìm toàn văn, bỏ dấu tiếng Việt, xếp theo độ liên quan
            OutgoingSearchService.IdPage found = searchService.search(req);
            Map<Long, OutgoingDocument> byId = outgoingRepository.findAllByIdInAndDeletedFalse(found.ids()).stream()
                    .collect(Collectors.toMap(OutgoingDocument::getId, d -> d));
            List<OutgoingDocumentResponse> content = found.ids().stream()
                    .map(byId::get)
                    .filter(Objects::nonNull)
                    .map(this::toResponse)
                    .toList();
            int size = Math.min(Math.max(req.getSize(), 1), 100);
            int totalPages = (int) Math.ceil(found.total() / (double) size);
            return new PageResponse<>(content, Math.max(req.getPage(), 0), size, found.total(), totalPages);
        } catch (Exception e) {
            // Elasticsearch không dùng được thì quay về tìm kiếm cơ bản bằng MySQL
            log.warn("Elasticsearch lỗi, chuyển sang tìm kiếm MySQL: {}", e.getMessage());
            return searchInMySql(req);
        }
    }

    public PageResponse<OutgoingDocumentResponse> searchInMySql(OutgoingSearchRequest req) {
        String kw = (req.getKeyword() != null && !req.getKeyword().trim().isEmpty()) ? "%" + req.getKeyword().trim().toLowerCase() + "%" : null;

        OutgoingDocumentStatus statusEnum = null;
        if (req.getStatus() != null && !req.getStatus().trim().isEmpty()) {
            try {
                statusEnum = OutgoingDocumentStatus.valueOf(req.getStatus().trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        Pageable pageable = PageRequest.of(Math.max(req.getPage(), 0), req.getSize() <= 0 ? 10 : Math.min(req.getSize(), 100));
        Page<OutgoingDocument> p = outgoingRepository.search(kw, req.getBookId(), req.getTypeId(), statusEnum, req.getFromDate(), req.getToDate(), pageable);

        List<OutgoingDocumentResponse> content = p.getContent().stream().map(this::toResponse).toList();
        return new PageResponse<>(content, p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public OutgoingDocumentResponse findById(Long id) {
        OutgoingDocument doc = outgoingRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy văn bản đi"));
        return toResponse(doc);
    }

    /** Chức năng 2: Tự động gợi ý Số văn bản đi tiếp theo & Ký hiệu văn bản theo Sổ và Loại văn bản */
    @Override
    @Transactional(readOnly = true)
    public Map<String, String> suggestNextNumberAndNotation(Long bookId, Long typeId) {
        long count = (bookId != null) ? outgoingRepository.countByDocumentBookIdAndDeletedFalse(bookId) : 0;
        String nextCodeNumber = String.format("%02d", count + 1);

        String typeShort = "CV";
        if (typeId != null) {
            typeShort = typeRepository.findById(typeId)
                    .map(t -> t.getAbbreviation() != null && !t.getAbbreviation().isBlank() ? t.getAbbreviation().trim().toUpperCase() : "VB")
                    .orElse("VB");
        }

        String suggestedNotation = "/" + typeShort + "-EOFFICE";
        return Map.of(
                "codeNumber", nextCodeNumber,
                "codeNotation", suggestedNotation
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Organ> getAllOrgans() {
        return organRepository.findAllByDeletedFalseOrderByOrganNameAsc();
    }

    /** Chức năng 3: Đăng ký, cấp số, thời gian ban hành và lưu danh sách nơi nhận văn bản đi */
    @Override
    @Transactional
    public OutgoingDocumentResponse create(OutgoingDocumentRequest req, String email) {
        DocumentBook book = bookRepository.findById(req.getDocumentBookId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sổ văn bản đi không tồn tại"));
        DocumentType type = typeRepository.findById(req.getDocumentTypeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại văn bản không tồn tại"));
        User creator = userRepository.findByEmail(email).orElse(null);

        OutgoingDocument doc = new OutgoingDocument();
        applyRequestToEntity(doc, req, book, type);
        doc.setCreatedBy(creator);

        if (req.isIssueImmediately()) {
            doc.setStatus(OutgoingDocumentStatus.ISSUED);
            doc.setIssuedTime(LocalDateTime.now());
            doc.getRecipients().forEach(r -> {
                r.setDeliveryStatus("SENT");
                r.setSentAt(LocalDateTime.now());
            });
        } else {
            doc.setStatus(OutgoingDocumentStatus.REGISTERED);
            doc.setIssuedTime(LocalDateTime.now());
        }

        OutgoingDocument saved = outgoingRepository.save(doc);
        publisher.publishEvent(new OutgoingDocumentChangedEvent(saved.getId()));
        return toResponse(saved);
    }

    @Override
    @Transactional
    public OutgoingDocumentResponse update(Long id, OutgoingDocumentRequest req) {
        OutgoingDocument doc = outgoingRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy văn bản đi"));
        DocumentBook book = bookRepository.findById(req.getDocumentBookId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sổ văn bản đi không tồn tại"));
        DocumentType type = typeRepository.findById(req.getDocumentTypeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại văn bản không tồn tại"));

        applyRequestToEntity(doc, req, book, type);
        if (req.isIssueImmediately() && doc.getStatus() == OutgoingDocumentStatus.REGISTERED) {
            doc.setStatus(OutgoingDocumentStatus.ISSUED);
            doc.setIssuedTime(LocalDateTime.now());
            doc.getRecipients().forEach(r -> {
                if ("PENDING".equals(r.getDeliveryStatus())) {
                    r.setDeliveryStatus("SENT");
                    r.setSentAt(LocalDateTime.now());
                }
            });
        }

        return saveAndNotify(doc);
    }

    @Override
    @Transactional
    public OutgoingDocumentResponse uploadAttachment(Long id, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn tệp đính kèm");
        }
        OutgoingDocument doc = outgoingRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy văn bản đi"));

        try {
            Files.createDirectories(uploadDir);
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "outgoing.pdf";
            String storedName = doc.getId() + "_" + UUID.randomUUID() + "_" + originalName;
            Path target = uploadDir.resolve(storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            doc.setAttachmentName(originalName);
            doc.setAttachmentPath(target.toString());
            return saveAndNotify(doc);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lưu tệp văn bản đi");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OutgoingDocument getDocumentForDownload(Long id) {
        return outgoingRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy văn bản đi"));
    }

    /** Phát hành văn bản đi ra các cơ quan bên ngoài */
    @Override
    @Transactional
    public OutgoingDocumentResponse issueDocument(Long id) {
        OutgoingDocument doc = outgoingRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy văn bản đi"));

        doc.setStatus(OutgoingDocumentStatus.ISSUED);
        doc.setIssuedTime(LocalDateTime.now());
        if (doc.getRecipients() != null) {
            for (OutgoingDocumentRecipient r : doc.getRecipients()) {
                if ("PENDING".equalsIgnoreCase(r.getDeliveryStatus())) {
                    r.setDeliveryStatus("SENT");
                    r.setSentAt(LocalDateTime.now());
                }
            }
        }
        return toResponse(outgoingRepository.save(doc));
    }

    /** Cập nhật trạng thái theo dõi phát hành của từng cơ quan nhận (SENT / RECEIVED / FAILED) */
    @Override
    @Transactional
    public OutgoingDocumentResponse updateRecipientStatus(Long id, Long recipientId, String deliveryStatus, String trackingNote) {
        OutgoingDocument doc = outgoingRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy văn bản đi"));

        if (doc.getRecipients() != null) {
            doc.getRecipients().stream()
                    .filter(r -> r.getId().equals(recipientId))
                    .findFirst()
                    .ifPresent(r -> {
                        if (deliveryStatus != null && !deliveryStatus.isBlank()) {
                            r.setDeliveryStatus(deliveryStatus.trim().toUpperCase());
                        }
                        if (trackingNote != null) {
                            r.setTrackingNote(trackingNote.trim());
                        }
                        if ("SENT".equalsIgnoreCase(r.getDeliveryStatus()) && r.getSentAt() == null) {
                            r.setSentAt(LocalDateTime.now());
                        }
                    });
        }
        return saveAndNotify(doc);
    }

    /** Lưu trữ văn bản đi */
    @Override
    @Transactional
    public OutgoingDocumentResponse archiveDocument(Long id) {
        OutgoingDocument doc = outgoingRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy văn bản đi"));
        doc.setStatus(OutgoingDocumentStatus.ARCHIVED);
        return toResponse(outgoingRepository.save(doc));
    }

    @Override
    @Transactional
    public void softDelete(Long id) {
        OutgoingDocument doc = outgoingRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy văn bản đi"));
        doc.setDeleted(true);
        outgoingRepository.save(doc);
        publisher.publishEvent(new OutgoingDocumentChangedEvent(doc.getId()));
    }

    /** Lưu văn bản, báo cho bộ đồng bộ Elasticsearch rồi trả về dữ liệu hiển thị. */
    private OutgoingDocumentResponse saveAndNotify(OutgoingDocument doc) {
        OutgoingDocument saved = outgoingRepository.save(doc);
        publisher.publishEvent(new OutgoingDocumentChangedEvent(saved.getId()));
        return toResponse(saved);
    }
    // --- HÀM HELPER ÁNH XẠ DỮ LIỆU ---

    private void applyRequestToEntity(OutgoingDocument doc, OutgoingDocumentRequest req, DocumentBook book, DocumentType type) {
        doc.setDocumentBook(book);
        doc.setDocumentType(type);
        doc.setCodeNumber(req.getCodeNumber().trim());
        doc.setCodeNotation(req.getCodeNotation().trim());
        doc.setIssuedDate(req.getIssuedDate());
        doc.setOrganName(req.getOrganName().trim());
        doc.setSubject(req.getSubject().trim());
        doc.setLanguage(req.getLanguage() != null && !req.getLanguage().isBlank() ? req.getLanguage().trim() : "Tiếng Việt");
        doc.setPageAmount(req.getPageAmount() != null && req.getPageAmount() > 0 ? req.getPageAmount() : 1);
        doc.setDescription(req.getDescription());
        doc.setSignerPosition(req.getSignerPosition());
        doc.setSignerFullName(req.getSignerFullName());
        doc.setPriority(req.getPriority() != null ? req.getPriority() : 0);
        doc.setDueDate(req.getDueDate());

        // Xử lý danh sách cơ quan nhận văn bản (Mục 13)
        doc.getRecipients().clear();
        List<String> summaryNames = new ArrayList<>();
        int totalCopies = 0;

        if (req.getRecipients() != null) {
            for (OutgoingDocumentRequest.RecipientItem item : req.getRecipients()) {
                if (item.getOrganName() == null || item.getOrganName().isBlank()) continue;

                OutgoingDocumentRecipient rec = new OutgoingDocumentRecipient();
                rec.setOutgoingDocument(doc);
                String orgId = (item.getOrganId() != null && !item.getOrganId().isBlank())
                        ? item.getOrganId().trim()
                        : "000.00.00.000";
                if (orgId.length() > 13) {
                    orgId = orgId.substring(0, 13);
                }
                rec.setOrganId(orgId);

                String orgName = item.getOrganName().trim();
                if (orgName.length() > 200) {
                    orgName = orgName.substring(0, 200);
                }
                rec.setOrganName(orgName);

                int copies = (item.getCopies() != null && item.getCopies() > 0) ? item.getCopies() : 1;
                rec.setCopies(copies);
                rec.setDeliveryMethod(item.getDeliveryMethod() != null ? item.getDeliveryMethod() : "TRUC_LIEN_THONG");
                rec.setDeliveryStatus("PENDING");
                rec.setTrackingNote(item.getTrackingNote());

                doc.getRecipients().add(rec);
                summaryNames.add(orgName + " (" + orgId + ")");
                totalCopies += copies;
            }
        }

        String summary = String.join("; ", summaryNames);
        if (summary.length() > 1000) {
            summary = summary.substring(0, 997) + "...";
        }
        doc.setToOrgansSummary(summary);

        if (req.getIssuedAmount() != null && req.getIssuedAmount() > 0) {
            doc.setIssuedAmount(req.getIssuedAmount());
        } else {
            doc.setIssuedAmount(Math.max(totalCopies, 1));
        }
    }

    private OutgoingDocumentResponse toResponse(OutgoingDocument d) {
        List<OutgoingDocumentResponse.RecipientDto> recDtos = (d.getRecipients() == null) ? List.of() :
                d.getRecipients().stream()
                        .map(r -> new OutgoingDocumentResponse.RecipientDto(
                                r.getId(),
                                r.getOrganId(),
                                r.getOrganName(),
                                r.getCopies(),
                                r.getDeliveryMethod(),
                                r.getDeliveryStatus(),
                                r.getSentAt(),
                                r.getTrackingNote()
                        )).toList();

        return OutgoingDocumentResponse.builder()
                .id(d.getId())
                .documentBookId(d.getDocumentBook() != null ? d.getDocumentBook().getId() : null)
                .documentBookName(d.getDocumentBook() != null ? d.getDocumentBook().getName() : null)
                .documentTypeId(d.getDocumentType() != null ? d.getDocumentType().getId() : null)
                .documentTypeName(d.getDocumentType() != null ? d.getDocumentType().getName() : null)
                .codeNumber(d.getCodeNumber())
                .codeNotation(d.getCodeNotation())
                .issuedDate(d.getIssuedDate())
                .organName(d.getOrganName())
                .subject(d.getSubject())
                .language(d.getLanguage())
                .pageAmount(d.getPageAmount())
                .description(d.getDescription())
                .signerPosition(d.getSignerPosition())
                .signerFullName(d.getSignerFullName())
                .toOrgansSummary(d.getToOrgansSummary())
                .recipients(recDtos)
                .priority(d.getPriority())
                .issuedAmount(d.getIssuedAmount())
                .dueDate(d.getDueDate())
                .issuedTime(d.getIssuedTime())
                .status(d.getStatus() != null ? d.getStatus().name() : "REGISTERED")
                .attachmentName(d.getAttachmentName())
                .createdByName(d.getCreatedBy() != null ? d.getCreatedBy().getFullName() : "Văn thư")
                .createdAt(d.getCreatedAt())
                .build();
    }
}