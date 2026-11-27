package com.example.eoffice.service;

import com.example.eoffice.dto.TaskGroupRequest;
import com.example.eoffice.dto.TaskGroupResponse;

import java.util.List;

public interface TaskGroupService {

    List<TaskGroupResponse> findAll(String keyword, String email);

    TaskGroupResponse findById(Long id, String email);

    TaskGroupResponse create(TaskGroupRequest request, String email);

    TaskGroupResponse update(Long id, TaskGroupRequest request, String email);

    void delete(Long id, String email);

    List<TaskGroupResponse.MemberDto> getMemberOptions();
}