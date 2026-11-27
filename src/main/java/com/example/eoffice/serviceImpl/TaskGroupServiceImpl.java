package com.example.eoffice.serviceImpl;

import com.example.eoffice.dto.TaskGroupRequest;
import com.example.eoffice.dto.TaskGroupResponse;
import com.example.eoffice.entity.TaskGroup;
import com.example.eoffice.entity.User;
import com.example.eoffice.entity.UserStatus;
import com.example.eoffice.repository.TaskGroupRepository;
import com.example.eoffice.repository.TaskRepository;
import com.example.eoffice.repository.UserRepository;
import com.example.eoffice.service.TaskGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TaskGroupServiceImpl implements TaskGroupService {

    /** Vai trò có chức năng nhóm công việc: Admin, Giám đốc, Phó giám đốc, Trưởng phòng. */
    private static final Set<String> MANAGER_ROLES =
            Set.of("ROLE_ADMIN", "ROLE_DIRECTOR", "ROLE_DEPUTY_DIRECTOR", "ROLE_HEAD");

    /** Vai trò được sửa, xóa mọi nhóm. Trưởng phòng chỉ sửa, xóa nhóm do chính mình tạo. */
    private static final Set<String> FULL_ACCESS_ROLES =
            Set.of("ROLE_ADMIN", "ROLE_DIRECTOR", "ROLE_DEPUTY_DIRECTOR");

    private final TaskGroupRepository repository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;

    // ------------------------------------------------------------------ Đọc

    @Override
    @Transactional(readOnly = true)
    public List<TaskGroupResponse> findAll(String keyword, String email) {
        // Người có chức năng thấy mọi nhóm, người khác chỉ thấy nhóm mình là thành viên
        List<TaskGroup> groups = hasAnyAuthority(MANAGER_ROLES)
                ? repository.findAllByOrderByIdAsc()
                : repository.findByMemberEmail(email);

        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim().toLowerCase();
        return groups.stream()
                .filter(g -> kw == null
                        || g.getName().toLowerCase().contains(kw)
                        || (g.getDescription() != null && g.getDescription().toLowerCase().contains(kw)))
                .map(g -> toResponse(g, email))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TaskGroupResponse findById(Long id, String email) {
        TaskGroup group = findOrThrow(id);
        boolean isMember = group.getMembers().stream().anyMatch(u -> email.equalsIgnoreCase(u.getEmail()));
        if (!hasAnyAuthority(MANAGER_ROLES) && !isMember) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền xem nhóm công việc này");
        }
        return toResponse(group, email);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskGroupResponse.MemberDto> getMemberOptions() {
        requireManager();
        return userRepository.findAllByStatusOrderByFullNameAsc(UserStatus.ACTIVE).stream()
                .map(this::toMember)
                .toList();
    }

    // ------------------------------------------------------------------ Ghi

    @Override
    @Transactional
    @CacheEvict(value = "dispatchOptions", allEntries = true)
    public TaskGroupResponse create(TaskGroupRequest request, String email) {
        requireManager();
        User creator = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không xác định được người dùng"));

        String name = request.getName().trim();
        if (repository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đã có nhóm công việc trùng tên");
        }

        TaskGroup group = new TaskGroup();
        group.setName(name);
        group.setDescription(clean(request.getDescription()));
        group.setCreator(creator);
        group.setMembers(loadMembers(request.getMemberIds()));
        return toResponse(repository.save(group), email);
    }

    @Override
    @Transactional
    @CacheEvict(value = "dispatchOptions", allEntries = true)
    public TaskGroupResponse update(Long id, TaskGroupRequest request, String email) {
        TaskGroup group = findOrThrow(id);
        requireCanManage(group, email);

        String name = request.getName().trim();
        if (repository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Đã có nhóm công việc trùng tên");
        }

        group.setName(name);
        group.setDescription(clean(request.getDescription()));
        group.getMembers().clear();
        group.getMembers().addAll(loadMembers(request.getMemberIds()));
        return toResponse(repository.save(group), email);
    }

    @Override
    @Transactional
    @CacheEvict(value = "dispatchOptions", allEntries = true)
    public void delete(Long id, String email) {
        TaskGroup group = findOrThrow(id);
        requireCanManage(group, email);
        if (taskRepository.existsByTaskGroupId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Nhóm đã được dùng trong công việc nên không thể xóa");
        }
        repository.delete(group);
    }

    // ------------------------------------------------------------------ Hàm hỗ trợ

    private TaskGroup findOrThrow(Long id) {
        return repository.findWithMembersById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhóm công việc"));
    }

    private Set<User> loadMembers(List<Long> memberIds) {
        Set<User> members = new LinkedHashSet<>();
        if (memberIds == null || memberIds.isEmpty()) {
            return members;
        }
        Set<Long> distinct = new LinkedHashSet<>(memberIds);
        List<User> users = userRepository.findAllById(distinct);
        if (users.size() != distinct.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Có thành viên được chọn không tồn tại");
        }
        for (User u : users) {
            if (u.getStatus() != UserStatus.ACTIVE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Không thể thêm người dùng đã bị khóa: " + u.getFullName());
            }
            members.add(u);
        }
        return members;
    }

    private boolean hasAnyAuthority(Set<String> roles) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> roles.contains(a.getAuthority()));
    }

    private void requireManager() {
        if (!hasAnyAuthority(MANAGER_ROLES)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Chỉ Admin, Giám đốc, Phó giám đốc hoặc Trưởng phòng được quản lý nhóm công việc");
        }
    }

    private boolean canManage(TaskGroup group, String email) {
        if (hasAnyAuthority(FULL_ACCESS_ROLES)) {
            return true;
        }
        return hasAnyAuthority(MANAGER_ROLES)
                && group.getCreator() != null
                && email.equalsIgnoreCase(group.getCreator().getEmail());
    }

    private void requireCanManage(TaskGroup group, String email) {
        requireManager();
        if (!canManage(group, email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Bạn chỉ được sửa hoặc xóa nhóm do chính mình tạo");
        }
    }

    private static String clean(String text) {
        return (text == null || text.isBlank()) ? null : text.trim();
    }

    private TaskGroupResponse.MemberDto toMember(User u) {
        return new TaskGroupResponse.MemberDto(
                u.getId(),
                u.getFullName(),
                u.getEmail(),
                u.getDepartment() != null ? u.getDepartment().getName() : null);
    }

    private TaskGroupResponse toResponse(TaskGroup g, String email) {
        List<TaskGroupResponse.MemberDto> members = g.getMembers().stream()
                .sorted(Comparator.comparing(User::getFullName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toMember)
                .toList();
        return TaskGroupResponse.builder()
                .id(g.getId())
                .name(g.getName())
                .description(g.getDescription())
                .createdAt(g.getCreatedAt())
                .creatorId(g.getCreator() != null ? g.getCreator().getId() : null)
                .creatorName(g.getCreator() != null ? g.getCreator().getFullName() : null)
                .memberCount(members.size())
                .members(members)
                .canManage(canManage(g, email))
                .build();
    }
}