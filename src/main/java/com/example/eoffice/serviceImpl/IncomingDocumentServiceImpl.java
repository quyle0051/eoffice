package com.example.eoffice.serviceImpl;


import com.example.eoffice.dto.IncomingDocumentRequest;
import com.example.eoffice.dto.IncomingDocumentResponse;
import com.example.eoffice.dto.PageResponse;
import com.example.eoffice.entity.DocumentBook;
import com.example.eoffice.entity.DocumentType;
import com.example.eoffice.entity.IncomingDocument;
import com.example.eoffice.repository.DocumentBookRepository;
import com.example.eoffice.repository.DocumentTypeRepository;
import com.example.eoffice.repository.IncomingDocumentRepository;
import com.example.eoffice.service.IncomingDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IncomingDocumentServiceImpl implements IncomingDocumentService {

    private static final String DEFAULT_LANGUAGE = "Tiếng Việt";

    private final IncomingDocumentRepository repository;
    private final DocumentBookRepository bookRepository;
    private final DocumentTypeRepository typeRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<IncomingDocumentResponse> search(String keyword, Long documentBookId, Long documentTypeId, LocalDate arrivalFrom, LocalDate arrivalTo, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = (size <= 0) ? 10 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        String searchPattern = null;
        String cleaned = cleanText(keyword);
        if (cleaned != null) {
            searchPattern = "%" + cleaned.toLowerCase() + "%";
        }
        String viewerEmail = resolveRestrictedViewerEmail();

        Page<IncomingDocument> docPage = repository.search(
                searchPattern,
                documentBookId,
                documentTypeId,
                arrivalFrom,
                arrivalTo,
                viewerEmail,
                pageable
        );

        List<IncomingDocumentResponse> content = docPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return new PageResponse<>(
                content,
                docPage.getNumber(),
                docPage.getSize(),
                docPage.getTotalElements(),
                docPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public IncomingDocumentResponse findById(Long id) {
        IncomingDocument document = findDocOrThrow(id);
        String viewerEmail = resolveRestrictedViewerEmail();
        if (viewerEmail != null && !canUserViewDocument(document, viewerEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem văn bản này");
        }
        return toResponse(document);
    }

    private String resolveRestrictedViewerEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        boolean isAdminOrClerk = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_CLERK"));
        return isAdminOrClerk ? null : auth.getName();
    }

    private boolean canUserViewDocument(IncomingDocument doc, String email) {
        if (doc.getCurrentHandler() != null && email.equalsIgnoreCase(doc.getCurrentHandler().getEmail())) {
            return true;
        }
        return doc.getParticipants() != null && doc.getParticipants().stream()
                .anyMatch(u -> email.equalsIgnoreCase(u.getEmail()));
    }

    @Override
    @Transactional
    public IncomingDocumentResponse create(IncomingDocumentRequest request) {
        DocumentBook book = findBookForUpdateOrThrow(request.getDocumentBookId());
        DocumentType type = findTypeOrThrow(request.getDocumentTypeId());

        IncomingDocument doc = new IncomingDocument();
        doc.setDocumentBook(book);
        doc.setDocumentType(type);
        copyFromRequest(doc, request);
        checkDates(doc);

        doc.setArrivalNumber(nextArrivalNumber(book.getId(), doc.getArrivalDate()));


        if (doc.getDueDate() == null && book.getStandardDays() != null) {
            doc.setDueDate(doc.getArrivalDate().plusDays(book.getStandardDays()));
        }

        IncomingDocument saved = repository.save(doc);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public IncomingDocumentResponse update(Long id, IncomingDocumentRequest request) {
        IncomingDocument doc = findDocOrThrow(id);

        DocumentBook oldBook = doc.getDocumentBook();
        if (oldBook != null && !oldBook.getId().equals(request.getDocumentBookId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Không được đổi sổ văn bản của văn bản đã đăng ký");
        }

        LocalDate oldArrivalDate = doc.getArrivalDate();
        LocalDate newArrivalDate = request.getArrivalDate();
        if (newArrivalDate != null && oldArrivalDate != null
                && newArrivalDate.getYear() != oldArrivalDate.getYear()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Không được đổi ngày đến sang năm khác");
        }

        doc.setDocumentType(findTypeOrThrow(request.getDocumentTypeId()));
        copyFromRequest(doc, request);
        checkDates(doc);

        IncomingDocument saved = repository.save(doc);
        return toResponse(saved);
    }

    @Override
    public void delete(Long id) {
        IncomingDocument doc = findDocOrThrow(id);

        doc.setDeleted(true);
        repository.save(doc);
    }

    private IncomingDocument findDocOrThrow(Long id) {
        Optional<IncomingDocument> found = repository.findByIdAndDeletedFalse(id);
        if (found.isEmpty()){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy văn bản đến");
        }
        return found.get();
    }

    private IncomingDocumentResponse toResponse(IncomingDocument incomingDocument) {
        Long bookId = null;
        String bookName = null;
        if (incomingDocument.getDocumentBook() != null) {
            bookId = incomingDocument.getDocumentBook().getId();
            bookName = incomingDocument.getDocumentBook().getName();
        }

        Long typeId = null;
        String typeName = null;
        String typeAbbreviation = null;
        if (incomingDocument.getDocumentType() != null) {
            typeId = incomingDocument.getDocumentType().getId();
            typeName = incomingDocument.getDocumentType().getName();
            typeAbbreviation = incomingDocument.getDocumentType().getAbbreviation();
        }

        Long handlerId = null;
        String handlerEmail = null;
        String handlerName = null;
        if (incomingDocument.getCurrentHandler() != null) {
            handlerId = incomingDocument.getCurrentHandler().getId();
            handlerEmail = incomingDocument.getCurrentHandler().getEmail();
            handlerName = incomingDocument.getCurrentHandler().getFullName();
        }

        return IncomingDocumentResponse.builder()
                .id(incomingDocument.getId())
                .documentBookId(bookId)
                .documentBookName(bookName)
                .documentTypeId(typeId)
                .documentTypeName(typeName)
                .documentTypeAbbreviation(typeAbbreviation)
                .arrivalNumber(incomingDocument.getArrivalNumber())
                .arrivalDate(incomingDocument.getArrivalDate())
                .codeNumber(incomingDocument.getCodeNumber())
                .codeNotation(incomingDocument.getCodeNotation())
                .issuedDate(incomingDocument.getIssuedDate())
                .organName(incomingDocument.getOrganName())
                .subject(incomingDocument.getSubject())
                .language(incomingDocument.getLanguage())
                .pageAmount(incomingDocument.getPageAmount())
                .docOrdinal(incomingDocument.getDocOrdinal())
                .description(incomingDocument.getDescription())
                .signerPosition(incomingDocument.getSignerPosition())
                .signerFullName(incomingDocument.getSignerFullName())
                .priority(incomingDocument.getPriority())
                .toPlaces(incomingDocument.getToPlaces())
                .traceHeaderList(incomingDocument.getTraceHeaderList())
                .dueDate(incomingDocument.getDueDate())
                .status(incomingDocument.getStatus() != null ? incomingDocument.getStatus().name() : "NEW")
                .attachmentName(incomingDocument.getAttachmentName())
                .currentHandlerId(handlerId)
                .currentHandlerEmail(handlerEmail)
                .currentHandlerName(handlerName)
                .build();
    }

    private DocumentBook findBookForUpdateOrThrow(Long bookId) {
        Optional<DocumentBook> found = bookRepository.findActiveByIdForUpdate(bookId);
        if (found.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Sổ văn bản không tồn tại hoặc đã bị xóa");
        }
        return found.get();
    }

    private DocumentType findTypeOrThrow(Long typeId) {
        Optional<DocumentType> found = typeRepository.findById(typeId);
        if (found.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại văn bản không tồn tại");
        }
        return found.get();
    }


    private void copyFromRequest(IncomingDocument doc, IncomingDocumentRequest request) {
        doc.setSubject(request.getSubject().trim());
        doc.setCodeNumber(cleanText(request.getCodeNumber()));
        doc.setCodeNotation(cleanText(request.getCodeNotation()));
        doc.setIssuedDate(request.getIssuedDate());
        doc.setOrganName(cleanText(request.getOrganName()));
        doc.setPageAmount(request.getPageAmount());
        doc.setDocOrdinal(request.getDocOrdinal());
        doc.setDescription(cleanText(request.getDescription()));
        doc.setSignerPosition(cleanText(request.getSignerPosition()));
        doc.setSignerFullName(cleanText(request.getSignerFullName()));
        doc.setPriority(request.getPriority());
        doc.setToPlaces(cleanText(request.getToPlaces()));
        doc.setTraceHeaderList(request.getTraceHeaderList());
        doc.setDueDate(request.getDueDate());

        String language = cleanText(request.getLanguage());
        if (language == null) {
            doc.setLanguage(DEFAULT_LANGUAGE);
        } else {
            doc.setLanguage(language);
        }

        if (request.getArrivalDate() != null) {
            doc.setArrivalDate(request.getArrivalDate());
        } else if (doc.getArrivalDate() == null) {
            doc.setArrivalDate(LocalDate.now());
        }
    }

    private void checkDates(IncomingDocument doc) {
        LocalDate arrival = doc.getArrivalDate();

        if (doc.getIssuedDate() != null && doc.getIssuedDate().isAfter(arrival)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ngày ban hành không được sau ngày đến");
        }
        if (doc.getDueDate() != null && doc.getDueDate().isBefore(arrival)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Hạn giải quyết không được trước ngày đến");
        }
    }

    private Long nextArrivalNumber(Long bookId, LocalDate arrivalDate) {
        LocalDate startOfYear = LocalDate.of(arrivalDate.getYear(), 1, 1);
        LocalDate endOfYear = LocalDate.of(arrivalDate.getYear(), 12, 31);

        Long max = repository.findMaxArrivalNumber(bookId, startOfYear, endOfYear);
        if (max == null) {
            return 1L;
        }
        return max + 1;
    }

    private String cleanText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.trim();
    }
}
