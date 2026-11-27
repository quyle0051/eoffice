package com.example.eoffice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TaskGroupRequest {

    @NotBlank(message = "Tên nhóm không được để trống")
    @Size(max = 255, message = "Tên nhóm tối đa 255 ký tự")
    private String name;

    @Size(max = 500, message = "Mô tả tối đa 500 ký tự")
    private String description;

    /** ID các thành viên của nhóm. */
    private List<Long> memberIds;
}