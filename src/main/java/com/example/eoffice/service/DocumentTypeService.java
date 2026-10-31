package com.example.eoffice.service;

import com.example.eoffice.dto.DocumentTypeResponse;

import java.util.List;

public interface DocumentTypeService {
    List<DocumentTypeResponse> findAll();
}
