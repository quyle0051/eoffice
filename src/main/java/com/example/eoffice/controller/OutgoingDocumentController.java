package com.example.eoffice.controller;

import com.example.eoffice.dto.OutgoingDocumentRequest;
import com.example.eoffice.dto.OutgoingDocumentResponse;
import com.example.eoffice.dto.OutgoingSearchRequest;
import com.example.eoffice.dto.PageResponse;
import com.example.eoffice.entity.Organ;
import com.example.eoffice.entity.OutgoingDocument;
import com.example.eoffice.service.OutgoingDocumentService;
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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/outgoing-documents")
@RequiredArgsConstructor
public class OutgoingDocumentController {

    private final OutgoingDocumentService outgoingService;

    @GetMapping
    public PageResponse<OutgoingDocumentResponse> search(OutgoingSearchRequest request) {
        return outgoingService.search(request);
    }

    /** Chức năng 2: Tự động gợi ý Số văn bản đi & Ký hiệu khi chọn Sổ và Loại văn bản */
    @GetMapping("/next-number")
    public Map<String, String> suggestNextNumber(
            @RequestParam(name = "bookId") Long bookId,
            @RequestParam(name = "typeId", required = false) Long typeId) {
        return outgoingService.suggestNextNumberAndNotation(bookId, typeId);
    }

    /** Lấy danh mục Cơ quan bên ngoài để chọn Nơi nhận (Mục 13) */
    @GetMapping("/organs")
    public List<Organ> getOrgans() {
        return outgoingService.getAllOrgans();
    }

    @GetMapping("/{id:\\d+}")
    public OutgoingDocumentResponse findById(@PathVariable("id") Long id) {
        return outgoingService.findById(id);
    }

    /** Chức năng 3: Đăng ký, cấp số, thời gian ban hành văn bản đi */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OutgoingDocumentResponse create(@Valid @RequestBody OutgoingDocumentRequest request,
                                           Authentication auth) {
        return outgoingService.create(request, auth != null ? auth.getName() : null);
    }

    @PutMapping("/{id:\\d+}")
    public OutgoingDocumentResponse update(@PathVariable("id") Long id,
                                           @Valid @RequestBody OutgoingDocumentRequest request) {
        return outgoingService.update(id, request);
    }

    /** Đính kèm tệp văn bản đi */
    @PostMapping(value = "/{id:\\d+}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public OutgoingDocumentResponse uploadAttachment(@PathVariable("id") Long id,
                                                     @RequestParam("file") MultipartFile file) {
        return outgoingService.uploadAttachment(id, file);
    }

    /** Xem / Tải về tệp văn bản đi */
    @GetMapping("/{id:\\d+}/attachment")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable("id") Long id) {
        OutgoingDocument doc = outgoingService.getDocumentForDownload(id);
        if (doc.getAttachmentPath() == null || doc.getAttachmentPath().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Văn bản đi chưa có tệp đính kèm");
        }
        try {
            Path path = Paths.get(doc.getAttachmentPath());
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tệp trên máy chủ");
            }
            String contentType = Files.probeContentType(path);
            if (contentType == null) contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            String encodedName = URLEncoder.encode(doc.getAttachmentName(), StandardCharsets.UTF_8).replace("+", "%20");
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedName)
                    .body(resource);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể đọc tệp đính kèm");
        }
    }

    /** Phát hành văn bản đi */
    @PostMapping("/{id:\\d+}/issue")
    public OutgoingDocumentResponse issue(@PathVariable("id") Long id) {
        return outgoingService.issueDocument(id);
    }

    /** Cập nhật trạng thái theo dõi phát hành của 1 cơ quan nhận */
    @PutMapping("/{id:\\d+}/recipients/{recipientId:\\d+}")
    public OutgoingDocumentResponse updateRecipient(
            @PathVariable("id") Long id,
            @PathVariable("recipientId") Long recipientId,
            @RequestBody Map<String, String> body) {
        return outgoingService.updateRecipientStatus(
                id,
                recipientId,
                body.get("deliveryStatus"),
                body.get("trackingNote")
        );
    }

    /** Lưu trữ văn bản đi */
    @PostMapping("/{id:\\d+}/archive")
    public OutgoingDocumentResponse archive(@PathVariable("id") Long id) {
        return outgoingService.archiveDocument(id);
    }

    @DeleteMapping("/{id:\\d+}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        outgoingService.softDelete(id);
    }
}