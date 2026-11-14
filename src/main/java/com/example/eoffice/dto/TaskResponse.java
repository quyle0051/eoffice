package com.example.eoffice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class TaskResponse {
    private Long id;
    private String title;
    private Long parentTaskId;
    private String parentTaskTitle;

    private Long taskGroupId;
    private String taskGroupName;

    private Long incomingDocumentId;
    private Long incomingArrivalNumber;
    private String incomingSubject;

    private String creatorName;
    private Long assigneeId;
    private String assigneeName;

    private List<String> supervisorNames;
    private List<String> supporterNames;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Integer progressPercent;
    private String myRoleLabel;
}