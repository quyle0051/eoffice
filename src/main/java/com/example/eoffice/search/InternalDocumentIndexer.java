package com.example.eoffice.search;

import com.example.eoffice.entity.InternalDocument;
import com.example.eoffice.repository.InternalDocumentRepository;
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

@Component
@RequiredArgsConstructor
@Slf4j
public class InternalDocumentIndexer {

    private final ElasticsearchOperations operations;
    private final InternalDocumentRepository repository;

    public InternalDocumentIndex toIndex(InternalDocument d) {
        InternalDocumentIndex x = new InternalDocumentIndex();
        x.setId(d.getId());
        x.setSortKey(d.getId());
        x.setSubject(nfc(d.getSubject()));
        x.setSignerFullName(nfc(d.getSignerFullName()));
        x.setCodeNumber(d.getCodeNumber());
        x.setCodeNotation(d.getCodeNotation());
        x.setDocumentBookId(d.getDocumentBook() != null ? d.getDocumentBook().getId() : null);
        x.setDocumentTypeId(d.getDocumentType() != null ? d.getDocumentType().getId() : null);
        x.setStatus(d.getStatus() != null ? d.getStatus().name() : "REGISTERED");
        x.setPriority(d.getPriority());
        x.setIssuedDate(d.getIssuedDate());
        x.setDueDate(d.getDueDate());
        x.setHasAttachment(d.getAttachmentName() != null);
        return x;
    }

    private static String nfc(String s) {
        return s == null ? null : Normalizer.normalize(s, Normalizer.Form.NFC);
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onChanged(InternalDocumentChangedEvent event) {
        try {
            repository.findByIdAndDeletedFalse(event.id()).ifPresentOrElse(
                    d -> operations.save(toIndex(d)),
                    () -> operations.delete(String.valueOf(event.id()), InternalDocumentIndex.class));
        } catch (Exception ex) {
            log.warn("Không đồng bộ được văn bản nội bộ {} sang Elasticsearch: {}", event.id(), ex.getMessage());
        }
    }
}