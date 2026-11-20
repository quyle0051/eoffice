package com.example.eoffice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class OutgoingDocumentRequest {

    @NotNull(message = "Vui lòng chọn Sổ văn bản đi")
    private Long documentBookId;

    @NotNull(message = "Vui lòng chọn Loại văn bản")
    private Long documentTypeId;

    @NotBlank(message = "Số của văn bản không được để trống")
    @Size(max = 11, message = "Số của văn bản tối đa 11 ký tự")
    private String codeNumber;

    @NotBlank(message = "Ký hiệu văn bản không được để trống")
    @Size(max = 30, message = "Ký hiệu của văn bản tối đa 30 ký tự")
    private String codeNotation;

    @NotNull(message = "Ngày ban hành văn bản không được để trống")
    private LocalDate issuedDate;

    @NotBlank(message = "Tên cơ quan ban hành không được để trống")
    @Size(max = 200)
    private String organName;

    @NotBlank(message = "Trích yếu nội dung không được để trống")
    @Size(max = 500)
    private String subject;

    @Size(max = 30)
    private String language = "Tiếng Việt";

    private Integer pageAmount = 1;

    @Size(max = 500)
    private String description;

    // Mục 12: Người ký
    @Size(max = 100)
    private String signerPosition;

    @Size(max = 50)
    private String signerFullName;

    // Mục 13: Danh sách cơ quan nhận văn bản (OrganId + OrganName)
    private List<RecipientItem> recipients;

    // Mục 14, 15, 16: Độ khẩn, Số lượng bản phát hành, Hạn trả lời
    private Integer priority = 0;
    private Integer issuedAmount = 1;
    private LocalDate dueDate;

    /** Nếu Văn thư chọn "Đăng ký & Phát hành ngay" */
    private boolean issueImmediately = false;

    @Getter
    @Setter
    public static class RecipientItem {
        private String organId;        // 13.1 String(13)
        private String organName;      // 13.2 String(200)
        private Integer copies = 1;
        private String deliveryMethod = "TRUC_LIEN_THONG";
        private String trackingNote;
    }
}