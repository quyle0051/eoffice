package com.example.eoffice.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class InternalDocumentResponse {
    private Long id;

    private Long documentBookId;
    private String documentBookName;
    private Long documentTypeId;
    private String documentTypeName;
    private String documentTypeAbbreviation;

    private Long serialNumber;
    private String codeNumber;
    private String codeNotation;
    private LocalDate issuedDate;
    private String organName;
    private String toPlaces;
    private String subject;
    private String language;
    private Integer pageAmount;
    private String description;
    private String signerPosition;
    private String signerFullName;
    private Integer priority;
    private Integer issuedAmount;
    private LocalDate dueDate;
    private LocalDateTime issuedTime;
    private String status;
    private String attachmentName;
    private String createdByName;
    private LocalDateTime createdAt;
}