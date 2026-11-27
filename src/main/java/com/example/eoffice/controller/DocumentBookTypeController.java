package com.example.eoffice.controller;

import com.example.eoffice.dto.DocumentBookTypeResponse;
import com.example.eoffice.service.DocumentBookTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/document-book-types")
@RequiredArgsConstructor
public class DocumentBookTypeController {

    private final DocumentBookTypeService service;

    @GetMapping
    public List<DocumentBookTypeResponse> findAll() {
        return service.findAll();
    }
}