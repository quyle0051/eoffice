package com.example.eoffice.dto;


import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class IncomingWorkflowRequest {
    private Long receiverId;
    private String directiveNote;

    private String taskTitle;
    private Long taskGroupId;

    // Có thể chọn nhiều phòng ban (tự lấy Trưởng/Phó phòng làm assignee) hoặc chọn danh sách assigneeIds
    private List<Long> departmentIds;
    private Boolean includeHead = true;
    private Boolean includeDeputy = false;
    private List<Long> assigneeIds;

    private Long supervisorId;
    private Long supporterId;
    private Boolean progressReportRequired = true;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
