package com.example.eoffice.repository;

import com.example.eoffice.entity.OutgoingDocument;
import com.example.eoffice.entity.OutgoingDocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OutgoingDocumentRepository extends JpaRepository<OutgoingDocument, Long> {

    @EntityGraph(attributePaths = {"documentBook", "documentType", "createdBy", "recipients"})
    Optional<OutgoingDocument> findByIdAndDeletedFalse(Long id);

    /** Đếm số lượng văn bản đi trong cùng 1 Sổ văn bản đi để tự động gợi ý Số văn bản đi tiếp theo */
    long countByDocumentBookIdAndDeletedFalse(Long documentBookId);

    /** Dùng để dựng lại index Elasticsearch theo từng lô. */
    Page<OutgoingDocument> findByDeletedFalse(Pageable pageable);

    /** Nạp đầy đủ các văn bản theo danh sách ID (kết quả tìm từ Elasticsearch). */
    @EntityGraph(attributePaths = {"documentBook", "documentType", "createdBy"})
    List<OutgoingDocument> findAllByIdInAndDeletedFalse(Collection<Long> ids);

    @EntityGraph(attributePaths = {"documentBook", "documentType", "createdBy"})
    @Query("""
            select d from OutgoingDocument d
            where d.deleted = false
              and (:bookId is null or d.documentBook.id = :bookId)
              and (:typeId is null or d.documentType.id = :typeId)
              and (:status is null or d.status = :status)
              and (:fromDate is null or d.issuedDate >= :fromDate)
              and (:toDate is null or d.issuedDate <= :toDate)
              and (:kw is null
                   or lower(d.subject) like :kw
                   or lower(d.codeNumber) like :kw
                   or lower(d.codeNotation) like :kw
                   or lower(d.toOrgansSummary) like :kw
                   or lower(d.signerFullName) like :kw)
            order by d.issuedDate desc, d.id desc
            """)
    Page<OutgoingDocument> search(@Param("kw") String keyword,
                                  @Param("bookId") Long bookId,
                                  @Param("typeId") Long typeId,
                                  @Param("status") OutgoingDocumentStatus status,
                                  @Param("fromDate") LocalDate fromDate,
                                  @Param("toDate") LocalDate toDate,
                                  Pageable pageable);
}