package com.example.eoffice.controller;

import com.example.eoffice.dto.InternalDocumentRequest;
import com.example.eoffice.dto.InternalDocumentResponse;
import com.example.eoffice.dto.InternalSearchRequest;
import com.example.eoffice.dto.PageResponse;
import com.example.eoffice.entity.InternalDocument;
import com.example.eoffice.service.InternalDocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/internal-documents")
@RequiredArgsConstructor
public class InternalDocumentController {

    private final InternalDocumentService service;

    @GetMapping
    public PageResponse<InternalDocumentResponse> search(InternalSearchRequest request) {
        return service.search(request);
    }

    /** Gợi ý số văn bản, ký hiệu và cơ quan ban hành khi chọn sổ và loại văn bản. */
    @GetMapping("/next-info")
    public Map<String, String> nextInfo(
            @RequestParam(name = "bookId", required = false) Long bookId,
            @RequestParam(name = "typeId", required = false) Long typeId,
            @RequestParam(name = "issuedDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issuedDate) {
        return service.getNextInfo(bookId, typeId, issuedDate);
    }

    @GetMapping("/{id:\\d+}")
    public InternalDocumentResponse findById(@PathVariable("id") Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InternalDocumentResponse create(@Valid @RequestBody InternalDocumentRequest request, Authentication auth) {
        requireClerk(auth);
        return service.create(request, auth.getName());
    }

    @PutMapping("/{id:\\d+}")
    public InternalDocumentResponse update(@PathVariable("id") Long id,
                                           @Valid @RequestBody InternalDocumentRequest request,
                                           Authentication auth) {
        requireClerk(auth);
        return service.update(id, request);
    }

    @PostMapping(value = "/{id:\\d+}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public InternalDocumentResponse uploadAttachment(@PathVariable("id") Long id,
                                                     @RequestParam("file") MultipartFile file,
                                                     Authentication auth) {
        requireClerk(auth);
        return service.uploadAttachment(id, file);
    }

    @GetMapping("/{id:\\d+}/attachment")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable("id") Long id) {
        InternalDocument doc = service.getDocumentForDownload(id);
        if (doc.getAttachmentPath() == null || doc.getAttachmentPath().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Văn bản nội bộ chưa có tệp đính kèm");
        }
        try {
            Path path = Paths.get(doc.getAttachmentPath());
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tệp trên máy chủ");
            }
            String contentType = Files.probeContentType(path);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            // Chỉ cho trình duyệt mở trực tiếp PDF và ảnh (trừ SVG). Các loại khác bắt tải về để tránh chạy mã trong tệp.
            boolean safeInline = contentType.equals("application/pdf")
                    || (contentType.startsWith("image/") && !contentType.contains("svg"));
            String name = doc.getAttachmentName() != null ? doc.getAttachmentName() : "attachment";
            String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            (safeInline ? "inline" : "attachment") + "; filename*=UTF-8''" + encodedName)
                    .header("X-Content-Type-Options", "nosniff")
                    .body(resource);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể đọc tệp đính kèm");
        }
    }

    @PostMapping("/{id:\\d+}/issue")
    public InternalDocumentResponse issue(@PathVariable("id") Long id, Authentication auth) {
        requireClerk(auth);
        return service.issueDocument(id);
    }

    @PostMapping("/{id:\\d+}/archive")
    public InternalDocumentResponse archive(@PathVariable("id") Long id, Authentication auth) {
        requireClerk(auth);
        return service.archiveDocument(id);
    }

    @DeleteMapping("/{id:\\d+}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id, Authentication auth) {
        requireClerk(auth);
        service.softDelete(id);
    }

    /** Chỉ Văn thư hoặc Quản trị viên được tạo, sửa, ban hành, lưu trữ và xóa văn bản nội bộ. */
    private void requireClerk(Authentication auth) {
        boolean allowed = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_CLERK"));
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Chỉ Văn thư hoặc Quản trị viên được thực hiện thao tác này");
        }
    }
}