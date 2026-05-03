package com.prephub.search;

import com.prephub.common.Difficulty;
import com.prephub.common.QuestionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;
import org.springframework.data.elasticsearch.annotations.Setting;

import java.time.Instant;
import java.util.Set;

@Document(indexName = "questions")
@Setting(settingPath = "elasticsearch/questions-settings.json")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionDocument {

    @Id
    private String id;

    @MultiField(
            mainField = @Field(type = FieldType.Text, analyzer = "edge_ngram_analyzer", searchAnalyzer = "standard_lowercase"),
            otherFields = {
                    @InnerField(suffix = "keyword", type = FieldType.Keyword),
                    @InnerField(suffix = "fuzzy", type = FieldType.Text, analyzer = "standard_lowercase")
            })
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard_lowercase")
    private String content;

    @Field(type = FieldType.Keyword)
    private String topicId;

    @Field(type = FieldType.Text, analyzer = "edge_ngram_analyzer", searchAnalyzer = "standard_lowercase")
    private String topicName;

    @Field(type = FieldType.Keyword)
    private String authorUsername;

    @Field(type = FieldType.Keyword)
    private Difficulty difficulty;

    @Field(type = FieldType.Keyword)
    private QuestionStatus status;

    @Field(type = FieldType.Text, analyzer = "edge_ngram_analyzer", searchAnalyzer = "standard_lowercase")
    private Set<String> tags;

    @Field(type = FieldType.Integer)
    private int likeCount;

    @Field(type = FieldType.Long)
    private long viewCount;

    @Field(type = FieldType.Date)
    private Instant createdAt;
}
