package com.example.eoffice.serviceImpl;

import com.example.eoffice.dto.DocumentBookTypeResponse;
import com.example.eoffice.repository.DocumentBookTypeRepository;
import com.example.eoffice.service.DocumentBookTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentBookTypeServiceImpl implements DocumentBookTypeService {

    private final DocumentBookTypeRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<DocumentBookTypeResponse> findAll() {
        return repository.findAllByOrderByIdAsc().stream()
                .map(t -> new DocumentBookTypeResponse(t.getId(), t.getCode(), t.getName()))
                .toList();
    }
}