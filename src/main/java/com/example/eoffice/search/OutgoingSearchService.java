package com.example.eoffice.search;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import com.example.eoffice.dto.OutgoingSearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.List;

/** Tìm văn bản đi bằng Elasticsearch. Chỉ trả về ID (đã xếp thứ tự) và tổng số kết quả. */
@Service
@RequiredArgsConstructor
public class OutgoingSearchService {

    private final ElasticsearchOperations operations;

    public record IdPage(List<Long> ids, long total) {
    }

    public IdPage search(OutgoingSearchRequest r) {
        String kw = (r.getKeyword() == null || r.getKeyword().isBlank()) ? null
                : Normalizer.normalize(r.getKeyword().trim(), Normalizer.Form.NFC);
        int page = Math.max(r.getPage(), 0);
        int size = Math.min(Math.max(r.getSize(), 1), 100);

        NativeQueryBuilder builder = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> {
                    if (kw != null) {
                        b.must(m -> m.multiMatch(mm -> mm
                                .query(kw)
                                .operator(Operator.And)
                                .fields("subject^3", "subject.prefix",
                                        "toOrgansSummary^2", "toOrgansSummary.prefix",
                                        "organName", "codeNumber^2", "codeNotation^2",
                                        "signerFullName")));
                    }
                    if (r.getBookId() != null) {
                        b.filter(f -> f.term(t -> t.field("documentBookId")
                                .value(v -> v.longValue(r.getBookId()))));
                    }
                    if (r.getTypeId() != null) {
                        b.filter(f -> f.term(t -> t.field("documentTypeId")
                                .value(v -> v.longValue(r.getTypeId()))));
                    }
                    if (r.getStatus() != null && !r.getStatus().isBlank()) {
                        b.filter(f -> f.term(t -> t.field("status")
                                .value(v -> v.stringValue(r.getStatus().trim().toUpperCase()))));
                    }
                    if (r.getPriority() != null) {
                        b.filter(f -> f.term(t -> t.field("priority")
                                .value(v -> v.longValue(r.getPriority()))));
                    }
                    if (r.getSigner() != null && !r.getSigner().isBlank()) {
                        b.filter(f -> f.match(m -> m.field("signerFullName")
                                .query(r.getSigner()).operator(Operator.And)));
                    }
                    if (r.getRecipient() != null && !r.getRecipient().isBlank()) {
                        b.filter(f -> f.match(m -> m.field("toOrgansSummary")
                                .query(r.getRecipient()).operator(Operator.And)));
                    }
                    if (Boolean.TRUE.equals(r.getHasAttachment())) {
                        b.filter(f -> f.term(t -> t.field("hasAttachment")
                                .value(v -> v.booleanValue(true))));
                    }
                    addDateRange(b, "issuedDate", r.getFromDate(), r.getToDate());
                    addDateRange(b, "dueDate", r.getDueFrom(), r.getDueTo());
                    if (Boolean.TRUE.equals(r.getOverdue())) {      // quá hạn trả lời và chưa lưu trữ
                        b.filter(f -> f.range(rg -> rg.date(d -> d.field("dueDate")
                                .lt(LocalDate.now().toString()))));
                        b.mustNot(m -> m.term(t -> t.field("status")
                                .value(v -> v.stringValue("ARCHIVED"))));
                    }
                    return b;
                }))
                .withPageable(PageRequest.of(page, size))
                .withTrackTotalHits(true);

        if (kw == null) {
            builder.withSort(Sort.by(Sort.Order.desc("issuedDate"), Sort.Order.desc("sortKey")));
        }

        SearchHits<OutgoingDocumentIndex> hits = operations.search(builder.build(), OutgoingDocumentIndex.class);
        List<Long> ids = hits.getSearchHits().stream().map(h -> h.getContent().getId()).toList();
        return new IdPage(ids, hits.getTotalHits());
    }

    private void addDateRange(BoolQuery.Builder b, String field, LocalDate from, LocalDate to) {
        if (from == null && to == null) {
            return;
        }
        b.filter(f -> f.range(rg -> rg.date(d -> {
            d.field(field);
            if (from != null) {
                d.gte(from.toString());
            }
            if (to != null) {
                d.lte(to.toString());
            }
            return d;
        })));
    }
}