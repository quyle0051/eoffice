package com.example.eoffice.service;

import com.example.eoffice.dto.PageResponse;
import com.example.eoffice.dto.SubTaskCreateRequest;
import com.example.eoffice.dto.TaskDetailResponse;
import com.example.eoffice.dto.TaskResponse;
import com.example.eoffice.dto.TaskPostResponse;
import com.example.eoffice.entity.TaskPostAttachment;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TaskService {
    PageResponse<TaskResponse> search(String email, String mode, String keyword, String status, int page, int size);

    TaskDetailResponse getDetail(Long taskId, String email);

    TaskDetailResponse updateStatus(Long taskId, String newStatus, String email);

    TaskResponse createSubTask(Long parentTaskId, SubTaskCreateRequest req, String email);

    TaskPostResponse createPost(Long taskId,
                                String weekLabel,
                                String postType,
                                Integer progressPercent,
                                String content,
                                List<MultipartFile> files,
                                String email);

    TaskPostResponse.CommentDto addComment(Long taskId, Long postId, String content, String email);

    TaskPostAttachment getAttachmentForDownload(Long attachmentId, String email);
}
