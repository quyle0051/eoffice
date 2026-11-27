package com.example.eoffice.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DocumentBookResponse {
    private Long id;
    private String name;
    private String shortName;
    private Long parentId;
    private String parentName;
    private Long bookTypeId;
    private String bookTypeCode;
    private String bookTypeName;
}
