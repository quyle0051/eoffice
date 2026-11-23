package com.example.eoffice.repository;

import com.example.eoffice.entity.IncomingDocument;
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

public interface IncomingDocumentRepository extends JpaRepository<IncomingDocument, Long> {

    @EntityGraph(attributePaths = {"documentBook", "documentType", "currentHandler"})
    @Query("""
            select d from IncomingDocument d
            left join d.currentHandler ch
            where d.deleted = false
              and (:viewerEmail is null
                   or ch.email = :viewerEmail
                   or exists (select 1 from d.participants p where p.email = :viewerEmail))
              and (:bookId is null or d.documentBook.id = :bookId)
              and (:typeId is null or d.documentType.id = :typeId)
              and (:fromDate is null or d.arrivalDate >= :fromDate)
              and (:toDate is null or d.arrivalDate <= :toDate)
              and (:keyword is null
                   or lower(d.subject) like :keyword
                   or lower(d.codeNumber) like :keyword
                   or lower(d.codeNotation) like :keyword
                   or lower(d.organName) like :keyword)
            order by d.arrivalDate desc, d.id desc
            """)
    Page<IncomingDocument> search(@Param("keyword") String keyword,
                                  @Param("bookId") Long bookId,
                                  @Param("typeId") Long typeId,
                                  @Param("fromDate") LocalDate fromDate,
                                  @Param("toDate") LocalDate toDate,
                                  @Param("viewerEmail") String viewerEmail,
                                  Pageable pageable);

    @EntityGraph(attributePaths = {"documentBook", "documentType", "currentHandler", "participants"})
    Optional<IncomingDocument> findByIdAndDeletedFalse(Long id);

    /** Dùng để dựng lại index Elasticsearch theo từng lô. */
    Page<IncomingDocument> findByDeletedFalse(Pageable pageable);

    /** Nạp đầy đủ các văn bản theo danh sách ID (kết quả tìm từ Elasticsearch). */
    @EntityGraph(attributePaths = {"documentBook", "documentType", "currentHandler"})
    List<IncomingDocument> findAllByIdInAndDeletedFalse(Collection<Long> ids);

    @Query("""
            select max(d.arrivalNumber) from IncomingDocument d
            where d.documentBook.id = :bookId
              and d.arrivalDate >= :fromDate
              and d.arrivalDate <= :toDate
            """)
    Long findMaxArrivalNumber(@Param("bookId") Long bookId,
                              @Param("fromDate") LocalDate fromDate,
                              @Param("toDate") LocalDate toDate);
}