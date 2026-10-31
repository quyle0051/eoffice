package com.example.eoffice.entity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "document_types")
@Getter
@Setter
@NoArgsConstructor
public class DocumentType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    //kí hiệu
    @Column(length = 10)
    private String abbreviation;

    @Column(nullable = false, length = 20)
    private String category;
}
