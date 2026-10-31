package com.example.eoffice.repository;

import com.example.eoffice.entity.TaskGroup;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskGroupRepository extends JpaRepository<TaskGroup, Long> {
    @EntityGraph(attributePaths = {"members"})
    List<TaskGroup> findAllByOrderByIdAsc();
}
