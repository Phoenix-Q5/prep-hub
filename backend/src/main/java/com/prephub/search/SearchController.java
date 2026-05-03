package com.prephub.search;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService search;

    @GetMapping("/typeahead")
    @Timed(value = "prephub.search.typeahead", description = "Type-ahead search latency")
    public List<SearchDtos.SearchHit> typeAhead(
            @RequestParam("q") String q,
            @RequestParam(required = false) String topicId,
            @RequestParam(defaultValue = "10") int limit) {
        return search.typeAhead(q, topicId, Math.min(limit, 50));
    }
}
