package com.example.eoffice.search;

import com.example.eoffice.entity.IncomingDocument;
import com.example.eoffice.repository.IncomingDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class IncomingDocumentIndexer {

    private final ElasticsearchOperations operations;
    private final IncomingDocumentRepository repository;

    /** Chuyển entity MySQL thành bản ghi Elasticsearch. Cần gọi trong một transaction (có đọc quan hệ lazy). */
    public IncomingDocumentIndex toIndex(IncomingDocument d) {
        IncomingDocumentIndex x = new IncomingDocumentIndex();
        x.setId(d.getId());
        x.setSubject(nfc(d.getSubject()));
        x.setOrganName(nfc(d.getOrganName()));
        x.setSignerFullName(nfc(d.getSignerFullName()));
        x.setCodeNumber(d.getCodeNumber());
        x.setCodeNotation(d.getCodeNotation());
        x.setDocumentBookId(d.getDocumentBook() != null ? d.getDocumentBook().getId() : null);
        x.setDocumentTypeId(d.getDocumentType() != null ? d.getDocumentType().getId() : null);
        x.setArrivalNumber(d.getArrivalNumber());
        x.setStatus(d.getStatus() != null ? d.getStatus().name() : "NEW");
        x.setPriority(d.getPriority());
        x.setArrivalDate(d.getArrivalDate());
        x.setIssuedDate(d.getIssuedDate());
        x.setDueDate(d.getDueDate());
        x.setHasAttachment(d.getAttachmentName() != null);

        Set<String> viewers = new HashSet<>();
        if (d.getCurrentHandler() != null && d.getCurrentHandler().getEmail() != null) {
            viewers.add(d.getCurrentHandler().getEmail().toLowerCase());
        }
        if (d.getParticipants() != null) {
            d.getParticipants().forEach(u -> {
                if (u.getEmail() != null) {
                    viewers.add(u.getEmail().toLowerCase());
                }
            });
        }
        x.setViewerEmails(new ArrayList<>(viewers));
        return x;
    }

    /** Chuẩn hóa Unicode về dạng NFC để tiếng Việt có dấu luôn được bỏ dấu đúng. */
    private static String nfc(String s) {
        return s == null ? null : Normalizer.normalize(s, Normalizer.Form.NFC);
    }

    /** Đồng bộ một văn bản sang Elasticsearch sau khi giao dịch MySQL đã commit. */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onChanged(IncomingDocumentChangedEvent event) {
        try {
            repository.findByIdAndDeletedFalse(event.id()).ifPresentOrElse(
                    d -> operations.save(toIndex(d)),
                    () -> operations.delete(String.valueOf(event.id()), IncomingDocumentIndex.class));
        } catch (Exception ex) {
            log.warn("Không đồng bộ được văn bản đến {} sang Elasticsearch: {}", event.id(), ex.getMessage());
        }
    }
}