package com.example.eoffice.service;

import com.example.eoffice.dto.IncomingDocumentRequest;
import com.example.eoffice.dto.IncomingDocumentResponse;
import com.example.eoffice.dto.PageResponse;

import java.time.LocalDate;

public interface IncomingDocumentService {
    PageResponse<IncomingDocumentResponse> search(String keyword, Long documentBookId, Long documentTypeId,
                                                  LocalDate arrivalFrom, LocalDate arrivalTo,
                                                  int page, int size);

    IncomingDocumentResponse findById(Long id);

    IncomingDocumentResponse create(IncomingDocumentRequest request);

    IncomingDocumentResponse update(Long id, IncomingDocumentRequest request);

    void delete(Long id);
}
