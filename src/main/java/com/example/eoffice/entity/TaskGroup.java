package com.example.eoffice.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "task_groups")
@Getter
@Setter
@NoArgsConstructor
public class TaskGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private User creator;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "task_group_members",
            joinColumns = @JoinColumn(name = "task_group_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> members = new HashSet<>();
}
