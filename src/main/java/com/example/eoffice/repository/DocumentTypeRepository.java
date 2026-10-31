package com.example.eoffice.repository;

import com.example.eoffice.entity.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentTypeRepository extends JpaRepository<DocumentType, Long> {
    List<DocumentType> findAllByOrderByIdAsc();
}
