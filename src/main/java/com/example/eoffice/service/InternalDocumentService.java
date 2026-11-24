package com.example.eoffice.service;

import com.example.eoffice.dto.InternalDocumentRequest;
import com.example.eoffice.dto.InternalDocumentResponse;
import com.example.eoffice.dto.InternalSearchRequest;
import com.example.eoffice.dto.PageResponse;
import com.example.eoffice.entity.InternalDocument;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Map;

public interface InternalDocumentService {

    PageResponse<InternalDocumentResponse> search(InternalSearchRequest request);

    InternalDocumentResponse findById(Long id);

    /** Gợi ý số văn bản, ký hiệu và cơ quan ban hành (cơ quan hiện tại) khi chọn sổ và loại văn bản. */
    Map<String, String> getNextInfo(Long bookId, Long typeId, LocalDate issuedDate);

    InternalDocumentResponse create(InternalDocumentRequest request, String email);

    InternalDocumentResponse update(Long id, InternalDocumentRequest request);

    InternalDocumentResponse uploadAttachment(Long id, MultipartFile file);

    InternalDocument getDocumentForDownload(Long id);

    InternalDocumentResponse issueDocument(Long id);

    InternalDocumentResponse archiveDocument(Long id);

    void softDelete(Long id);
}