package com.example.eoffice.serviceImpl;

import com.example.eoffice.dto.InternalDocumentRequest;
import com.example.eoffice.dto.InternalDocumentResponse;
import com.example.eoffice.dto.InternalSearchRequest;
import com.example.eoffice.dto.PageResponse;
import com.example.eoffice.entity.DocumentBook;
import com.example.eoffice.entity.DocumentType;
import com.example.eoffice.entity.InternalDocument;
import com.example.eoffice.entity.InternalDocumentStatus;
import com.example.eoffice.entity.User;
import com.example.eoffice.repository.DocumentBookRepository;
import com.example.eoffice.repository.DocumentTypeRepository;
import com.example.eoffice.repository.InternalDocumentRepository;
import com.example.eoffice.repository.UserRepository;
import com.example.eoffice.search.InternalDocumentChangedEvent;
import com.example.eoffice.search.InternalSearchService;
import com.example.eoffice.service.InternalDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InternalDocumentServiceImpl implements InternalDocumentService {

    private static final String NOT_FOUND = "Không tìm thấy văn bản nội bộ";

    private final InternalDocumentRepository repository;
    private final DocumentBookRepository bookRepository;
    private final DocumentTypeRepository typeRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher publisher;
    private final InternalSearchService searchService;

    /** Tên cơ quan hiện tại: vừa là cơ quan ban hành, vừa là nơi nhận của văn bản nội bộ. */
    @Value("${eoffice.organization-name:Văn phòng Điện tử E-Office}")
    private String organizationName;

    private final Path uploadDir = Paths.get("uploads/internal");

    // ------------------------------------------------------------------ Tìm kiếm

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InternalDocumentResponse> search(InternalSearchRequest req) {
        try {
            // Ưu tiên Elasticsearch: tìm toàn văn, bỏ dấu tiếng Việt, xếp theo độ liên quan
            InternalSearchService.IdPage found = searchService.search(req);
            Map<Long, InternalDocument> byId = repository.findAllByIdInAndDeletedFalse(found.ids()).stream()
                    .collect(Collectors.toMap(InternalDocument::getId, d -> d));
            List<InternalDocumentResponse> content = found.ids().stream()
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

    /** Phương án dự phòng: chỉ hỗ trợ từ khóa, sổ, loại, trạng thái và khoảng ngày ban hành. */
    private PageResponse<InternalDocumentResponse> searchInMySql(InternalSearchRequest req) {
        String kw = (req.getKeyword() != null && !req.getKeyword().trim().isEmpty())
                ? "%" + req.getKeyword().trim().toLowerCase() + "%" : null;

        InternalDocumentStatus statusEnum = null;
        if (req.getStatus() != null && !req.getStatus().trim().isEmpty()) {
            try {
                statusEnum = InternalDocumentStatus.valueOf(req.getStatus().trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // trạng thái không hợp lệ thì bỏ qua điều kiện này
            }
        }

        Pageable pageable = PageRequest.of(Math.max(req.getPage(), 0),
                req.getSize() <= 0 ? 10 : Math.min(req.getSize(), 100));
        Page<InternalDocument> p = repository.search(kw, req.getBookId(), req.getTypeId(),
                statusEnum, req.getFromDate(), req.getToDate(), pageable);

        List<InternalDocumentResponse> content = p.getContent().stream().map(this::toResponse).toList();
        return new PageResponse<>(content, p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }

    // ------------------------------------------------------------------ Đọc

    @Override
    @Transactional(readOnly = true)
    public InternalDocumentResponse findById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getNextInfo(Long bookId, Long typeId, LocalDate issuedDate) {
        LocalDate date = issuedDate != null ? issuedDate : LocalDate.now();
        long next = (bookId != null) ? nextSerial(bookId, date) : 1;

        String abbreviation = "CV";
        if (typeId != null) {
            abbreviation = typeRepository.findById(typeId)
                    .map(t -> t.getAbbreviation() != null && !t.getAbbreviation().isBlank()
                            ? t.getAbbreviation().trim().toUpperCase() : "VB")
                    .orElse("VB");
        }

        Map<String, String> info = new LinkedHashMap<>();
        info.put("codeNumber", String.valueOf(next));
        info.put("codeNotation", "/" + abbreviation + "-NB");
        info.put("organName", organizationName);
        return info;
    }

    // ------------------------------------------------------------------ Ghi

    @Override
    @Transactional
    public InternalDocumentResponse create(InternalDocumentRequest req, String email) {
        // Khóa dòng của sổ trước tiên (phải là truy vấn đầu tiên của giao dịch) để hai người
        // đăng ký cùng lúc không bao giờ cùng nhận một số.
        DocumentBook book = bookRepository.findActiveByIdForUpdate(req.getDocumentBookId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Sổ văn bản không tồn tại hoặc đã bị xóa"));
        DocumentType type = findType(req.getDocumentTypeId());
        User creator = (email != null) ? userRepository.findByEmail(email).orElse(null) : null;

        InternalDocument doc = new InternalDocument();
        applyRequest(doc, req, book, type);
        checkDates(doc);

        long serial = nextSerial(book.getId(), doc.getIssuedDate());
        doc.setSerialNumber(serial);
        doc.setCodeNumber(String.valueOf(serial));
        doc.setCreatedBy(creator);

        if (req.isIssueImmediately()) {
            doc.setStatus(InternalDocumentStatus.ISSUED);
            doc.setIssuedTime(LocalDateTime.now());
        } else {
            doc.setStatus(InternalDocumentStatus.REGISTERED);
        }
        return saveAndNotify(doc);
    }

    @Override
    @Transactional
    public InternalDocumentResponse update(Long id, InternalDocumentRequest req) {
        InternalDocument doc = findOrThrow(id);
        if (doc.getStatus() != InternalDocumentStatus.REGISTERED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Văn bản đã ban hành hoặc đã lưu trữ, không thể sửa");
        }
        if (!doc.getDocumentBook().getId().equals(req.getDocumentBookId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Không được đổi sổ văn bản của văn bản đã cấp số");
        }
        if (doc.getIssuedDate().getYear() != req.getIssuedDate().getYear()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Không được đổi ngày ban hành sang năm khác");
        }

        applyRequest(doc, req, doc.getDocumentBook(), findType(req.getDocumentTypeId()));
        checkDates(doc);

        if (req.isIssueImmediately()) {
            doc.setStatus(InternalDocumentStatus.ISSUED);
            doc.setIssuedTime(LocalDateTime.now());
        }
        return saveAndNotify(doc);
    }

    @Override
    @Transactional
    public InternalDocumentResponse uploadAttachment(Long id, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn tệp đính kèm");
        }
        InternalDocument doc = findOrThrow(id);

        try {
            Files.createDirectories(uploadDir);

            // Tên gốc chỉ để hiển thị. Tên lưu trên đĩa là UUID nên không thể thoát ra ngoài thư mục upload.
            String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment";
            original = original.replaceAll(".*[\\\\/]", "");
            if (original.isBlank()) {
                original = "attachment";
            }
            if (original.length() > 255) {
                original = original.substring(original.length() - 255);
            }
            String extension = "";
            int dot = original.lastIndexOf('.');
            if (dot >= 0 && dot < original.length() - 1) {
                String ext = original.substring(dot + 1).toLowerCase();
                if (ext.matches("[a-z0-9]{1,10}")) {
                    extension = "." + ext;
                }
            }
            Path target = uploadDir.resolve(doc.getId() + "_" + UUID.randomUUID() + extension).normalize();
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            // Xóa tệp cũ để không để lại tệp mồ côi
            if (doc.getAttachmentPath() != null) {
                try {
                    Files.deleteIfExists(Paths.get(doc.getAttachmentPath()));
                } catch (IOException ignored) {
                    // không xóa được tệp cũ cũng không ảnh hưởng đến việc lưu tệp mới
                }
            }

            doc.setAttachmentName(original);
            doc.setAttachmentPath(target.toString());
            return saveAndNotify(doc);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lưu tệp văn bản nội bộ");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InternalDocument getDocumentForDownload(Long id) {
        return findOrThrow(id);
    }

    @Override
    @Transactional
    public InternalDocumentResponse issueDocument(Long id) {
        InternalDocument doc = findOrThrow(id);
        if (doc.getStatus() != InternalDocumentStatus.REGISTERED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ văn bản đã đăng ký mới được ban hành");
        }
        doc.setStatus(InternalDocumentStatus.ISSUED);
        doc.setIssuedTime(LocalDateTime.now());
        return saveAndNotify(doc);
    }

    @Override
    @Transactional
    public InternalDocumentResponse archiveDocument(Long id) {
        InternalDocument doc = findOrThrow(id);
        if (doc.getStatus() != InternalDocumentStatus.ISSUED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ văn bản đã ban hành mới được lưu trữ");
        }
        doc.setStatus(InternalDocumentStatus.ARCHIVED);
        return saveAndNotify(doc);
    }

    @Override
    @Transactional
    public void softDelete(Long id) {
        InternalDocument doc = findOrThrow(id);
        if (doc.getStatus() != InternalDocumentStatus.REGISTERED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Văn bản đã ban hành hoặc đã lưu trữ, không thể xóa");
        }
        doc.setDeleted(true);
        repository.save(doc);
        publisher.publishEvent(new InternalDocumentChangedEvent(doc.getId()));
    }

    // ------------------------------------------------------------------ Hàm hỗ trợ

    private InternalDocument findOrThrow(Long id) {
        return repository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, NOT_FOUND));
    }

    private DocumentType findType(Long typeId) {
        return typeRepository.findById(typeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại văn bản không tồn tại"));
    }

    private long nextSerial(Long bookId, LocalDate date) {
        Long max = repository.findMaxSerial(bookId,
                LocalDate.of(date.getYear(), 1, 1), LocalDate.of(date.getYear(), 12, 31));
        return (max == null) ? 1L : max + 1;
    }

    /** Lưu văn bản, báo cho bộ đồng bộ Elasticsearch rồi trả về dữ liệu hiển thị. */
    private InternalDocumentResponse saveAndNotify(InternalDocument doc) {
        InternalDocument saved = repository.save(doc);
        publisher.publishEvent(new InternalDocumentChangedEvent(saved.getId()));
        return toResponse(saved);
    }

    private void applyRequest(InternalDocument doc, InternalDocumentRequest req, DocumentBook book, DocumentType type) {
        doc.setDocumentBook(book);
        doc.setDocumentType(type);
        doc.setCodeNotation(req.getCodeNotation().trim());
        doc.setIssuedDate(req.getIssuedDate());
        // Văn bản nội bộ: cơ quan ban hành và nơi nhận đều là chính cơ quan hiện tại
        doc.setOrganName(organizationName);
        doc.setToPlaces(organizationName);
        doc.setSubject(req.getSubject().trim());
        doc.setLanguage(clean(req.getLanguage()) != null ? clean(req.getLanguage()) : "Tiếng Việt");
        doc.setPageAmount(req.getPageAmount() != null && req.getPageAmount() > 0 ? req.getPageAmount() : 1);
        doc.setDescription(clean(req.getDescription()));
        doc.setSignerPosition(clean(req.getSignerPosition()));
        doc.setSignerFullName(clean(req.getSignerFullName()));
        doc.setPriority(req.getPriority() != null ? req.getPriority() : 0);
        doc.setIssuedAmount(req.getIssuedAmount() != null && req.getIssuedAmount() > 0 ? req.getIssuedAmount() : 1);
        doc.setDueDate(req.getDueDate());
    }

    private void checkDates(InternalDocument doc) {
        if (doc.getDueDate() != null && doc.getDueDate().isBefore(doc.getIssuedDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Hạn xử lý không được trước ngày ban hành");
        }
    }

    private static String clean(String text) {
        return (text == null || text.isBlank()) ? null : text.trim();
    }

    private InternalDocumentResponse toResponse(InternalDocument d) {
        DocumentBook book = d.getDocumentBook();
        DocumentType type = d.getDocumentType();
        return InternalDocumentResponse.builder()
                .id(d.getId())
                .documentBookId(book != null ? book.getId() : null)
                .documentBookName(book != null ? book.getName() : null)
                .documentTypeId(type != null ? type.getId() : null)
                .documentTypeName(type != null ? type.getName() : null)
                .documentTypeAbbreviation(type != null ? type.getAbbreviation() : null)
                .serialNumber(d.getSerialNumber())
                .codeNumber(d.getCodeNumber())
                .codeNotation(d.getCodeNotation())
                .issuedDate(d.getIssuedDate())
                .organName(d.getOrganName())
                .toPlaces(d.getToPlaces())
                .subject(d.getSubject())
                .language(d.getLanguage())
                .pageAmount(d.getPageAmount())
                .description(d.getDescription())
                .signerPosition(d.getSignerPosition())
                .signerFullName(d.getSignerFullName())
                .priority(d.getPriority())
                .issuedAmount(d.getIssuedAmount())
                .dueDate(d.getDueDate())
                .issuedTime(d.getIssuedTime())
                .status(d.getStatus() != null ? d.getStatus().name() : "REGISTERED")
                .attachmentName(d.getAttachmentName())
                .createdByName(d.getCreatedBy() != null ? d.getCreatedBy().getFullName() : null)
                .createdAt(d.getCreatedAt())
                .build();
    }
}