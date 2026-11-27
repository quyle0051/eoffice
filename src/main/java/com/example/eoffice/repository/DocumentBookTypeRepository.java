package com.example.eoffice.repository;

import com.example.eoffice.entity.DocumentBookType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentBookTypeRepository extends JpaRepository<DocumentBookType, Long> {
    List<DocumentBookType> findAllByOrderByIdAsc();

    Optional<DocumentBookType> findByCode(String code);
}