package com.example.eoffice.search;

/** Phát ra mỗi khi một văn bản đi thay đổi (tạo, sửa, phát hành, lưu trữ, xóa, đính kèm tệp). */
public record OutgoingDocumentChangedEvent(Long id) {
}