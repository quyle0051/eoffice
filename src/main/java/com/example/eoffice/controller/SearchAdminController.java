package com.example.eoffice.controller;

import com.example.eoffice.search.IncomingReindexService;
import com.example.eoffice.search.InternalReindexService;
import com.example.eoffice.search.OutgoingReindexService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/search")
@RequiredArgsConstructor
public class SearchAdminController {

    private final IncomingReindexService incomingReindexService;
    private final OutgoingReindexService outgoingReindexService;
    private final InternalReindexService internalReindexService;

    /** Dựng lại toàn bộ index từ MySQL (chỉ Admin hoặc Văn thư). */
    @PostMapping("/reindex")
    public Map<String, Object> reindex(Authentication auth) {
        boolean allowed = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_CLERK"));
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("incomingDocuments", incomingReindexService.reindexAll());
        result.put("outgoingDocuments", outgoingReindexService.reindexAll());
        result.put("internalDocuments", internalReindexService.reindexAll());
        return result;
    }
}