package com.prephub.admin;

import com.prephub.common.Difficulty;
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
            String authorUsername    // optional — defaults to uploader (admin)
    ) {}

    public record BulkUploadResult(
            int totalRows,
            int successCount,
            int errorCount,
            List<RowError> errors
    ) {}

    public record RowError(
            int row,
            String title,
            String error
    ) {}

    /**
     * For JSON array upload — the body is just a list of QuestionRow.
     */
    public record JsonUploadRequest(List<QuestionRow> questions) {}
}
