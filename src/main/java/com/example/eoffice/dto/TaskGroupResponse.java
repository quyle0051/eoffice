package com.example.eoffice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class TaskGroupResponse {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private Long creatorId;
    private String creatorName;
    private int memberCount;
    private List<MemberDto> members;

    /** Người đang đăng nhập có được sửa, xóa nhóm này không. */
    private boolean canManage;

    @Getter
    @AllArgsConstructor
    public static class MemberDto {
        private Long id;
        private String fullName;
        private String email;
        private String departmentName;
    }
}