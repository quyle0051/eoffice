package com.example.eoffice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class SubTaskCreateRequest {

    @NotBlank(message = "Tên công việc con không được để trống")
    private String title;

    private String description;

    @NotNull(message = "Vui lòng chọn 1 Người chủ trì cho công việc con")
    private Long mainAssigneeId;

    private List<Long> supervisorIds;
    private List<Long> supporterUserIds;
    private Long primaryTaskGroupId;
    private LocalDate dueDate;
}