package com.example.eoffice.search;

/** Phát ra mỗi khi một văn bản đến thay đổi (tạo, sửa, xóa, đổi luồng xử lý, đổi quyền xem). */
public record IncomingDocumentChangedEvent(Long id) {
}