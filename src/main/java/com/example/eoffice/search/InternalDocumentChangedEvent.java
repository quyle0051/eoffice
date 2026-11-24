package com.example.eoffice.search;

/** Phát ra mỗi khi một văn bản nội bộ thay đổi (tạo, sửa, ban hành, lưu trữ, xóa, đính kèm tệp). */
public record InternalDocumentChangedEvent(Long id) {
}