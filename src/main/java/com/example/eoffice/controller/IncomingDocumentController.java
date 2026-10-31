package com.example.eoffice.controller;

import com.example.eoffice.dto.*;
import com.example.eoffice.entity.IncomingDocument;
import com.example.eoffice.repository.IncomingDocumentRepository;
import com.example.eoffice.service.IncomingDocumentService;
import com.example.eoffice.serviceImpl.IncomingWorkflowService;
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
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/incoming-documents")
@RequiredArgsConstructor
public class IncomingDocumentController {

    private final IncomingDocumentService service;
    private final IncomingWorkflowService workflowService;
    private final IncomingDocumentRepository docRepository;

    private final Path uploadDir = Paths.get("uploads/incoming");
    @GetMapping
    public PageResponse<IncomingDocumentResponse> search(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "documentBookId", required = false) Long documentBookId,
            @RequestParam(name = "documentTypeId", required = false) Long documentTypeId,
            @RequestParam(name = "arrivalFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate arrivalFrom,
            @RequestParam(name = "arrivalTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate arrivalTo,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return service.search(keyword, documentBookId, documentTypeId, arrivalFrom, arrivalTo, page, size);
    }

    @GetMapping("/{id:\\d+}")
    public IncomingDocumentResponse findById(@PathVariable("id") Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncomingDocumentResponse create(@Valid @RequestBody IncomingDocumentRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public IncomingDocumentResponse update(@PathVariable("id") Long id,
                                           @Valid @RequestBody IncomingDocumentRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        service.delete(id);
    }

    @PostMapping("/{id}/submit-chief")
    public IncomingDocumentResponse submitToOfficeChief(@PathVariable("id") Long id,
                                                        @RequestBody IncomingWorkflowRequest request,
                                                        Authentication auth) {
        return workflowService.submitToOfficeChief(id, auth.getName(), request);
    }

    @PostMapping("/{id}/forward-director")
    public IncomingDocumentResponse forwardToDirector(@PathVariable("id") Long id,
                                                      @RequestBody IncomingWorkflowRequest request,
                                                      Authentication auth) {
        return workflowService.forwardToDirector(id, auth.getName(), request);
    }

    @PostMapping("/{id}/approve-director")
    public IncomingDocumentResponse approveByDirector(@PathVariable("id") Long id,
                                                      @RequestBody IncomingWorkflowRequest request,
                                                      Authentication auth) {
        return workflowService.approveByDirector(id, auth.getName(), request);
    }

    @PostMapping("/{id}/dispatch")
    public IncomingDocumentResponse dispatchToHandlers(@PathVariable("id") Long id,
                                                       @RequestBody IncomingWorkflowRequest request,
                                                       Authentication auth) {
        return workflowService.dispatchToHandlers(id, auth.getName(), request);
    }

    @GetMapping("/dispatch-options")
    public DispatchOptionsResponse getDispatchOptions() {
        return workflowService.getDispatchOptions();
    }


    // --- UPLOAD & DOWNLOAD FILE ĐÍNH KÈM ---

    @PostMapping(value = "/{id:\\d+}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IncomingDocumentResponse uploadAttachment(@PathVariable("id") Long id,
                                                     @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File đính kèm trống");
        }
        IncomingDocument doc = docRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy văn bản đến"));
        try {
            Files.createDirectories(uploadDir);
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment";
            String storedName = id + "_" + UUID.randomUUID() + "_" + originalName;
            Path target = uploadDir.resolve(storedName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            doc.setAttachmentName(originalName);
            doc.setAttachmentPath(target.toString());
            docRepository.save(doc);
            return service.findById(id);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lưu file đính kèm");
        }
    }

    @GetMapping("/{id:\\d+}/attachment")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable("id") Long id) {
        IncomingDocument doc = docRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy văn bản đến"));
        if (doc.getAttachmentPath() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Văn bản không có file đính kèm");
        }
        try {
            Path filePath = Paths.get(doc.getAttachmentPath());
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File đính kèm không tồn tại trên máy chủ");
            }
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            String encodedName = URLEncoder.encode(doc.getAttachmentName(), StandardCharsets.UTF_8).replace("+", "%20");
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedName)
                    .body(resource);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể đọc file đính kèm");
        }
    }

    @GetMapping("/{id:\\d+}/dispatch-task")
    public IncomingDispatchRequest getDispatchTask(@PathVariable("id") Long id) {
        return workflowService.getDispatchState(id);
    }

    @PostMapping("/{id:\\d+}/dispatch-task")
    public IncomingDocumentResponse dispatchTask(@PathVariable("id") Long id,
                                                 @Valid @RequestBody IncomingDispatchRequest request,
                                                 Authentication auth) {
        return workflowService.dispatchDocument(id, auth.getName(), request);
    }
}
