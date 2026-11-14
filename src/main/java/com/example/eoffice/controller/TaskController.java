package com.example.eoffice.controller;

import com.example.eoffice.dto.*;
import com.example.eoffice.entity.TaskPostAttachment;
import com.example.eoffice.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public PageResponse<TaskResponse> search(
            @RequestParam(name = "mode", defaultValue = "ASSIGNED") String mode,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            Authentication auth) {
        return taskService.search(auth.getName(), mode, keyword, status, page, size);
    }

    //Lấy chi tiết công việc (kèm danh sách việc con & bảng tin báo cáo tuần)
    @GetMapping("/{id:\\d+}")
    public TaskDetailResponse getDetail(@PathVariable("id") Long id, Authentication auth) {
        return taskService.getDetail(id, auth.getName());
    }

    //Cập nhật nhanh trạng thái công việc
    @PutMapping("/{id:\\d+}/status")
    public TaskDetailResponse updateStatus(@PathVariable("id") Long id,
                                           @RequestBody Map<String, String> body,
                                           Authentication auth) {
        return taskService.updateStatus(id, body.get("status"), auth.getName());
    }

    //Người chủ trì chia nhỏ công việc thành việc con (Sub-task)
    @PostMapping("/{id:\\d+}/subtasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createSubTask(@PathVariable("id") Long id,
                                      @Valid @RequestBody SubTaskCreateRequest request,
                                      Authentication auth) {
        return taskService.createSubTask(id, request, auth.getName());
    }


    @PostMapping(value = "/{id:\\d+}/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public TaskPostResponse createPost(
            @PathVariable("id") Long id,
            @RequestParam(name = "weekLabel", required = false) String weekLabel,
            @RequestParam(name = "postType", defaultValue = "PROGRESS_REPORT") String postType,
            @RequestParam(name = "progressPercent", required = false) Integer progressPercent,
            @RequestParam(name = "content") String content,
            @RequestParam(name = "files", required = false) List<MultipartFile> files,
            Authentication auth) {
        return taskService.createPost(id, weekLabel, postType, progressPercent, content, files, auth.getName());
    }

    // thêm bình luận phản hồi bên dưới 1 bài báo cáo
    @PostMapping("/{id:\\d+}/posts/{postId:\\d+}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskPostResponse.CommentDto addComment(
            @PathVariable("id") Long id,
            @PathVariable("postId") Long postId,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        return taskService.addComment(id, postId, body.get("content"), auth.getName());
    }


    @GetMapping("/attachments/{attachmentId:\\d+}")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable("attachmentId") Long attachmentId,
            Authentication auth) {
        TaskPostAttachment att = taskService.getAttachmentForDownload(attachmentId, auth.getName());
        try {
            Path filePath = Paths.get(att.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tệp báo cáo không tồn tại trên máy chủ");
            }
            String contentType = att.getFileType();
            if (contentType == null || contentType.isBlank()) {
                contentType = Files.probeContentType(filePath);
            }
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            String encodedName = URLEncoder.encode(att.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedName)
                    .body(resource);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể đọc tệp đính kèm");
        }
    }
}