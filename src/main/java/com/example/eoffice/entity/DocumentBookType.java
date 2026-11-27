package com.example.eoffice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "document_book_types")
@Getter
@Setter
@NoArgsConstructor
public class DocumentBookType {

    public static final String INCOMING = "INCOMING";
    public static final String OUTGOING = "OUTGOING";
    public static final String INTERNAL = "INTERNAL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;
}