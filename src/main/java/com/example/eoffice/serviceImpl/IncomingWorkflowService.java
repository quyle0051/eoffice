package com.example.eoffice.serviceImpl;

import com.example.eoffice.dto.DispatchOptionsResponse;
import com.example.eoffice.dto.IncomingDispatchRequest;
import com.example.eoffice.dto.IncomingDocumentResponse;
import com.example.eoffice.dto.IncomingWorkflowRequest;
import com.example.eoffice.entity.*;
import com.example.eoffice.repository.*;
import com.example.eoffice.search.IncomingDocumentChangedEvent;
import com.example.eoffice.service.IncomingDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class IncomingWorkflowService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final IncomingDocumentRepository docRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final TaskGroupRepository taskGroupRepository;
    private final IncomingDocumentService incomingDocumentService;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher publisher;

    @Transactional(readOnly = true)
    @Cacheable("dispatchOptions")
    public DispatchOptionsResponse getDispatchOptions() {
        List<DispatchOptionsResponse.UserOption> chiefs = userRepository
                .findByRoleNameAndStatus("OFFICE_CHIEF", UserStatus.ACTIVE)
                .stream().map(this::toUserOption).toList();

        List<DispatchOptionsResponse.UserOption> directors = userRepository
                .findByRoleNameAndStatus("DIRECTOR", UserStatus.ACTIVE)
                .stream().map(this::toUserOption).toList();

        // Danh sách cho popup Người chủ trì & Người giám sát (Giám đốc, Phó GĐ, Chánh VP, Trưởng phòng, Phó phòng)
        List<String> leaderRoles = List.of("DIRECTOR", "DEPUTY_DIRECTOR", "OFFICE_CHIEF", "HEAD", "DEPUTY");
        List<DispatchOptionsResponse.UserOption> leaders = userRepository
                .findByRoleNamesAndStatus(leaderRoles, UserStatus.ACTIVE)
                .stream().map(this::toUserOption).toList();

        List<DispatchOptionsResponse.UserOption> allUsers = userRepository
                .findAllByStatusOrderByFullNameAsc(UserStatus.ACTIVE)
                .stream().map(this::toUserOption).toList();

        List<DispatchOptionsResponse.DepartmentOption> depts = departmentRepository
                .findAllByOrderByIdAsc()
                .stream().map(d -> {
                    List<DispatchOptionsResponse.UserOption> deputyList = (d.getDeputies() == null)
                            ? List.of()
                            : d.getDeputies().stream().map(this::toUserOption).toList();
                    List<DispatchOptionsResponse.UserOption> clerkList = (d.getClerks() == null)
                            ? List.of()
                            : d.getClerks().stream().map(this::toUserOption).toList();

                    Long firstDeputyId = deputyList.isEmpty() ? null : deputyList.get(0).getId();
                    String allDeputyNames = deputyList.isEmpty() ? null :
                            deputyList.stream().map(DispatchOptionsResponse.UserOption::getFullName).reduce((a, b) -> a + ", " + b).orElse(null);
                    String firstDeputyEmail = deputyList.isEmpty() ? null : deputyList.get(0).getEmail();

                    return new DispatchOptionsResponse.DepartmentOption(
                            d.getId(),
                            d.getName(),
                            d.getHead() != null ? d.getHead().getId() : null,
                            d.getHead() != null ? d.getHead().getFullName() : null,
                            d.getHead() != null ? d.getHead().getEmail() : null,
                            firstDeputyId,
                            allDeputyNames,
                            firstDeputyEmail,
                            deputyList,
                            clerkList
                        );
                }).toList();

        List<DispatchOptionsResponse.TaskGroupOption> groups = taskGroupRepository
                .findAllByOrderByIdAsc()
                .stream().map(g -> {
                    List<DispatchOptionsResponse.UserOption> memberOpts = (g.getMembers() == null)
                            ? List.of()
                            : g.getMembers().stream().map(this::toUserOption).toList();
                    return new DispatchOptionsResponse.TaskGroupOption(
                            g.getId(),
                            g.getName(),
                            memberOpts.size(),
                            memberOpts
                    );
                }).toList();

        return new DispatchOptionsResponse(chiefs, directors, leaders, allUsers, depts, groups);
    }

    @Transactional(readOnly = true)
    public IncomingDispatchRequest getDispatchState(Long docId) {
        Task latestTask = taskRepository
                .findFirstByIncomingDocumentIdAndParentTaskIsNullOrderByIdDesc(docId)
                .orElse(null);
        if (latestTask == null) {
            return null;
        }
        String summaryJson = latestTask.getDispatchSummary();
        if (summaryJson != null && summaryJson.trim().startsWith("{")) {
            try {
                return objectMapper.readValue(summaryJson, IncomingDispatchRequest.class);
            } catch (JacksonException ignored) {
                // Nếu dữ liệu cũ không phải JSON thì fallback xuống đoạn dựng lại bên dưới
            }
        }

        // Fallback cho các bản ghi đã lưu trước khi có JSON
        IncomingDispatchRequest state = new IncomingDispatchRequest();
        if (latestTask.getAssignee() != null) {
            state.setMainAssigneeId(latestTask.getAssignee().getId());
        }
        if (latestTask.getSupervisors() != null) {
            state.setSupervisorIds(latestTask.getSupervisors().stream().map(User::getId).toList());
        }
        if (latestTask.getSupporters() != null) {
            state.setSupporterUserIds(latestTask.getSupporters().stream().map(User::getId).toList());
        }
        if (latestTask.getTaskGroup() != null) {
            state.setPrimaryTaskGroupId(latestTask.getTaskGroup().getId());
        }
        return state;
    }

    /** API RIÊNG: Phân phối văn bản đến & Lưu trạng thái để hiển thị lại khi mở trang */
    @Transactional
    public IncomingDocumentResponse dispatchDocument(Long docId, String email, IncomingDispatchRequest req) {
        IncomingDocument doc = findDocOrThrow(docId);
        User creator = findUserByEmailOrThrow(email);

        if (req.getMainAssigneeId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn 1 Người chủ trì");
        }
        User mainAssignee = userRepository.findById(req.getMainAssigneeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy Người chủ trì"));

        Set<User> supervisors = new LinkedHashSet<>();
        if (req.getSupervisorIds() != null && !req.getSupervisorIds().isEmpty()) {
            supervisors.addAll(userRepository.findAllById(req.getSupervisorIds()));
        }

        Set<User> supporters = new LinkedHashSet<>();
        if (req.getSupporterUserIds() != null && !req.getSupporterUserIds().isEmpty()) {
            supporters.addAll(userRepository.findAllById(req.getSupporterUserIds()));
        }

        TaskGroup primaryGroup = (req.getPrimaryTaskGroupId() != null)
                ? taskGroupRepository.findById(req.getPrimaryTaskGroupId()).orElse(null)
                : null;

        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = doc.getDueDate() != null ? doc.getDueDate().atTime(17, 0) : start.plusDays(7);

        if (req.getRows() != null) {
            for (IncomingDispatchRequest.DispatchRowItem row : req.getRows()) {
                if ("Chủ trì".equalsIgnoreCase(row.getRoleLabel()) && row.getDueDate() != null) {
                    end = row.getDueDate().atTime(17, 0);
                }
            }
        }

        // Nếu văn bản đã có Task từ lần phân phối trước thì cập nhật lại Task đó, nếu chưa có thì tạo mới
        Task existingTask  = taskRepository
                .findFirstByIncomingDocumentIdAndParentTaskIsNullOrderByIdDesc(docId)
                .orElse(null);

        Set<Long> previousMemberIds = new HashSet<>();
        if (existingTask != null) {
            collectMemberIds(existingTask, previousMemberIds);
        }
        Task task = (existingTask != null) ? existingTask : new Task();

        task.setTitle("Xử lý VB đến số " + doc.getArrivalNumber() + ": " + doc.getSubject());
        task.setIncomingDocument(doc);
        task.setTaskGroup(primaryGroup);
        task.setCreator(creator);
        task.setAssignee(mainAssignee);
        task.setSupervisors(supervisors);
        task.setSupporters(supporters);
        task.setStartTime(start);
        task.setEndTime(end);
        if (task.getStatus() == null) {
            task.setStatus(TaskStatus.NEW);
        }
        task.setDescription(doc.getTraceHeaderList());

        try {
            task.setDispatchSummary(objectMapper.writeValueAsString(req));
        } catch (JacksonException e) {
            task.setDispatchSummary(null);
        }
        taskRepository.save(task);

        // Cấp quyền xem văn bản đến cho tất cả Chủ trì, Giám sát và Hỗ trợ
        doc.getParticipants().add(mainAssignee);
        doc.getParticipants().addAll(supervisors);
        doc.getParticipants().addAll(supporters);

        // Phân phối lại: thu hồi quyền xem của người không còn vai trò nào trong văn bản
        pruneParticipants(doc, previousMemberIds);

        doc.setStatus(IncomingDocumentStatus.DISPATCHED);
        doc.setCurrentHandler(mainAssignee);
        String places = (req.getToPlacesSummary() != null && !req.getToPlacesSummary().isBlank())
                ? req.getToPlacesSummary().trim()
                : mainAssignee.getFullName();
        doc.setToPlaces(places);

        appendTraceLog(doc, "Văn thư " + creator.getFullName() + " phân phối giao việc",
                "Chủ trì: " + mainAssignee.getFullName() + " | Phối hợp/Giám sát: " + places);
        docRepository.save(doc);
        publisher.publishEvent(new IncomingDocumentChangedEvent(doc.getId()));

        return incomingDocumentService.findById(docId);
    }

    private static final Set<String> WORKFLOW_LEADER_ROLES = Set.of("OFFICE_CHIEF", "DIRECTOR", "DEPUTY_DIRECTOR");

    private void collectMemberIds(Task t, Set<Long> ids) {
        if (t.getAssignee() != null) ids.add(t.getAssignee().getId());
        if (t.getSupervisors() != null) t.getSupervisors().forEach(u -> ids.add(u.getId()));
        if (t.getSupporters() != null) t.getSupporters().forEach(u -> ids.add(u.getId()));
    }

    private void pruneParticipants(IncomingDocument doc, Set<Long> previousMemberIds) {
        if (previousMemberIds.isEmpty()) return;

        Set<Long> stillInvolved = new HashSet<>();
        for (Task t : taskRepository.findByIncomingDocumentIdOrderByIdDesc(doc.getId())) {
            collectMemberIds(t, stillInvolved);
        }

        doc.getParticipants().removeIf(u ->
                previousMemberIds.contains(u.getId())
                        && !stillInvolved.contains(u.getId())
                        && !isWorkflowLeader(u));
    }

    private boolean isWorkflowLeader(User u) {
        return u.getRoles() != null
                && u.getRoles().stream().anyMatch(r -> WORKFLOW_LEADER_ROLES.contains(r.getName()));
    }

    private DispatchOptionsResponse.UserOption toUserOption(User u) {
        String deptName = (u.getDepartment() != null) ? u.getDepartment().getName() : "Ban Giám đốc / Chung";
        String rolesStr = (u.getRoles() == null) ? "" : u.getRoles().stream()
                .map(r -> switch (r.getName()) {
                    case "DIRECTOR" -> "Giám đốc";
                    case "DEPUTY_DIRECTOR" -> "Phó Giám đốc";
                    case "OFFICE_CHIEF" -> "Chánh VP";
                    case "HEAD" -> "Trưởng phòng";
                    case "DEPUTY" -> "Phó phòng";
                    case "CLERK" -> "Văn thư";
                    case "STAFF" -> "Chuyên viên";
                    default -> r.getName();
                })
                .reduce((a, b) -> a + ", " + b).orElse("");
        return new DispatchOptionsResponse.UserOption(u.getId(), u.getFullName(), u.getEmail(), deptName, rolesStr);
    }

    /** Bước 1: Văn thư trình đích danh Chánh văn phòng được chọn */
    @Transactional
    public IncomingDocumentResponse submitToOfficeChief(Long docId, String email, IncomingWorkflowRequest req) {
        IncomingDocument doc = findDocOrThrow(docId);
        User sender = findUserByEmailOrThrow(email);

        if (req.getReceiverId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn Chánh văn phòng nhận văn bản");
        }
        User chief = userRepository.findById(req.getReceiverId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy Chánh văn phòng đã chọn"));

        String note = cleanNote(req.getDirectiveNote(), "Kính trình Chánh văn phòng xem xét.");
        doc.setStatus(IncomingDocumentStatus.WAITING_OFFICE_CHIEF);
        doc.setCurrentHandler(chief);
        doc.getParticipants().add(sender);
        doc.getParticipants().add(chief);

        appendTraceLog(doc, "Văn thư " + sender.getFullName() + " -> " + chief.getFullName(), note);
        docRepository.save(doc);
        publisher.publishEvent(new IncomingDocumentChangedEvent(doc.getId()));

        return incomingDocumentService.findById(docId);
    }

    /** Bước 2: Đích danh Chánh văn phòng được chọn cho ý kiến & trình Giám đốc được chọn */
    @Transactional
    public IncomingDocumentResponse forwardToDirector(Long docId, String email, IncomingWorkflowRequest req) {
        IncomingDocument doc = findDocOrThrow(docId);
        User sender = findUserByEmailOrThrow(email);

        // Kiểm tra đúng Chánh văn phòng được giao mới được phép cho ý kiến
        if (doc.getCurrentHandler() == null || !doc.getCurrentHandler().getId().equals(sender.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Chỉ Chánh văn phòng được phân công mới có quyền cho ý kiến văn bản này");
        }
        if (req.getReceiverId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn Giám đốc trình duyệt");
        }
        if (req.getDirectiveNote() == null || req.getDirectiveNote().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập ý kiến của Chánh văn phòng");
        }

        User director = userRepository.findById(req.getReceiverId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy Giám đốc đã chọn"));

        doc.setStatus(IncomingDocumentStatus.WAITING_DIRECTOR);
        doc.setCurrentHandler(director);
        doc.getParticipants().add(director);

        appendTraceLog(doc, "Chánh VP " + sender.getFullName() + " -> GĐ " + director.getFullName(), req.getDirectiveNote().trim());
        docRepository.save(doc);
        publisher.publishEvent(new IncomingDocumentChangedEvent(doc.getId()));
        return incomingDocumentService.findById(docId);
    }

    /** Bước 3: Đích danh Giám đốc được chọn bút phê duyệt & chuyển lại Văn thư */
    @Transactional
    public IncomingDocumentResponse approveByDirector(Long docId, String email, IncomingWorkflowRequest req) {
        IncomingDocument doc = findDocOrThrow(docId);
        User sender = findUserByEmailOrThrow(email);

        // Kiểm tra đúng Giám đốc được trình mới có quyền bút phê
        if (doc.getCurrentHandler() == null || !doc.getCurrentHandler().getId().equals(sender.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Chỉ Giám đốc được trình văn bản này mới có quyền bút phê duyệt");
        }
        if (req.getDirectiveNote() == null || req.getDirectiveNote().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng nhập bút phê chỉ đạo của Giám đốc");
        }

        doc.setStatus(IncomingDocumentStatus.APPROVED_WAITING_DISPATCH);
        doc.setCurrentHandler(null); // Trả về cho bộ phận Văn thư phân phối

        appendTraceLog(doc, "Giám đốc " + sender.getFullName() + " bút phê", req.getDirectiveNote().trim());
        docRepository.save(doc);
        publisher.publishEvent(new IncomingDocumentChangedEvent(doc.getId()));
        return incomingDocumentService.findById(docId);
    }

    /** Bước 4: Văn thư phân phối tạo Công việc (Task) và cấp quyền xem văn bản cho những người nhận */
    @Transactional
    public IncomingDocumentResponse dispatchToHandlers(Long docId, String email, IncomingWorkflowRequest req) {
        IncomingDocument doc = findDocOrThrow(docId);
        User creator = findUserByEmailOrThrow(email);

        Map<Long, User> assignees = new LinkedHashMap<>();
        List<String> recipientPlaces = new ArrayList<>();

        boolean sendHead = req.getIncludeHead() == null || req.getIncludeHead();
        boolean sendDeputy = req.getIncludeDeputy() != null && req.getIncludeDeputy();

        if (req.getDepartmentIds() != null && !req.getDepartmentIds().isEmpty()) {
            for (Department d : departmentRepository.findAllById(req.getDepartmentIds())) {
                recipientPlaces.add(d.getName());
                if (sendHead && d.getHead() != null) {
                    assignees.put(d.getHead().getId(), d.getHead());
                }
                if (sendDeputy && d.getDeputies() != null) {
                    for (User dp : d.getDeputies()) {
                        assignees.put(dp.getId(), dp);
                    }
                }
            }
        }

        if (req.getAssigneeIds() != null && !req.getAssigneeIds().isEmpty()) {
            for (User u : userRepository.findAllById(req.getAssigneeIds())) {
                assignees.put(u.getId(), u);
                recipientPlaces.add(u.getFullName());
            }
        }

        if (assignees.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Vui lòng chọn ít nhất 1 người xử lý chính hoặc phòng ban đã có Trưởng/Phó phòng");
        }

        TaskGroup taskGroup = (req.getTaskGroupId() != null)
                ? taskGroupRepository.findById(req.getTaskGroupId()).orElse(null)
                : null;
        User supervisor = (req.getSupervisorId() != null)
                ? userRepository.findById(req.getSupervisorId()).orElse(null)
                : null;
        User supporter = (req.getSupporterId() != null)
                ? userRepository.findById(req.getSupporterId()).orElse(null)
                : null;

        String title = (req.getTaskTitle() != null && !req.getTaskTitle().isBlank())
                ? req.getTaskTitle().trim()
                : "Xử lý VB đến số " + doc.getArrivalNumber() + ": " + doc.getSubject();

        LocalDateTime start = req.getStartTime() != null ? req.getStartTime() : LocalDateTime.now();
        LocalDateTime end = req.getEndTime() != null
                ? req.getEndTime()
                : (doc.getDueDate() != null ? doc.getDueDate().atTime(17, 0) : start.plusDays(7));

        String desc = cleanNote(req.getDirectiveNote(), doc.getTraceHeaderList());

        List<Task> tasksToSave = new ArrayList<>();
        for (User assignee : assignees.values()) {
            Task task = new Task();
            task.setTitle(title);
            task.setTaskGroup(taskGroup);
            task.setIncomingDocument(doc);
            task.setCreator(creator);
            task.setAssignee(assignee);
            task.setProgressReportRequired(req.getProgressReportRequired() == null || req.getProgressReportRequired());
            task.setStartTime(start);
            task.setEndTime(end);
            task.setStatus(TaskStatus.NEW);
            task.setDescription(desc);
            tasksToSave.add(task);

            // Cấp quyền xem văn bản đến cho người xử lý chính
            doc.getParticipants().add(assignee);
        }
        taskRepository.saveAll(tasksToSave);

        // Cấp quyền xem văn bản cho cả Người theo dõi và Người hỗ trợ (nếu có)
        if (supervisor != null) doc.getParticipants().add(supervisor);
        if (supporter != null) doc.getParticipants().add(supporter);

        doc.setStatus(IncomingDocumentStatus.DISPATCHED);
        doc.setCurrentHandler(null);
        String placesStr = String.join(", ", recipientPlaces);
        doc.setToPlaces(placesStr);
        appendTraceLog(doc, "Văn thư " + creator.getFullName() + " phân phối cho [" + placesStr + "]", title);
        docRepository.save(doc);
        publisher.publishEvent(new IncomingDocumentChangedEvent(doc.getId()));

        return incomingDocumentService.findById(docId);
    }

    private void appendTraceLog(IncomingDocument doc, String actorLabel, String content) {
        String timeStr = LocalDateTime.now().format(TIME_FMT);
        String line = "[" + timeStr + " - " + actorLabel + "]: " + content;
        if (doc.getTraceHeaderList() == null || doc.getTraceHeaderList().isBlank()) {
            doc.setTraceHeaderList(line);
        } else {
            doc.setTraceHeaderList(doc.getTraceHeaderList() + "\n" + line);
        }
    }

    private IncomingDocument findDocOrThrow(Long id) {
        return docRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy văn bản đến"));
    }

    private User findUserByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không tìm thấy người dùng"));
    }

    private String cleanNote(String note, String fallback) {
        return (note == null || note.isBlank()) ? fallback : note.trim();
    }
}