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

/** Bản ghi văn bản nội bộ trong Elasticsearch: chỉ giữ các trường dùng để TÌM và LỌC. */
@Document(indexName = "internal_documents")
@Setting(settingPath = "es/vi-analysis.json")
@Getter
@Setter
@NoArgsConstructor
public class InternalDocumentIndex {

    @Id
    private Long id;

    /** Bản sao của id, dùng để sắp xếp (không sắp xếp trực tiếp theo _id). */
    @Field(type = FieldType.Long)
    private Long sortKey;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "vi_fold"),
            otherFields = @InnerField(suffix = "prefix", type = FieldType.Text,
                    analyzer = "vi_prefix", searchAnalyzer = "vi_fold"))
    private String subject;

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

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Integer)
    private Integer priority;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate issuedDate;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate dueDate;

    @Field(type = FieldType.Boolean)
    private boolean hasAttachment;
}