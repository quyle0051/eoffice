package com.example.eoffice.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_group_id")
    private TaskGroup taskGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incoming_document_id")
    private IncomingDocument incomingDocument;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    /** Người chủ trì (1 người) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id", nullable = false)
    private User assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id")
    private User supervisor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supporter_id")
    private User supporter;

    /** Danh sách nhiều Người giám sát */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "task_supervisors",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> supervisors = new HashSet<>();

    /** Danh sách nhiều Người hỗ trợ (gồm user lẻ, Trưởng/Phó phòng ban, thành viên nhóm) */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "task_supporters",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> supporters = new HashSet<>();

    @Column(name = "progress_report_required")
    private boolean progressReportRequired = true;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status = TaskStatus.NEW;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "dispatch_summary", columnDefinition = "LONGTEXT")
    private String dispatchSummary;
}
