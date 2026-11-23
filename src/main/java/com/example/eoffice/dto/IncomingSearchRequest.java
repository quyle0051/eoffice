package com.example.eoffice.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Điều kiện tìm kiếm văn bản đến (ô tìm nhanh + tìm nâng cao). Tên tham số giữ nguyên các tên cũ. */
@Getter
@Setter
public class IncomingSearchRequest {

    private String keyword;
    private Long documentBookId;
    private Long documentTypeId;

    /** NEW, WAITING_OFFICE_CHIEF, WAITING_DIRECTOR, APPROVED_WAITING_DISPATCH, DISPATCHED, COMPLETED */
    private String status;
    private Integer priority;
    private String signer;
    private String organ;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate arrivalFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate arrivalTo;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueTo;

    /** true = chỉ văn bản đã quá hạn và chưa hoàn thành. */
    private Boolean overdue;
    private Boolean hasAttachment;

    private int page = 0;
    private int size = 10;
}