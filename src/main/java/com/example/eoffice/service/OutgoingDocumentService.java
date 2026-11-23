package com.example.eoffice.service;

import com.example.eoffice.dto.OutgoingDocumentRequest;
import com.example.eoffice.dto.OutgoingDocumentResponse;
import com.example.eoffice.dto.OutgoingSearchRequest;
import com.example.eoffice.dto.PageResponse;
import com.example.eoffice.entity.Organ;
import com.example.eoffice.entity.OutgoingDocument;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface OutgoingDocumentService {
    PageResponse<OutgoingDocumentResponse> search(OutgoingSearchRequest request);

    OutgoingDocumentResponse findById(Long id);

    Map<String, String> suggestNextNumberAndNotation(Long bookId, Long typeId);

    List<Organ> getAllOrgans();

    OutgoingDocumentResponse create(OutgoingDocumentRequest req, String email);

    OutgoingDocumentResponse update(Long id, OutgoingDocumentRequest req);

    OutgoingDocumentResponse uploadAttachment(Long id, MultipartFile file);

    OutgoingDocument getDocumentForDownload(Long id);

    OutgoingDocumentResponse issueDocument(Long id);

    OutgoingDocumentResponse updateRecipientStatus(Long id, Long recipientId, String deliveryStatus, String trackingNote);

    OutgoingDocumentResponse archiveDocument(Long id);

    void softDelete(Long id);
}