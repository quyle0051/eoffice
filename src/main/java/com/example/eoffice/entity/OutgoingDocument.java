package com.example.eoffice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "outgoing_documents")
@Getter
@Setter
@NoArgsConstructor
public class OutgoingDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_book_id", nullable = false)
    private DocumentBook documentBook;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_type_id", nullable = false)
    private DocumentType documentType;

    @Column(name = "code_number", nullable = false, length = 11)
    private String codeNumber;

    @Column(name = "code_notation", nullable = false, length = 30)
    private String codeNotation;

    @Column(name = "issued_date", nullable = false)
    private LocalDate issuedDate;

    @Column(name = "organ_name", nullable = false, length = 200)
    private String organName;

    @Column(nullable = false, length = 500)
    private String subject;

    @Column(length = 30)
    private String language = "Tiếng Việt";

    @Column(name = "page_amount")
    private Integer pageAmount = 1;

    @Column(length = 500)
    private String description;

    // --- Mục 12: Người ký văn bản ---
    @Column(name = "signer_position", length = 100)
    private String signerPosition;

    @Column(name = "signer_full_name", length = 50)
    private String signerFullName;

    // --- Mục 13: Nơi nhận ---
    @Column(name = "to_organs_summary", length = 1000)
    private String toOrgansSummary;

    @OneToMany(mappedBy = "outgoingDocument", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OutgoingDocumentRecipient> recipients = new ArrayList<>();

    // --- Mục 14, 15, 16: Độ khẩn, Số bản phát hành, Hạn trả lời ---
    @Column(nullable = false)
    private Integer priority = 0;

    @Column(name = "issued_amount")
    private Integer issuedAmount = 1;

    @Column(name = "due_date")
    private LocalDate dueDate;

    // --- Thông tin quản lý phát hành & tệp đính kèm ---
    @Column(name = "issued_time")
    private LocalDateTime issuedTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OutgoingDocumentStatus status = OutgoingDocumentStatus.REGISTERED;

    @Column(name = "attachment_name", length = 255)
    private String attachmentName;

    @Column(name = "attachment_path", length = 500)
    private String attachmentPath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;
}