package com.example.eoffice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DocumentTypeResponse {
    private Long id;
    private String name;
    private String abbreviation;
    private String category;
}
