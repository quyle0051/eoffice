package com.example.eoffice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "organs")
@Getter
@Setter
@NoArgsConstructor
public class Organ {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 13.1 Mã định danh cơ quan, tổ chức (String 13) */
    @Column(name = "organ_id", nullable = false, unique = true, length = 13)
    private String organId;

    /** 13.2 Tên cơ quan, tổ chức (String 200) */
    @Column(name = "organ_name", nullable = false, length = 200)
    private String organName;

    @Column(length = 300)
    private String address;

    @Column(length = 150)
    private String email;

    @Column(length = 30)
    private String phone;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;
}