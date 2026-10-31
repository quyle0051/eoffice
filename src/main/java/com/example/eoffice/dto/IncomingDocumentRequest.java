package com.example.eoffice.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class IncomingDocumentRequest {
    @NotNull(message = "Vui lòng chọn sổ văn bản")
    private Long documentBookId;

    @NotNull(message = "Vui lòng chọn loại văn bản")
    private Long documentTypeId;

    @NotBlank(message = "Trích yếu không được để trống")
    @Size(max = 500, message = "Trích yếu tối đa 500 ký tự")
    private String subject;

    @Size(max = 11, message = "Số văn bản tối đa 11 ký tự")
    private String codeNumber;

    @Size(max = 30, message = "Ký hiệu văn bản tối đa 30 ký tự")
    private String codeNotation;

    private LocalDate issuedDate;

    @Size(max = 200, message = "Tên cơ quan ban hành tối đa 200 ký tự")
    private String organName;

    @Size(max = 30, message = "Ngôn ngữ tối đa 30 ký tự")
    private String language;

    @Min(value = 0, message = "Số trang không được âm")
    @Max(value = 999, message = "Số trang tối đa 999")
    private Integer pageAmount;

    @Min(value = 0, message = "Số thứ tự trong hồ sơ không được âm")
    @Max(value = 999, message = "Số thứ tự trong hồ sơ tối đa 999")
    private Integer docOrdinal;

    @Size(max = 500, message = "Ghi chú tối đa 500 ký tự")
    private String description;

    /** Để trống thì lấy ngày hôm nay. */
    private LocalDate arrivalDate;

    @Size(max = 100, message = "Chức vụ người ký tối đa 100 ký tự")
    private String signerPosition;

    @Size(max = 50, message = "Họ tên người ký tối đa 50 ký tự")
    private String signerFullName;

    @Min(value = 0, message = "Mức độ khẩn, mật từ 0 đến 9")
    @Max(value = 9, message = "Mức độ khẩn, mật từ 0 đến 9")
    private Integer priority;

    @Size(max = 1000, message = "Nơi nhận tối đa 1000 ký tự")
    private String toPlaces;

    private String traceHeaderList;

    /** Để trống thì tự tính = ngày đến + số ngày xử lý chuẩn của sổ. */
    private LocalDate dueDate;
}
