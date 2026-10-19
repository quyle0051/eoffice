package com.example.eoffice.controller;

import com.example.eoffice.dto.DocumentBookRequest;
import com.example.eoffice.dto.DocumentBookResponse;
import com.example.eoffice.entity.DocumentBook;
import com.example.eoffice.serviceImpl.DocumentBookServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/document-books")
@RequiredArgsConstructor
public class DocumentBookController {

    private final DocumentBookServiceImpl  service;

    @GetMapping
    public List<DocumentBookResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public DocumentBookResponse findById(@PathVariable("id") Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentBookResponse create(@Valid @RequestBody DocumentBookRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public DocumentBookResponse update(@PathVariable("id") Long id, @Valid @RequestBody DocumentBookRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        service.delete(id);
    }
}
