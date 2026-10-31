package com.example.eoffice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "incoming_documents")
@Getter
@Setter
@NoArgsConstructor
public class IncomingDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_book_id")
    private DocumentBook documentBook;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_type_id")
    private DocumentType documentType;

    /** Số thứ tự văn bản trong hồ sơ */
    @Column(name = "doc_ordinal")
    private Integer docOrdinal;

    /** Số của văn bản (do cơ quan gửi đặt) */
    @Column(name = "code_number", length = 11)
    private String codeNumber;

    /** Ký hiệu của văn bản, ví dụ "123/UBND-VP" */
    @Column(name = "code_notation", length = 30)
    private String codeNotation;

    /** Ngày ban hành */
    @Column(name = "issued_date")
    private LocalDate issuedDate;

    /** Tên cơ quan ban hành */
    @Column(name = "organ_name", length = 200)
    private String organName;

    /** Trích yếu nội dung */
    @Column(length = 500)
    private String subject;

    @Column(length = 30)
    private String language;

    @Column(name = "page_amount")
    private Integer pageAmount;

    /** Ghi chú */
    @Column(length = 500)
    private String description;

    /** Ngày đến */
    @Column(name = "arrival_date")
    private LocalDate arrivalDate;

    /** Số đến: hệ thống tự đánh số theo từng sổ, từng năm */
    @Column(name = "arrival_number")
    private Long arrivalNumber;

    @Column(name = "signer_position", length = 100)
    private String signerPosition;

    @Column(name = "signer_full_name", length = 50)
    private String signerFullName;

    /** Mức độ khẩn, mật */
    @Column(name = "priority")
    private Integer priority;

    /** Đơn vị hoặc người nhận */
    @Column(name = "to_places", length = 1000)
    private String toPlaces;

    /** Ý kiến phân phối, chỉ đạo, trạng thái xử lý */
    @Column(name = "trace_header_list", columnDefinition = "LONGTEXT")
    private String traceHeaderList;

    /** Hạn giải quyết */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /** Xóa mềm: true = đã xóa (bị ẩn) */
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private IncomingDocumentStatus status = IncomingDocumentStatus.NEW;

    @Column(name = "attachment_name", length = 255)
    private String attachmentName;

    @Column(name = "attachment_path", length = 500)
    private String attachmentPath;

    /** Người đang được chọn để cho lời phê / xử lý ở bước hiện tại */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_handler_id")
    private User currentHandler;

    /** Danh sách những người có quyền xem văn bản này (ngoài Văn thư & Admin) */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "incoming_document_participants",
            joinColumns = @JoinColumn(name = "incoming_document_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> participants = new HashSet<>();
}
