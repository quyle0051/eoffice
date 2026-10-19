package com.example.eoffice.service;

import com.example.eoffice.dto.DocumentBookRequest;
import com.example.eoffice.dto.DocumentBookResponse;

import java.util.List;

public interface DocumentBookService {
    List<DocumentBookResponse> findAll();
    DocumentBookResponse findById(Long id);
    DocumentBookResponse create(DocumentBookRequest request);
    DocumentBookResponse update(Long id, DocumentBookRequest request);
    void delete(Long id);
}
