package com.example.eoffice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Số văn bản, cơ quan ban hành và nơi nhận do hệ thống tự điền nên không có trong request. */
@Getter
@Setter
public class InternalDocumentRequest {

    @NotNull(message = "Vui lòng chọn sổ văn bản")
    private Long documentBookId;

    @NotNull(message = "Vui lòng chọn loại văn bản")
    private Long documentTypeId;

    @NotBlank(message = "Ký hiệu văn bản không được để trống")
    @Size(max = 30, message = "Ký hiệu văn bản tối đa 30 ký tự")
    private String codeNotation;

    @NotNull(message = "Ngày ban hành không được để trống")
    private LocalDate issuedDate;

    @NotBlank(message = "Trích yếu không được để trống")
    @Size(max = 500, message = "Trích yếu tối đa 500 ký tự")
    private String subject;

    @Size(max = 30, message = "Ngôn ngữ tối đa 30 ký tự")
    private String language;

    @Min(value = 1, message = "Số trang tối thiểu là 1")
    @Max(value = 999, message = "Số trang tối đa 999")
    private Integer pageAmount = 1;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String description;

    @Size(max = 100, message = "Chức vụ người ký tối đa 100 ký tự")
    private String signerPosition;

    @Size(max = 50, message = "Họ tên người ký tối đa 50 ký tự")
    private String signerFullName;

    @Min(value = 0, message = "Mức độ khẩn từ 0 đến 9")
    @Max(value = 9, message = "Mức độ khẩn từ 0 đến 9")
    private Integer priority = 0;

    @Min(value = 1, message = "Số bản tối thiểu là 1")
    @Max(value = 999, message = "Số bản tối đa 999")
    private Integer issuedAmount = 1;

    private LocalDate dueDate;

    /** true = đăng ký và ban hành ngay. */
    private boolean issueImmediately = false;
}