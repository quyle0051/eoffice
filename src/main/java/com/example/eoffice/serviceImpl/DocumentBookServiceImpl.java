package com.example.eoffice.serviceImpl;

import com.example.eoffice.dto.DocumentBookRequest;
import com.example.eoffice.dto.DocumentBookResponse;
import com.example.eoffice.entity.DocumentBook;
import com.example.eoffice.repository.DocumentBookRepository;
import com.example.eoffice.service.DocumentBookService;
import lombok.RequiredArgsConstructor;
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

    //lấy danh sách
    @Override
    @Transactional(readOnly = true)
    public List<DocumentBookResponse> findAll() {
        //lay cac so chua bi xoa
        List<DocumentBook> books = documentBookRepository.findAllByDeletedFalseOrderByIdAsc();

        //đổi sang dạng trả về cho angular
        List<DocumentBookResponse> result = new ArrayList<>();
        for (DocumentBook book : books) {
            result.add(toResponse(book));
        }
        return result;
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
    public DocumentBookResponse create(DocumentBookRequest request) {
        DocumentBook book = new DocumentBook();
        copyFromRequest(book, request);
        DocumentBook savedBook = documentBookRepository.save(book);
        return toResponse(savedBook);
    }

    @Override
    @Transactional
    public DocumentBookResponse update(Long id, DocumentBookRequest request) {
        DocumentBook book = findBookOrThrow(id);
        copyFromRequest(book, request);
        DocumentBook savedBook = documentBookRepository.save(book);
        return toResponse(savedBook);
    }

    //Xóa sổ (xóa mềm)
    @Override
    @Transactional
    public void delete(Long id) {
        DocumentBook book = findBookOrThrow(id);
        boolean hasChildren = documentBookRepository.existsByParentIdAndDeletedFalse(id);
        if (hasChildren) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Không thể xóa: sổ này còn có sổ con");
        }

        book.setDeleted(true);
        documentBookRepository.save(book);
    }

    private DocumentBookResponse toResponse(DocumentBook documentBook) {
        Long parentId = null;
        String parentName = null;

        if(documentBook.getParent() != null) {
            parentId = documentBook.getParent().getId();
            parentName = documentBook.getParent().getName();
        }

        return new DocumentBookResponse(
                documentBook.getId(),
                documentBook.getName(),
                documentBook.getShortName(),
                documentBook.getStandardDays(),
                parentId,
                parentName
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

        book.setStandardDays(request.getStandardDays());

        //Sổ cha
        DocumentBook parent =findParent(book, request.getParentId());
        book.setParent(parent);
    }

    //Tìm sổ cha theo parentId
    private DocumentBook findParent(DocumentBook book, Long parentId) {
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
