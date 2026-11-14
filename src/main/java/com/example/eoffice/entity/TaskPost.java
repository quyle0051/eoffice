package com.example.eoffice.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "task_posts")
@Getter
@Setter
@NoArgsConstructor
public class TaskPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "author_role_label", length = 50)
    private String authorRoleLabel;

    @Column(name = "week_label", length = 100)
    private String weekLabel;

    /** PROGRESS_REPORT (Báo cáo tuần) | DISCUSSION (Thảo luận) | DIRECTIVE (Chỉ đạo/Đôn đốc) */
    @Column(name = "post_type", nullable = false, length = 30)
    private String postType = "PROGRESS_REPORT";

    @Column(name = "progress_percent")
    private Integer progressPercent;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskPostAttachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<TaskPostComment> comments = new ArrayList<>();
}
