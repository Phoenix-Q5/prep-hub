package com.prephub.embedding;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * HTTP client that calls the Python GPU embedding service.
 */
@Slf4j
@Component
public class EmbeddingClient {

    private final RestClient rest;

    public EmbeddingClient(@Value("${app.embedding.url:http://localhost:8000}") String baseUrl) {
        this.rest = RestClient.builder().baseUrl(baseUrl).build();
    }

    public List<float[]> embed(List<String> texts) {
        EmbedRequest req = new EmbedRequest(texts, true);
        EmbedResponse resp = rest.post()
                .uri("/embed")
                .body(req)
                .retrieve()
                .body(EmbedResponse.class);
        if (resp == null || resp.embeddings() == null) {
            throw new RuntimeException("Empty response from embedding service");
        }
        log.debug("Embedded {} texts in {}ms (dim={})", texts.size(), resp.elapsedMs(), resp.dimension());
        return resp.embeddings();
    }

    public float[] embedSingle(String text) {
        return embed(List.of(text)).get(0);
    }

    public boolean isHealthy() {
        try {
            rest.get().uri("/health").retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // DTOs
    record EmbedRequest(List<String> texts, boolean normalize) {}
    record EmbedResponse(
            List<float[]> embeddings,
            int dimension,
            String model,
            String device,
            @JsonProperty("elapsed_ms") double elapsedMs
    ) {}
}
