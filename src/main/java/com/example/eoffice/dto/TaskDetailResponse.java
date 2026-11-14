package com.example.eoffice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class TaskDetailResponse {
    private Long id;
    private String title;
    private String description;

    private Long parentTaskId;
    private String parentTaskTitle;

    private Long taskGroupId;
    private String taskGroupName;

    private Long incomingDocumentId;
    private Long incomingArrivalNumber;
    private String incomingSubject;
    private String incomingAttachmentName;

    private Long creatorId;
    private String creatorName;
    private String creatorEmail;

    private Long assigneeId;
    private String assigneeName;
    private String assigneeEmail;

    private List<SimpleUserDto> supervisors;
    private List<SimpleUserDto> supporters;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Integer progressPercent;

    private List<TaskResponse> subTasks;
    private List<TaskPostResponse> posts;

    @Getter
    @AllArgsConstructor
    public static class SimpleUserDto {
        private Long id;
        private String fullName;
        private String email;
    }
}