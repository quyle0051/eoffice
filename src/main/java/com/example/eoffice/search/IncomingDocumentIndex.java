package com.example.eoffice.search;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.LocalDate;
import java.util.List;

/**
 * Bản ghi văn bản đến trong Elasticsearch: chỉ giữ các trường dùng để TÌM và LỌC.
 * Dữ liệu đầy đủ vẫn lấy từ MySQL.
 */
@Document(indexName = "incoming_documents")
@Setting(settingPath = "es/vi-analysis.json")
@Getter
@Setter
@NoArgsConstructor
public class IncomingDocumentIndex {

    @Id
    private Long id;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "vi_fold"),
            otherFields = @InnerField(suffix = "prefix", type = FieldType.Text,
                    analyzer = "vi_prefix", searchAnalyzer = "vi_fold"))
    private String subject;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "vi_fold"),
            otherFields = @InnerField(suffix = "prefix", type = FieldType.Text,
                    analyzer = "vi_prefix", searchAnalyzer = "vi_fold"))
    private String organName;

    @Field(type = FieldType.Text, analyzer = "vi_fold")
    private String signerFullName;

    @Field(type = FieldType.Text, analyzer = "vi_fold")
    private String codeNumber;

    @Field(type = FieldType.Text, analyzer = "vi_fold")
    private String codeNotation;

    @Field(type = FieldType.Long)
    private Long documentBookId;

    @Field(type = FieldType.Long)
    private Long documentTypeId;

    @Field(type = FieldType.Long)
    private Long arrivalNumber;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Integer)
    private Integer priority;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate arrivalDate;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate issuedDate;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate dueDate;

    @Field(type = FieldType.Boolean)
    private boolean hasAttachment;

    /** Email (viết thường) của người đang xử lý và những người được cấp quyền xem. */
    @Field(type = FieldType.Keyword)
    private List<String> viewerEmails;
}