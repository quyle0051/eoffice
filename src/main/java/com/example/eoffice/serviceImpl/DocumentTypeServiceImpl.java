package com.example.eoffice.serviceImpl;

import com.example.eoffice.dto.DocumentTypeResponse;
import com.example.eoffice.entity.DocumentType;
import com.example.eoffice.repository.DocumentTypeRepository;
import com.example.eoffice.service.DocumentTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentTypeServiceImpl implements DocumentTypeService {

    private final DocumentTypeRepository repository;

    @Override
    @Cacheable("documentTypes")
    public List<DocumentTypeResponse> findAll() {
        List<DocumentType> types = repository.findAllByOrderByIdAsc();

        List<DocumentTypeResponse> result = new ArrayList<>();
        for (DocumentType type : types) {
            result.add(new DocumentTypeResponse(type.getId(), type.getName(), type.getAbbreviation(), type.getCategory()));
        }
        return result;
    }
}
