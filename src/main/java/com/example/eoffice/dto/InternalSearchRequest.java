package com.example.eoffice.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Điều kiện tìm kiếm văn bản nội bộ. */
@Getter
@Setter
public class InternalSearchRequest {

    private String keyword;
    private Long bookId;
    private Long typeId;

    /** REGISTERED, ISSUED, ARCHIVED */
    private String status;
    private Integer priority;
    private String signer;

    /** Khoảng ngày ban hành. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    /** Khoảng hạn xử lý. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueTo;

    private Boolean overdue;
    private Boolean hasAttachment;

    private int page = 0;
    private int size = 10;
}