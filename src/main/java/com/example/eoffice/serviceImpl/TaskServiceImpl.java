package com.example.eoffice.serviceImpl;

import com.example.eoffice.dto.*;
import com.example.eoffice.entity.*;
import com.example.eoffice.repository.*;
import com.example.eoffice.search.IncomingDocumentChangedEvent;
import com.example.eoffice.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.*;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final TaskPostRepository taskPostRepository;
    private final TaskPostAttachmentRepository attachmentRepository;
    private final TaskPostCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final TaskGroupRepository taskGroupRepository;
    private final IncomingDocumentRepository incomingDocumentRepository;
    private final DepartmentRepository departmentRepository;
    private final IncomingWorkflowService incomingWorkflowService;
    private final ApplicationEventPublisher publisher;

    private final Path postUploadDir = Paths.get("uploads/task-posts");

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> search(String email, String mode, String keyword, String status, int page, int size) {
        String safeMode = (mode == null || mode.isBlank()) ? "ASSIGNED" : mode.trim().toUpperCase();

        String searchPattern = null;
        if (keyword != null && !keyword.trim().isEmpty()) {
            searchPattern = "%" + keyword.trim().toLowerCase() + "%";
        }

        TaskStatus statusEnum = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                statusEnum = TaskStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        int safePage = Math.max(page, 0);
        int safeSize = (size <= 0) ? 10 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<Task> taskPage = taskRepository.searchTasks(safeMode, email, searchPattern, statusEnum, pageable);

        List<TaskResponse> content = taskPage.getContent().stream()
                .map(t -> toResponse(t, safeMode))
                .toList();

        return new PageResponse<>(
                content,
                taskPage.getNumber(),
                taskPage.getSize(),
                taskPage.getTotalElements(),
                taskPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TaskDetailResponse getDetail(Long taskId, String email) {
        Task task = taskRepository.findWithDetailsById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy công việc"));

        if (!canViewTask(task, email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập công việc này");
        }

        return toDetailResponse(task);
    }

    @Override
    @Transactional
    public TaskDetailResponse updateStatus(Long taskId, String newStatus, String email) {
        Task task = taskRepository.findWithDetailsById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy công việc"));

        boolean isAssignee = task.getAssignee() != null && email.equalsIgnoreCase(task.getAssignee().getEmail());
        boolean isCreator = task.getCreator() != null && email.equalsIgnoreCase(task.getCreator().getEmail());

        if (!isAssignee && !isCreator && !isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Người chủ trì hoặc Người giao việc mới được đổi trạng thái");
        }
        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Công việc đã kết thúc, không thể đổi trạng thái");
        }
        try {
            TaskStatus statusEnum = TaskStatus.valueOf(newStatus.trim().toUpperCase());
            if (statusEnum == TaskStatus.COMPLETED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Hoàn thành công việc phải dùng nút \"Kết thúc công việc\" (chỉ Người chủ trì)");
            }
            task.setStatus(statusEnum);
            taskRepository.save(task);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trạng thái công việc không hợp lệ");
        }

        return toDetailResponse(task);
    }

    @Override
    @Transactional
    public TaskResponse createSubTask(Long parentTaskId, SubTaskCreateRequest req, String email) {
        Task parentTask = taskRepository.findWithDetailsById(parentTaskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy công việc gốc"));

        User creator = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy tài khoản đang đăng nhập"));

        // Chỉ Người chủ trì của công việc gốc (hoặc Admin) mới được quyền chia nhỏ công việc
        boolean isMainAssignee = parentTask.getAssignee() != null
                && parentTask.getAssignee().getId().equals(creator.getId());
        if (!isMainAssignee && !isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Chỉ Người chủ trì của công việc này mới có quyền chia nhỏ và giao việc con");
        }

        User mainAssignee = userRepository.findById(req.getMainAssigneeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy Người chủ trì việc con"));
        if (mainAssignee.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Người chủ trì việc con không còn hoạt động");
        }

        Set<User> supervisors = new LinkedHashSet<>();
        if (req.getSupervisorIds() != null && !req.getSupervisorIds().isEmpty()) {
            supervisors.addAll(findUsersOrThrow(req.getSupervisorIds()));
        } else {
            // Mặc định Người chia việc sẽ là Người giám sát của việc con
            supervisors.add(creator);
        }

        Set<User> supporters = new LinkedHashSet<>();
        if (req.getSupporterUserIds() != null && !req.getSupporterUserIds().isEmpty()) {
            supporters.addAll(findUsersOrThrow(req.getSupporterUserIds()));
        }

        if (!isAdmin()) {
            Set<Long> allowed = assignableUserIds(parentTask);
            boolean outOfScope = !allowed.contains(mainAssignee.getId())
                    || supervisors.stream().anyMatch(u -> !allowed.contains(u.getId()))
                    || supporters.stream().anyMatch(u -> !allowed.contains(u.getId()));
            if (outOfScope) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Chỉ được giao việc con cho người trong danh sách phân phối, cùng phòng ban hoặc cùng nhóm công việc");
            }
            Long parentGroupId = parentTask.getTaskGroup() != null ? parentTask.getTaskGroup().getId() : null;
            if (req.getPrimaryTaskGroupId() != null && !req.getPrimaryTaskGroupId().equals(parentGroupId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Chỉ được chọn nhóm công việc đã gắn với công việc gốc");
            }
        }

        TaskGroup group = (req.getPrimaryTaskGroupId() != null)
                ? taskGroupRepository.findById(req.getPrimaryTaskGroupId()).orElse(parentTask.getTaskGroup())
                : parentTask.getTaskGroup();

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = (req.getDueDate() != null)
                ? req.getDueDate().atTime(17, 0)
                : parentTask.getEndTime();

        Task subTask = new Task();
        subTask.setTitle(req.getTitle().trim());
        subTask.setDescription(req.getDescription() != null && !req.getDescription().isBlank()
                ? req.getDescription().trim()
                : parentTask.getDescription());
        subTask.setParentTask(parentTask);
        subTask.setIncomingDocument(parentTask.getIncomingDocument());
        subTask.setTaskGroup(group);
        subTask.setCreator(creator);
        subTask.setAssignee(mainAssignee);
        subTask.setSupervisors(supervisors);
        subTask.setSupporters(supporters);
        subTask.setStartTime(start);
        subTask.setEndTime(end);
        subTask.setStatus(TaskStatus.NEW);

        Task savedSub = taskRepository.save(subTask);

        // Nếu việc gốc gắn với Văn bản đến, tự động cấp quyền xem Văn bản đến đó cho những người nhận việc con
        if (parentTask.getIncomingDocument() != null) {
            incomingDocumentRepository.findByIdAndDeletedFalse(parentTask.getIncomingDocument().getId())
                    .ifPresent(doc -> {
                        doc.getParticipants().add(mainAssignee);
                        doc.getParticipants().addAll(supervisors);
                        doc.getParticipants().addAll(supporters);
                        incomingDocumentRepository.save(doc);
                        publisher.publishEvent(new IncomingDocumentChangedEvent(doc.getId()));
                    });
        }

        // Nếu việc gốc đang ở trạng thái NEW thì tự động chuyển sang IN_PROGRESS khi bắt đầu chia việc
        if (parentTask.getStatus() == TaskStatus.NEW) {
            parentTask.setStatus(TaskStatus.IN_PROGRESS);
            taskRepository.save(parentTask);
        }

        return toResponse(savedSub, "CREATED");
    }

    @Override
    @Transactional
    public TaskPostResponse createPost(Long taskId,
                                       String weekLabel,
                                       String postType,
                                       Integer progressPercent,
                                       String content,
                                       List<MultipartFile> files,
                                       String email) {
        if (content == null || content.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nội dung báo cáo không được để trống");
        }

        Task task = taskRepository.findWithDetailsById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy công việc"));

        if (!canViewTask(task, email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền đăng báo cáo trong công việc này");
        }

        User author = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy tài khoản"));

        TaskPost post = new TaskPost();
        post.setTask(task);
        post.setAuthor(author);
        post.setAuthorRoleLabel(resolveAuthorRoleLabel(task, author));
        post.setWeekLabel(weekLabel != null && !weekLabel.isBlank() ? weekLabel.trim() : null);
        post.setPostType(postType != null && !postType.isBlank() ? postType.trim().toUpperCase() : "PROGRESS_REPORT");
        post.setContent(content.trim());

        if ("PROGRESS_REPORT".equalsIgnoreCase(post.getPostType()) && progressPercent != null) {
            int safeProgress = Math.max(0, Math.min(100, progressPercent));
            post.setProgressPercent(safeProgress);

            // Tự động đồng bộ trạng thái công việc theo % tiến độ báo cáo
            if (safeProgress > 0 && task.getStatus() == TaskStatus.NEW) {
                task.setStatus(TaskStatus.IN_PROGRESS);
                taskRepository.save(task);
            }
        }

        TaskPost savedPost = taskPostRepository.save(post);

        if (files != null && !files.isEmpty()) {
            try {
                Files.createDirectories(postUploadDir);
                for (MultipartFile file : files) {
                    if (file == null || file.isEmpty()) continue;

                    String originalName = file.getOriginalFilename() != null
                            ? file.getOriginalFilename()
                            : "report_file";
                    String storedName = savedPost.getId() + "_" + UUID.randomUUID() + "_" + originalName;
                    Path target = postUploadDir.resolve(storedName);
                    Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

                    TaskPostAttachment att = new TaskPostAttachment();
                    att.setPost(savedPost);
                    att.setFileName(originalName);
                    att.setFilePath(target.toString());
                    att.setFileType(file.getContentType());
                    att.setFileSize(file.getSize());
                    savedPost.getAttachments().add(att);
                }
                savedPost = taskPostRepository.save(savedPost);
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi khi lưu tệp đính kèm báo cáo");
            }
        }

        return toPostResponse(savedPost);
    }

    @Override
    @Transactional
    public TaskPostResponse.CommentDto addComment(Long taskId, Long postId, String content, String email) {
        if (content == null || content.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nội dung bình luận không được để trống");
        }

        Task task = taskRepository.findWithDetailsById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy công việc"));

        if (!canViewTask(task, email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền bình luận trong công việc này");
        }

        TaskPost post = taskPostRepository.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy bài báo cáo"));

        if (!post.getTask().getId().equals(taskId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bài báo cáo không thuộc công việc này");
        }

        User author = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy tài khoản"));

        TaskPostComment comment = new TaskPostComment();
        comment.setPost(post);
        comment.setAuthor(author);
        comment.setContent(content.trim());

        TaskPostComment saved = commentRepository.save(comment);

        return new TaskPostResponse.CommentDto(
                saved.getId(),
                author.getId(),
                author.getFullName(),
                author.getEmail(),
                saved.getContent(),
                saved.getCreatedAt()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TaskPostAttachment getAttachmentForDownload(Long attachmentId, String email) {
        TaskPostAttachment att = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy tệp đính kèm"));

        Task task = taskRepository.findWithDetailsById(att.getPost().getTask().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy công việc"));

        if (!canViewTask(task, email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền tải tệp đính kèm này");
        }

        return att;
    }

    @Override
    @Transactional(readOnly = true)
    public DispatchOptionsResponse getAssignableOptions(Long taskId, String email) {
        Task task = taskRepository.findWithDetailsById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy công việc"));

        if (!canViewTask(task, email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập công việc này");
        }
        if (isAdmin()) {
            return incomingWorkflowService.getDispatchOptions();
        }

        boolean isMainAssignee = task.getAssignee() != null && email.equalsIgnoreCase(task.getAssignee().getEmail());
        if (!isMainAssignee) {
            // Chỉ Người chủ trì mới chia việc con nên những người khác không cần danh sách này
            return new DispatchOptionsResponse(List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        }

        DispatchOptionsResponse all = incomingWorkflowService.getDispatchOptions();
        Set<Long> allowedUserIds = assignableUserIds(task);
        Set<Long> allowedDepartmentIds = new HashSet<>();
        for (Department d : assignableDepartments(task)) {
            allowedDepartmentIds.add(d.getId());
        }
        Long groupId = task.getTaskGroup() != null ? task.getTaskGroup().getId() : null;

        return new DispatchOptionsResponse(
                filterUsers(all.getOfficeChiefs(), allowedUserIds),
                filterUsers(all.getDirectors(), allowedUserIds),
                filterUsers(all.getLeaders(), allowedUserIds),
                filterUsers(all.getAllUsers(), allowedUserIds),
                all.getDepartments().stream().filter(d -> allowedDepartmentIds.contains(d.getId())).toList(),
                all.getTaskGroups().stream().filter(g -> g.getId().equals(groupId)).toList()
        );
    }

    @Override
    @Transactional
    public TaskDetailResponse finishTask(Long taskId, String email) {
        Task task = taskRepository.findWithDetailsById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy công việc"));

        boolean isAssignee = task.getAssignee() != null && email.equalsIgnoreCase(task.getAssignee().getEmail());
        if (!isAssignee) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ Người chủ trì mới được kết thúc công việc");
        }
        if (task.getStatus() == TaskStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Công việc đã được kết thúc");
        }

        task.setStatus(TaskStatus.COMPLETED);
        taskRepository.save(task);

        // Việc con không ảnh hưởng trạng thái văn bản
        if (task.getParentTask() == null && task.getIncomingDocument() != null) {
            completeIncomingDocumentIfAllDone(task, email);
        }

        return toDetailResponse(task);
    }

    private void completeIncomingDocumentIfAllDone(Task finishedTask, String email) {
        IncomingDocument doc = finishedTask.getIncomingDocument();
        if (doc.getStatus() == IncomingDocumentStatus.COMPLETED) {
            return;
        }

        boolean allRootTasksDone = taskRepository.findByIncomingDocumentIdOrderByIdDesc(doc.getId())
                .stream()
                .filter(t -> t.getParentTask() == null)
                .allMatch(t -> t.getStatus() == TaskStatus.COMPLETED);
        if (!allRootTasksDone) {
            return;
        }

        doc.setStatus(IncomingDocumentStatus.COMPLETED);

        // Ghi vào nhật ký xử lý của văn bản (cùng định dạng với IncomingWorkflowService)
        String actor = userRepository.findByEmail(email).map(User::getFullName).orElse(email);
        String line = "[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                + " - " + actor + "]: Hoàn thành xử lý văn bản (kết thúc công việc \"" + finishedTask.getTitle() + "\")";
        String trace = doc.getTraceHeaderList();
        doc.setTraceHeaderList(trace == null || trace.isBlank() ? line : trace + "\n" + line);

        incomingDocumentRepository.save(doc);
        publisher.publishEvent(new IncomingDocumentChangedEvent(doc.getId()));
    }

    private String resolveAuthorRoleLabel(Task task, User user) {
        if (task.getAssignee() != null && task.getAssignee().getId().equals(user.getId())) {
            return "Chủ trì";
        }
        if (task.getSupervisors() != null && task.getSupervisors().stream().anyMatch(u -> u.getId().equals(user.getId()))) {
            return "Giám sát";
        }
        if (task.getSupporters() != null && task.getSupporters().stream().anyMatch(u -> u.getId().equals(user.getId()))) {
            return "Hỗ trợ";
        }
        if (task.getCreator() != null && task.getCreator().getId().equals(user.getId())) {
            return "Người giao";
        }
        return "Quản trị";
    }

    private TaskResponse toResponse(Task t, String mode) {
        List<String> supNames = (t.getSupervisors() == null) ? List.of() :
                t.getSupervisors().stream().map(User::getFullName).toList();

        List<String> sptNames = (t.getSupporters() == null) ? List.of() :
                t.getSupporters().stream().map(User::getFullName).toList();

        Integer latestProgress = t.getStatus() == TaskStatus.COMPLETED ? Integer.valueOf(100) : taskPostRepository
                .findFirstByTaskIdAndProgressPercentIsNotNullOrderByCreatedAtDesc(t.getId())
                .map(TaskPost::getProgressPercent)
                .orElseGet(() -> t.getStatus() == TaskStatus.COMPLETED ? 100 : 0);

        String roleLabel = switch (mode) {
            case "ASSIGNED" -> "Chủ trì";
            case "SUPPORTED" -> "Hỗ trợ";
            case "SUPERVISED" -> "Giám sát";
            case "CREATED" -> "Người giao";
            default -> "";
        };

        return TaskResponse.builder()
                .id(t.getId())
                .title(t.getTitle())
                .parentTaskId(t.getParentTask() != null ? t.getParentTask().getId() : null)
                .parentTaskTitle(t.getParentTask() != null ? t.getParentTask().getTitle() : null)
                .taskGroupId(t.getTaskGroup() != null ? t.getTaskGroup().getId() : null)
                .taskGroupName(t.getTaskGroup() != null ? t.getTaskGroup().getName() : null)
                .incomingDocumentId(t.getIncomingDocument() != null ? t.getIncomingDocument().getId() : null)
                .incomingArrivalNumber(t.getIncomingDocument() != null ? t.getIncomingDocument().getArrivalNumber() : null)
                .incomingSubject(t.getIncomingDocument() != null ? t.getIncomingDocument().getSubject() : null)
                .creatorName(t.getCreator() != null ? t.getCreator().getFullName() : "—")
                .assigneeId(t.getAssignee() != null ? t.getAssignee().getId() : null)
                .assigneeName(t.getAssignee() != null ? t.getAssignee().getFullName() : "—")
                .supervisorNames(supNames)
                .supporterNames(sptNames)
                .startTime(t.getStartTime())
                .endTime(t.getEndTime())
                .status(t.getStatus() != null ? t.getStatus().name() : "NEW")
                .progressPercent(latestProgress)
                .myRoleLabel(roleLabel)
                .build();
    }

    private boolean canViewTask(Task t, String email) {
        if (isAdminOrClerk()) return true;
        if (t.getCreator() != null && email.equalsIgnoreCase(t.getCreator().getEmail())) return true;
        if (t.getAssignee() != null && email.equalsIgnoreCase(t.getAssignee().getEmail())) return true;
        if (t.getSupervisors() != null && t.getSupervisors().stream().anyMatch(u -> email.equalsIgnoreCase(u.getEmail()))) return true;
        if (t.getSupporters() != null && t.getSupporters().stream().anyMatch(u -> email.equalsIgnoreCase(u.getEmail()))) return true;
        if (t.getParentTask() != null) {
            return canViewTask(t.getParentTask(), email);
        }
        return false;
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean isAdminOrClerk() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_CLERK"));
    }

    private TaskDetailResponse toDetailResponse(Task t) {
        List<TaskDetailResponse.SimpleUserDto> sups = (t.getSupervisors() == null) ? List.of() :
                t.getSupervisors().stream()
                        .map(u -> new TaskDetailResponse.SimpleUserDto(u.getId(), u.getFullName(), u.getEmail()))
                        .toList();

        List<TaskDetailResponse.SimpleUserDto> spts = (t.getSupporters() == null) ? List.of() :
                t.getSupporters().stream()
                        .map(u -> new TaskDetailResponse.SimpleUserDto(u.getId(), u.getFullName(), u.getEmail()))
                        .toList();

        List<TaskResponse> subTaskDtos = taskRepository.findByParentTaskIdOrderByIdAsc(t.getId())
                .stream()
                .map(sub -> toResponse(sub, ""))
                .toList();

        List<TaskPostResponse> postDtos = taskPostRepository.findByTaskIdOrderByCreatedAtDesc(t.getId())
                .stream()
                .map(this::toPostResponse)
                .toList();

        Integer latestProgress = t.getStatus() == TaskStatus.COMPLETED ? Integer.valueOf(100) : taskPostRepository
                .findFirstByTaskIdAndProgressPercentIsNotNullOrderByCreatedAtDesc(t.getId())
                .map(TaskPost::getProgressPercent)
                .orElseGet(() -> t.getStatus() == TaskStatus.COMPLETED ? 100 : 0);

        return TaskDetailResponse.builder()
                .id(t.getId())
                .title(t.getTitle())
                .description(t.getDescription())
                .parentTaskId(t.getParentTask() != null ? t.getParentTask().getId() : null)
                .parentTaskTitle(t.getParentTask() != null ? t.getParentTask().getTitle() : null)
                .taskGroupId(t.getTaskGroup() != null ? t.getTaskGroup().getId() : null)
                .taskGroupName(t.getTaskGroup() != null ? t.getTaskGroup().getName() : null)
                .incomingDocumentId(t.getIncomingDocument() != null ? t.getIncomingDocument().getId() : null)
                .incomingArrivalNumber(t.getIncomingDocument() != null ? t.getIncomingDocument().getArrivalNumber() : null)
                .incomingSubject(t.getIncomingDocument() != null ? t.getIncomingDocument().getSubject() : null)
                .incomingAttachmentName(t.getIncomingDocument() != null ? t.getIncomingDocument().getAttachmentName() : null)
                .creatorId(t.getCreator() != null ? t.getCreator().getId() : null)
                .creatorName(t.getCreator() != null ? t.getCreator().getFullName() : "—")
                .creatorEmail(t.getCreator() != null ? t.getCreator().getEmail() : "")
                .assigneeId(t.getAssignee() != null ? t.getAssignee().getId() : null)
                .assigneeName(t.getAssignee() != null ? t.getAssignee().getFullName() : "—")
                .assigneeEmail(t.getAssignee() != null ? t.getAssignee().getEmail() : "")
                .supervisors(sups)
                .supporters(spts)
                .startTime(t.getStartTime())
                .endTime(t.getEndTime())
                .status(t.getStatus() != null ? t.getStatus().name() : "NEW")
                .progressPercent(latestProgress)
                .subTasks(subTaskDtos)
                .posts(postDtos)
                .build();
    }

    private TaskPostResponse toPostResponse(TaskPost p) {
        List<TaskPostResponse.AttachmentDto> atts = (p.getAttachments() == null) ? List.of() :
                p.getAttachments().stream()
                        .map(a -> new TaskPostResponse.AttachmentDto(
                                a.getId(), a.getFileName(), a.getFileType(), a.getFileSize(), a.getUploadedAt()))
                        .toList();

        List<TaskPostResponse.CommentDto> cmts = (p.getComments() == null) ? List.of() :
                p.getComments().stream()
                        .map(c -> new TaskPostResponse.CommentDto(
                                c.getId(),
                                c.getAuthor().getId(),
                                c.getAuthor().getFullName(),
                                c.getAuthor().getEmail(),
                                c.getContent(),
                                c.getCreatedAt()))
                        .toList();

        return TaskPostResponse.builder()
                .id(p.getId())
                .authorId(p.getAuthor().getId())
                .authorName(p.getAuthor().getFullName())
                .authorEmail(p.getAuthor().getEmail())
                .authorRoleLabel(p.getAuthorRoleLabel())
                .weekLabel(p.getWeekLabel())
                .postType(p.getPostType())
                .progressPercent(p.getProgressPercent())
                .content(p.getContent())
                .createdAt(p.getCreatedAt())
                .attachments(atts)
                .comments(cmts)
                .build();
    }

    private List<User> findUsersOrThrow(Collection<Long> ids) {
        Set<Long> distinct = new LinkedHashSet<>(ids);
        List<User> users = userRepository.findAllById(distinct);
        if (users.size() != distinct.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Có người dùng được chọn không tồn tại");
        }
        return users;
    }

    private List<Department> assignableDepartments(Task parent) {
        Department own = (parent.getAssignee() != null) ? parent.getAssignee().getDepartment() : null;
        if (own == null) {
            return List.of();
        }
        List<Department> all = departmentRepository.findAll();
        Map<Long, Long> parentOf = new HashMap<>();
        for (Department d : all) {
            parentOf.put(d.getId(), d.getParent() != null ? d.getParent().getId() : null);
        }
        List<Department> result = new ArrayList<>();
        for (Department d : all) {
            Long current = d.getId();
            for (int depth = 0; current != null && depth < 50; depth++) {
                if (current.equals(own.getId())) {
                    result.add(d);
                    break;
                }
                current = parentOf.get(current);
            }
        }
        return result;
    }

    private Set<Long> assignableUserIds(Task parent) {
        Set<Long> ids = new HashSet<>();
        if (parent.getAssignee() != null) {
            ids.add(parent.getAssignee().getId());
        }
        if (parent.getSupervisors() != null) {
            parent.getSupervisors().forEach(u -> ids.add(u.getId()));
        }
        if (parent.getSupporters() != null) {
            parent.getSupporters().forEach(u -> ids.add(u.getId()));
        }

        List<Department> departments = assignableDepartments(parent);
        if (!departments.isEmpty()) {
            Set<Long> departmentIds = new HashSet<>();
            for (Department d : departments) {
                departmentIds.add(d.getId());
                if (d.getHead() != null) {
                    ids.add(d.getHead().getId());
                }
                if (d.getDeputies() != null) {
                    d.getDeputies().forEach(u -> ids.add(u.getId()));
                }
                if (d.getClerks() != null) {
                    d.getClerks().forEach(u -> ids.add(u.getId()));
                }
            }
            for (User u : userRepository.findAllByStatusOrderByFullNameAsc(UserStatus.ACTIVE)) {
                if (u.getDepartment() != null && departmentIds.contains(u.getDepartment().getId())) {
                    ids.add(u.getId());
                }
            }
        }

        if (parent.getTaskGroup() != null && parent.getTaskGroup().getMembers() != null) {
            parent.getTaskGroup().getMembers().forEach(u -> ids.add(u.getId()));
        }
        return ids;
    }

    private List<DispatchOptionsResponse.UserOption> filterUsers(List<DispatchOptionsResponse.UserOption> source,
                                                                 Set<Long> allowedIds) {
        if (source == null) {
            return List.of();
        }
        return source.stream().filter(u -> allowedIds.contains(u.getId())).toList();
    }
}