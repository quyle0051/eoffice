package com.example.eoffice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentBookRequest {
    @NotBlank(message = "Tên sổ không được để trống")
    @Size(max = 255, message = "Tên sổ tối đa 255 ký tự")
    private String name;

    @Size(max = 50, message = "Tên viết tắt tối đa 50 ký tự")
    private String shortName;

    @Min(value = 0, message = "Số ngày xử lý không được âm")
    private Integer standardDays;

    /** id của sổ cha, để trống nếu là sổ gốc. */
    private Long parentId;
}
