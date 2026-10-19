package com.example.eoffice.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DocumentBookResponse {
    private Long id;
    private String name;
    private String shortName;
    private Integer standardDays;
    private Long parentId;
    private String parentName;
}
