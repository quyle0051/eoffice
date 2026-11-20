package com.example.eoffice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "outgoing_document_recipients")
@Getter
@Setter
@NoArgsConstructor
public class OutgoingDocumentRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outgoing_document_id", nullable = false)
    private OutgoingDocument outgoingDocument;

    /** 13.1 Mã định danh của cơ quan, tổ chức nhận văn bản (String 13) */
    @Column(name = "organ_id", nullable = false, length = 13)
    private String organId;

    /** 13.2 Tên cơ quan, tổ chức nhận văn bản (String 200) */
    @Column(name = "organ_name", nullable = false, length = 200)
    private String organName;

    @Column(nullable = false)
    private Integer copies = 1;

    /** TRUC_LIEN_THONG | EMAIL | BUU_DIEN | TRUC_TIEP */
    @Column(name = "delivery_method", nullable = false, length = 50)
    private String deliveryMethod = "TRUC_LIEN_THONG";

    /** PENDING (Chờ phát hành) | SENT (Đã gửi) | RECEIVED (Đã nhận) | FAILED (Gửi lỗi) */
    @Column(name = "delivery_status", nullable = false, length = 30)
    private String deliveryStatus = "PENDING";

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "tracking_note", length = 300)
    private String trackingNote;
}