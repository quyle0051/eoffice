package com.example.eoffice.repository;

import com.example.eoffice.entity.DocumentBook;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentBookRepository extends JpaRepository<DocumentBook, Long> {
    @EntityGraph(attributePaths = {"parent", "bookType"})
    List<DocumentBook> findAllByDeletedFalseOrderByIdAsc();

    @EntityGraph(attributePaths = {"parent", "bookType"})
    List<DocumentBook> findAllByBookTypeCodeAndDeletedFalseOrderByIdAsc(String code);

    @EntityGraph(attributePaths = {"parent", "bookType"})
    Optional<DocumentBook> findByIdAndDeletedFalse(Long id);

    boolean existsByParentIdAndDeletedFalse(Long parentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from DocumentBook b where b.id = :id and b.deleted = false")
    Optional<DocumentBook> findActiveByIdForUpdate(@Param("id") Long id);
}
