package com.example.eoffice.search;

import com.example.eoffice.entity.OutgoingDocument;
import com.example.eoffice.repository.OutgoingDocumentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutgoingReindexService {

    private static final int BATCH_SIZE = 500;

    private final ElasticsearchOperations operations;
    private final OutgoingDocumentRepository repository;
    private final OutgoingDocumentIndexer indexer;

    @PersistenceContext
    private EntityManager entityManager;

    /** Xóa index cũ, tạo lại theo cấu hình rồi nạp toàn bộ văn bản đi từ MySQL. */
    @Transactional(readOnly = true)
    public long reindexAll() {
        IndexOperations ops = operations.indexOps(OutgoingDocumentIndex.class);
        if (ops.exists()) {
            ops.delete();
        }
        ops.createWithMapping();

        long total = 0;
        int page = 0;
        Page<OutgoingDocument> result;
        do {
            result = repository.findByDeletedFalse(PageRequest.of(page++, BATCH_SIZE, Sort.by("id")));
            if (!result.isEmpty()) {
                operations.save(result.getContent().stream().map(indexer::toIndex).toList());
                total += result.getNumberOfElements();
                log.info("Đã đồng bộ {} văn bản đi sang Elasticsearch", total);
            }
            entityManager.clear();
        } while (result.hasNext());
        return total;
    }
}