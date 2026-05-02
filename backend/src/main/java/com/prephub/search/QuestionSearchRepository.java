package com.prephub.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionSearchRepository extends ElasticsearchRepository<QuestionDocument, String> {
}
