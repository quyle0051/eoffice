package com.example.eoffice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class IncomingDispatchRequest {

    @NotNull(message = "Vui lòng chọn 1 Người chủ trì")
    private Long mainAssigneeId;

    private List<Long> supervisorIds;
    private List<Long> supporterUserIds;
    private Long primaryTaskGroupId;
    private String toPlacesSummary;

    private List<DispatchRowItem> rows;

    @Getter
    @Setter
    public static class DispatchRowItem {
        private String key;
        private String type;          // USER | DEPARTMENT | GROUP
        private String targetLabel;   // Tên hiển thị trên bảng và chip
        private String codeNotation;  // Số, ký hiệu
        private String roleLabel;     // Chủ trì | Giám sát | Hỗ trợ
        private String priorityLabel; // Bình thường | Khẩn | Hỏa tốc
        private LocalDate dueDate;    // Hạn xử lý
        private String note;          // Ghi chú
        private String status;        // Chưa xem | Đã xem
        private Long groupId;
        private List<MemberItem> members;
    }

    @Getter
    @Setter
    public static class MemberItem {
        private Long id;
        private String fullName;
        private String email;
        private String positionNote;
    }
}
