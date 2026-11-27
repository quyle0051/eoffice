package com.example.eoffice.controller;

import com.example.eoffice.dto.TaskGroupRequest;
import com.example.eoffice.dto.TaskGroupResponse;
import com.example.eoffice.service.TaskGroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/task-groups")
@RequiredArgsConstructor
public class TaskGroupController {

    private final TaskGroupService service;

    @GetMapping
    public List<TaskGroupResponse> findAll(
            @RequestParam(name = "keyword", required = false) String keyword,
            Authentication auth) {
        return service.findAll(keyword, auth.getName());
    }

    /** Danh sách người dùng để chọn thành viên khi tạo hoặc sửa nhóm. */
    @GetMapping("/member-options")
    public List<TaskGroupResponse.MemberDto> memberOptions() {
        return service.getMemberOptions();
    }

    @GetMapping("/{id:\\d+}")
    public TaskGroupResponse findById(@PathVariable("id") Long id, Authentication auth) {
        return service.findById(id, auth.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskGroupResponse create(@Valid @RequestBody TaskGroupRequest request, Authentication auth) {
        return service.create(request, auth.getName());
    }

    @PutMapping("/{id:\\d+}")
    public TaskGroupResponse update(@PathVariable("id") Long id,
                                    @Valid @RequestBody TaskGroupRequest request,
                                    Authentication auth) {
        return service.update(id, request, auth.getName());
    }

    @DeleteMapping("/{id:\\d+}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id, Authentication auth) {
        service.delete(id, auth.getName());
    }
}