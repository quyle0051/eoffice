package com.example.eoffice.service;

import com.example.eoffice.dto.DocumentBookTypeResponse;

import java.util.List;

public interface DocumentBookTypeService {
    List<DocumentBookTypeResponse> findAll();
}