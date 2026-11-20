package com.example.eoffice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class OutgoingDocumentResponse {
    private Long id;

    private Long documentBookId;
    private String documentBookName;
    private Long documentTypeId;
    private String documentTypeName;

    private String codeNumber;
    private String codeNotation;
    private LocalDate issuedDate;
    private String organName;
    private String subject;
    private String language;
    private Integer pageAmount;
    private String description;

    private String signerPosition;
    private String signerFullName;

    private String toOrgansSummary;
    private List<RecipientDto> recipients;

    private Integer priority;
    private Integer issuedAmount;
    private LocalDate dueDate;

    private LocalDateTime issuedTime;
    private String status;
    private String attachmentName;
    private String createdByName;
    private LocalDateTime createdAt;

    @Getter
    @AllArgsConstructor
    public static class RecipientDto {
        private Long id;
        private String organId;
        private String organName;
        private Integer copies;
        private String deliveryMethod;
        private String deliveryStatus;
        private LocalDateTime sentAt;
        private String trackingNote;
    }
}