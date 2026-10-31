package com.example.eoffice.controller;


import com.example.eoffice.dto.DocumentTypeResponse;
import com.example.eoffice.service.DocumentTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/document-types")
@RequiredArgsConstructor
public class DocumentTypeController {

    private final DocumentTypeService service;

    @GetMapping
    public List<DocumentTypeResponse> findAll() {
        return service.findAll();
    }
}
