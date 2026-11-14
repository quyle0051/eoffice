package com.example.eoffice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class TaskPostResponse {
    private Long id;
    private Long authorId;
    private String authorName;
    private String authorEmail;
    private String authorRoleLabel;
    private String weekLabel;
    private String postType;
    private Integer progressPercent;
    private String content;
    private LocalDateTime createdAt;
    private List<AttachmentDto> attachments;
    private List<CommentDto> comments;

    @Getter
    @AllArgsConstructor
    public static class AttachmentDto {
        private Long id;
        private String fileName;
        private String fileType;
        private Long fileSize;
        private LocalDateTime uploadedAt;
    }

    @Getter
    @AllArgsConstructor
    public static class CommentDto {
        private Long id;
        private Long authorId;
        private String authorName;
        private String authorEmail;
        private String content;
        private LocalDateTime createdAt;
    }
}