package com.example.eoffice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "Vui lòng chọn loại sổ")
    private Long bookTypeId;

    /** id của sổ cha, để trống nếu là sổ gốc. */
    private Long parentId;
}
