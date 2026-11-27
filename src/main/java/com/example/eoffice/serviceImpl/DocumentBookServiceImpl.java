package com.example.eoffice.serviceImpl;

import com.example.eoffice.dto.DocumentBookRequest;
import com.example.eoffice.dto.DocumentBookResponse;
import com.example.eoffice.entity.DocumentBook;
import com.example.eoffice.entity.DocumentBookType;
import com.example.eoffice.repository.*;
import com.example.eoffice.service.DocumentBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DocumentBookServiceImpl implements DocumentBookService {

    private final DocumentBookRepository documentBookRepository;
    private final DocumentBookTypeRepository bookTypeRepository;
    private final IncomingDocumentRepository incomingRepository;
    private final OutgoingDocumentRepository outgoingRepository;
    private final InternalDocumentRepository internalRepository;

    //lấy danh sách
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "documentBooks",
            key = "#typeCode == null || #typeCode.isBlank() ? 'ALL' : #typeCode.trim().toUpperCase()")
    public List<DocumentBookResponse> findAll(String typeCode) {
        List<DocumentBook> books;
        if (typeCode == null || typeCode.isBlank()) {
            books = documentBookRepository.findAllByDeletedFalseOrderByIdAsc();
        } else {
            books = documentBookRepository
                    .findAllByBookTypeCodeAndDeletedFalseOrderByIdAsc(typeCode.trim().toUpperCase());
        }
        return books.stream().map(this::toResponse).toList();
    }

    //lấy sổ theo ID
    @Override
    @Transactional(readOnly = true)
    public DocumentBookResponse findById(Long id) {
        DocumentBook book = findBookOrThrow(id);
        return toResponse(book);
    }

    @Override
    @Transactional
    @CacheEvict(value = "documentBooks", allEntries = true)
    public DocumentBookResponse create(DocumentBookRequest request) {
        DocumentBook book = new DocumentBook();
        copyFromRequest(book, request);
        DocumentBook savedBook = documentBookRepository.save(book);
        return toResponse(savedBook);
    }

    @Override
    @Transactional
    @CacheEvict(value = "documentBooks", allEntries = true)
    public DocumentBookResponse update(Long id, DocumentBookRequest request) {
        DocumentBook book = findBookOrThrow(id);
        copyFromRequest(book, request);
        DocumentBook savedBook = documentBookRepository.save(book);
        return toResponse(savedBook);
    }

    //Xóa sổ (xóa mềm)
    @Override
    @Transactional
    @CacheEvict(value = "documentBooks", allEntries = true)
    public void delete(Long id) {
        DocumentBook book = findBookOrThrow(id);
        boolean hasChildren = documentBookRepository.existsByParentIdAndDeletedFalse(id);
        if (hasChildren) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Không thể xóa: sổ này còn có sổ con");
        }
        if (hasDocuments(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không thể xóa: sổ này đã có văn bản");
        }
        book.setDeleted(true);
        documentBookRepository.save(book);
    }

    private boolean hasDocuments(Long bookId) {
        return incomingRepository.existsByDocumentBookIdAndDeletedFalse(bookId)
                || outgoingRepository.existsByDocumentBookIdAndDeletedFalse(bookId)
                || internalRepository.existsByDocumentBookIdAndDeletedFalse(bookId);
    }

    private DocumentBookResponse toResponse(DocumentBook documentBook) {
        Long parentId = null;
        String parentName = null;

        if(documentBook.getParent() != null) {
            parentId = documentBook.getParent().getId();
            parentName = documentBook.getParent().getName();
        }
        DocumentBookType type = documentBook.getBookType();
        return new DocumentBookResponse(
                documentBook.getId(),
                documentBook.getName(),
                documentBook.getShortName(),
                parentId,
                parentName,
                type != null ? type.getId() : null,
                type != null ? type.getCode() : null,
                type != null ? type.getName() : null
        );
    }

    //tìm sổ chưa bị xóa theo id
    private DocumentBook findBookOrThrow(Long id) {
        Optional<DocumentBook> foundBook = documentBookRepository.findByIdAndDeletedFalse(id);
        if(foundBook.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sổ văn bản");
        }
        return foundBook.get();
    }

    //chép dữ liệu từ request vào sổ
    private void copyFromRequest (DocumentBook book, DocumentBookRequest request){
        book.setName(request.getName().trim());
        String shortName = request.getShortName();
        if (shortName == null || shortName.isBlank()) {
            book.setShortName(null);
        }else  {
            book.setShortName(shortName.trim());
        }

        DocumentBookType type = bookTypeRepository.findById(request.getBookTypeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại sổ không tồn tại"));
        if (book.getId() != null && book.getBookType() != null
                && !book.getBookType().getId().equals(type.getId())) {
            if (documentBookRepository.existsByParentIdAndDeletedFalse(book.getId()) || hasDocuments(book.getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Không thể đổi loại sổ vì sổ này đã có sổ con hoặc văn bản");
            }
        }
        //Sổ cha
        book.setBookType(type);
        book.setParent(findParent(book, request.getParentId(), type));
    }

    //Tìm sổ cha theo parentId
    private DocumentBook findParent(DocumentBook book, Long parentId, DocumentBookType type) {
        if(parentId == null) {
            return null;
        }

        Optional<DocumentBook> foundBook = documentBookRepository.findByIdAndDeletedFalse(parentId);
        if(foundBook.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Sổ cha không tồn tại");
        }
        DocumentBook parent = foundBook.get();

        if (book.getId() != null){
            DocumentBook current = parent;
            while (current != null) {
                if(current.getId().equals(book.getId())) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sổ cha không được là chính nó hoặc sổ con của nó");
                }
                current = current.getParent();
            }
        }
        return parent;
    }
}
