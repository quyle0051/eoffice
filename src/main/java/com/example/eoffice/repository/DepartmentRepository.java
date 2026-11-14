package com.example.eoffice.repository;

import com.example.eoffice.entity.Department;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    @EntityGraph(attributePaths = {"head", "deputies", "clerks"})
    List<Department> findAllByOrderByIdAsc();
}
