package com.example.eoffice.entity;

public enum IncomingDocumentStatus {
    NEW,                        // Mới vào sổ (Văn thư vừa tạo)
    WAITING_OFFICE_CHIEF,       // Đang chờ Chánh văn phòng cho ý kiến
    WAITING_DIRECTOR,           // Đang chờ Giám đốc ký duyệt / bút phê
    APPROVED_WAITING_DISPATCH,  // Giám đốc đã duyệt, chờ Văn thư phân phối
    DISPATCHED,                 // Văn thư đã chuyển cho Phòng ban / Nhóm công việc
    COMPLETED                   // Hoàn thành xử lý
}
