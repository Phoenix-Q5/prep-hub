package com.prephub.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

import com.prephub.search.QuestionDocument;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticsearchIndexConfig {

    private final ElasticsearchTemplate template;

    @PostConstruct
    public void initIndex() {
        try {
            IndexOperations ops = template.indexOps(QuestionDocument.class);
            if (!ops.exists()) {
                ops.create();
                ops.putMapping();
                log.info("Created Elasticsearch index for QuestionDocument");
            }
        } catch (Exception e) {
            log.warn("Could not initialize ES index: {}", e.getMessage());
        }
    }
}
