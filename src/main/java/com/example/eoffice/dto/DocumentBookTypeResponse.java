package com.example.eoffice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DocumentBookTypeResponse {
    private Long id;
    private String code;
    private String name;
}