package com.example.eoffice.search;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

/** Lần đầu chạy (chưa có index) thì tự dựng index từ MySQL. Elasticsearch chưa bật thì chỉ cảnh báo. */
@Component
@RequiredArgsConstructor
@Slf4j
public class SearchStartup implements ApplicationRunner {

    private final ElasticsearchOperations operations;
    private final IncomingReindexService incomingReindexService;
    private final OutgoingReindexService outgoingReindexService;
    private final InternalReindexService internalReindexService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!operations.indexOps(IncomingDocumentIndex.class).exists()) {
                log.info("Chưa có index incoming_documents, bắt đầu dựng từ MySQL...");
                log.info("Đã dựng xong index incoming_documents: {} văn bản", incomingReindexService.reindexAll());
            }
            if (!operations.indexOps(OutgoingDocumentIndex.class).exists()) {
                log.info("Chưa có index outgoing_documents, bắt đầu dựng từ MySQL...");
                log.info("Đã dựng xong index outgoing_documents: {} văn bản", outgoingReindexService.reindexAll());
            }
            if (!operations.indexOps(InternalDocumentIndex.class).exists()) {
                log.info("Chưa có index internal_documents, bắt đầu dựng từ MySQL...");
                log.info("Đã dựng xong index internal_documents: {} văn bản", internalReindexService.reindexAll());
            }
        } catch (Exception e) {
            log.warn("Elasticsearch chưa sẵn sàng, hệ thống dùng tìm kiếm MySQL: {}", e.getMessage());
        }
    }
}