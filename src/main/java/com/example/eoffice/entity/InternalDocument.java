package com.example.eoffice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Văn bản nội bộ: do chính cơ quan ban hành và gửi cho chính cơ quan đó,
 * nên không có danh sách cơ quan nhận bên ngoài như văn bản đi.
 */
@Entity
@Table(name = "internal_documents", indexes = {
        @Index(name = "idx_internal_issued_date", columnList = "issued_date"),
        @Index(name = "idx_internal_status", columnList = "status"),
        @Index(name = "idx_internal_book_issued", columnList = "document_book_id, issued_date")
})
@Getter
@Setter
@NoArgsConstructor
public class InternalDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_book_id", nullable = false)
    private DocumentBook documentBook;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_type_id", nullable = false)
    private DocumentType documentType;

    /** Số thứ tự của văn bản trong sổ, tính lại từ 1 mỗi năm. Hệ thống tự cấp. */
    @Column(name = "serial_number", nullable = false)
    private Long serialNumber;

    @Column(name = "code_number", nullable = false, length = 11)
    private String codeNumber;

    @Column(name = "code_notation", nullable = false, length = 30)
    private String codeNotation;

    @Column(name = "issued_date", nullable = false)
    private LocalDate issuedDate;

    /** Cơ quan ban hành, chính là cơ quan hiện tại. */
    @Column(name = "organ_name", nullable = false, length = 200)
    private String organName;

    /** Nơi nhận, cũng chính là cơ quan hiện tại. */
    @Column(name = "to_places", length = 1000)
    private String toPlaces;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(length = 30)
    private String language = "Tiếng Việt";

    @Column(name = "page_amount")
    private Integer pageAmount = 1;

    @Column(length = 500)
    private String description;

    @Column(name = "signer_position", length = 100)
    private String signerPosition;

    @Column(name = "signer_full_name", length = 50)
    private String signerFullName;

    @Column(nullable = false)
    private Integer priority = 0;

    @Column(name = "issued_amount")
    private Integer issuedAmount = 1;

    @Column(name = "due_date")
    private LocalDate dueDate;

    /** Thời điểm ban hành (chỉ có khi đã ban hành). */
    @Column(name = "issued_time")
    private LocalDateTime issuedTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InternalDocumentStatus status = InternalDocumentStatus.REGISTERED;

    @Column(name = "attachment_name", length = 255)
    private String attachmentName;

    @Column(name = "attachment_path", length = 500)
    private String attachmentPath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}