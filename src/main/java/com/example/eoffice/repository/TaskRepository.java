package com.example.eoffice.repository;

import com.example.eoffice.entity.Task;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    @EntityGraph(attributePaths = {"taskGroup", "creator", "assignee", "supervisor", "supporter"})
    List<Task> findByIncomingDocumentIdOrderByIdDesc(Long incomingDocumentId);
}
