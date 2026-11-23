package com.example.eoffice.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Điều kiện tìm kiếm văn bản đi. Các tên tham số cũ được giữ nguyên. */
@Getter
@Setter
public class OutgoingSearchRequest {

    private String keyword;
    private Long bookId;
    private Long typeId;

    /** REGISTERED, ISSUED, ARCHIVED */
    private String status;
    private Integer priority;
    private String signer;

    /** Tên cơ quan nhận văn bản. */
    private String recipient;

    /** Khoảng ngày ban hành. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    /** Khoảng hạn trả lời. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueTo;

    private Boolean overdue;
    private Boolean hasAttachment;

    private int page = 0;
    private int size = 10;
}