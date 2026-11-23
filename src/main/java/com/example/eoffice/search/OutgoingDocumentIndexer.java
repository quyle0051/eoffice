package com.example.eoffice.search;

import com.example.eoffice.entity.OutgoingDocument;
import com.example.eoffice.repository.OutgoingDocumentRepository;
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
public class OutgoingDocumentIndexer {

    private final ElasticsearchOperations operations;
    private final OutgoingDocumentRepository repository;

    public OutgoingDocumentIndex toIndex(OutgoingDocument d) {
        OutgoingDocumentIndex x = new OutgoingDocumentIndex();
        x.setId(d.getId());
        x.setSortKey(d.getId());
        x.setSubject(nfc(d.getSubject()));
        x.setOrganName(nfc(d.getOrganName()));
        x.setToOrgansSummary(nfc(d.getToOrgansSummary()));
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
    public void onChanged(OutgoingDocumentChangedEvent event) {
        try {
            repository.findByIdAndDeletedFalse(event.id()).ifPresentOrElse(
                    d -> operations.save(toIndex(d)),
                    () -> operations.delete(String.valueOf(event.id()), OutgoingDocumentIndex.class));
        } catch (Exception ex) {
            log.warn("Không đồng bộ được văn bản đi {} sang Elasticsearch: {}", event.id(), ex.getMessage());
        }
    }
}