package com.example.eoffice.entity;

import jakarta.persistence.*;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(name = "document_books")
@Getter
@Setter
@NoArgsConstructor
public class DocumentBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private DocumentBook parent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_type_id", nullable = false)
    private DocumentBookType bookType;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "short_name", length = 50)
    private String shortName;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;
}
