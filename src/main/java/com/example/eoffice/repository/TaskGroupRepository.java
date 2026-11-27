package com.example.eoffice.repository;

import com.example.eoffice.entity.TaskGroup;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaskGroupRepository extends JpaRepository<TaskGroup, Long> {
    @EntityGraph(attributePaths = {"members", "creator"})
    List<TaskGroup> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = {"members", "creator"})
    Optional<TaskGroup> findWithMembersById(Long id);

    /** Các nhóm mà người dùng là thành viên. */
    @EntityGraph(attributePaths = {"members", "creator"})
    @Query("""
            select g from TaskGroup g
            where exists (select 1 from g.members m where m.email = :email)
            order by g.id
            """)
    List<TaskGroup> findByMemberEmail(@Param("email") String email);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
