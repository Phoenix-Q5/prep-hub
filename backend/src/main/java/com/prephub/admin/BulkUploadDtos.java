package com.prephub.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

public class BulkUploadDtos {

    /**
     * Single row in a bulk upload — works for JSON array items, CSV rows, or Excel rows.
     */
    public record QuestionRow(
            @NotBlank @Size(max = 300) String title,
            @NotBlank String content,
            @NotBlank String topicSlug,
            String difficulty,      // EASY, MEDIUM, HARD — defaults to MEDIUM
            String tags,            // comma-separated, e.g. "java, collections, hashmap"
            String authorUsername,   // optional — defaults to uploader (admin)
            String answer           // optional — if provided, creates an official answer
    ) {}

    public record BulkUploadResult(
            int totalRows,
            int successCount,
            int skippedDuplicates,
            int errorCount,
            List<RowError> errors
    ) {}

    public record RowError(
            int row,
            String title,
            String error
    ) {}

    public record JsonUploadRequest(List<QuestionRow> questions) {}

    // ── Validation (pre-upload check) ──────────────────────

    public record ValidationResult(
            int totalRows,
            int validRows,
            int duplicateRows,
            Set<String> unknownTopics,
            Set<String> availableTopics,
            List<String> duplicateTitles,
            List<RowError> errors
    ) {}

    // ── Batch topic creation ───────────────────────────────

    public record CreateTopicRequest(
            @NotBlank String name,
            @NotBlank String slug,
            String description,
            String colorHex,
            boolean featured
    ) {}

    public record BatchCreateTopicsRequest(List<CreateTopicRequest> topics) {}

    public record BatchCreateTopicsResult(
            int created,
            List<String> slugs
    ) {}
}
