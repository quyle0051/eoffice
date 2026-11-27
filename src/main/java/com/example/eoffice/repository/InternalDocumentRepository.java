package com.example.eoffice.repository;

import com.example.eoffice.entity.InternalDocument;
import com.example.eoffice.entity.InternalDocumentStatus;
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

public interface InternalDocumentRepository extends JpaRepository<InternalDocument, Long> {

    @EntityGraph(attributePaths = {"documentBook", "documentType", "createdBy"})
    Optional<InternalDocument> findByIdAndDeletedFalse(Long id);

    /** Dùng để dựng lại index Elasticsearch theo từng lô. */
    Page<InternalDocument> findByDeletedFalse(Pageable pageable);

    /** Nạp đầy đủ các văn bản theo danh sách ID (kết quả tìm từ Elasticsearch). */
    @EntityGraph(attributePaths = {"documentBook", "documentType", "createdBy"})
    List<InternalDocument> findAllByIdInAndDeletedFalse(Collection<Long> ids);

    boolean existsByDocumentBookIdAndDeletedFalse(Long bookId);

    /** Số thứ tự lớn nhất trong sổ và năm (tính cả văn bản đã xóa mềm để số không bị cấp lại). */
    @Query("""
            select max(d.serialNumber) from InternalDocument d
            where d.documentBook.id = :bookId
              and d.issuedDate >= :fromDate
              and d.issuedDate <= :toDate
            """)
    Long findMaxSerial(@Param("bookId") Long bookId,
                       @Param("fromDate") LocalDate fromDate,
                       @Param("toDate") LocalDate toDate);

    /** Phương án dự phòng khi Elasticsearch không dùng được. */
    @EntityGraph(attributePaths = {"documentBook", "documentType", "createdBy"})
    @Query("""
            select d from InternalDocument d
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
                   or lower(d.signerFullName) like :kw)
            order by d.issuedDate desc, d.id desc
            """)
    Page<InternalDocument> search(@Param("kw") String keyword,
                                  @Param("bookId") Long bookId,
                                  @Param("typeId") Long typeId,
                                  @Param("status") InternalDocumentStatus status,
                                  @Param("fromDate") LocalDate fromDate,
                                  @Param("toDate") LocalDate toDate,
                                  Pageable pageable);
}