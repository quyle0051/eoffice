package com.example.eoffice.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class IncomingDocumentResponse {
    private Long id;

    private Long documentBookId;
    private String documentBookName;

    private Long documentTypeId;
    private String documentTypeName;
    private String documentTypeAbbreviation;

    private Long arrivalNumber;
    private LocalDate arrivalDate;

    private String codeNumber;
    private String codeNotation;
    private LocalDate issuedDate;
    private String organName;
    private String subject;
    private String language;
    private Integer pageAmount;
    private Integer docOrdinal;
    private String description;
    private String signerPosition;
    private String signerFullName;
    private Integer priority;
    private String toPlaces;
    private String traceHeaderList;
    private LocalDate dueDate;
    private String status;
    private String attachmentName;
    private Long currentHandlerId;
    private String currentHandlerEmail;
    private String currentHandlerName;
}
