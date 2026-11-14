package com.example.eoffice.repository;

import com.example.eoffice.entity.TaskPost;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskPostRepository extends JpaRepository<TaskPost, Long> {
    @EntityGraph(attributePaths = {"author", "attachments"})
    List<TaskPost> findByTaskIdOrderByCreatedAtDesc(Long taskId);

    Optional<TaskPost> findFirstByTaskIdAndProgressPercentIsNotNullOrderByCreatedAtDesc(Long taskId);
}
