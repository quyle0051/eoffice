package com.example.eoffice.repository;

import com.example.eoffice.entity.Organ;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrganRepository extends JpaRepository<Organ, Long> {
    List<Organ> findAllByDeletedFalseOrderByOrganNameAsc();
}