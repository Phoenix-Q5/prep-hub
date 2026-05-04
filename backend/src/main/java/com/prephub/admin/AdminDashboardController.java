package com.prephub.admin;

import com.prephub.common.QuestionStatus;
import com.prephub.common.SuggestionStatus;
import com.prephub.question.QuestionRepository;
import com.prephub.security.CurrentUser;
import com.prephub.suggestion.SuggestionRepository;
import com.prephub.topic.Topic;
import com.prephub.topic.TopicRepository;
import com.prephub.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Unified admin controller:
 *   - Dashboard stats
 *   - Bulk upload with validation + duplicate detection
 *   - Batch topic creation
 *   - Template downloads
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final BulkUploadService uploadService;
    private final UserRepository users;
    private final QuestionRepository questions;
    private final SuggestionRepository suggestions;
    private final TopicRepository topicRepo;
    private final CurrentUser currentUser;

    // ── Dashboard stats ─────────────────────────────────────

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        long totalUsers = users.count();
        long totalQuestions = questions.count();
        long publishedQuestions = questions.findByStatus(
                QuestionStatus.PUBLISHED,
                org.springframework.data.domain.PageRequest.of(0, 1)
        ).getTotalElements();
        long pendingSuggestions = suggestions.findByStatus(
                SuggestionStatus.PENDING,
                org.springframework.data.domain.PageRequest.of(0, 1)
        ).getTotalElements();

        return Map.of(
                "totalUsers", totalUsers,
                "totalQuestions", totalQuestions,
                "publishedQuestions", publishedQuestions,
                "pendingSuggestions", pendingSuggestions
        );
    }

    // ── Pre-upload validation ──────────────────────────────

    /**
     * Validate a JSON upload without persisting anything.
     * Returns unknown topics, duplicates, and errors.
     */
    @PostMapping("/upload/validate")
    public ResponseEntity<BulkUploadDtos.ValidationResult> validateUpload(
            @RequestBody BulkUploadDtos.JsonUploadRequest body) {
        return ResponseEntity.ok(uploadService.validate(body.questions()));
    }

    /**
     * Validate a file upload (JSON/CSV/Excel) without persisting.
     */
    @PostMapping(value = "/upload/validate-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkUploadDtos.ValidationResult> validateFile(
            @RequestParam("file") MultipartFile file) throws Exception {
        String name = file.getOriginalFilename();
        String ext = name != null ? name.substring(name.lastIndexOf('.') + 1).toLowerCase() : "";
        List<BulkUploadDtos.QuestionRow> rows = switch (ext) {
            case "json" -> {
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                yield mapper.readValue(file.getInputStream(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<BulkUploadDtos.QuestionRow>>() {});
            }
            case "csv" -> throw new IllegalArgumentException("CSV validation: upload as JSON for now");
            case "xlsx" -> throw new IllegalArgumentException("Excel validation: upload as JSON for now");
            default -> throw new IllegalArgumentException("Unsupported file type: " + ext);
        };
        return ResponseEntity.ok(uploadService.validate(rows));
    }

    // ── Batch topic creation ────────────────────────────────

    /**
     * Create multiple topics at once (used when validation finds unknown topics).
     */
    @PostMapping("/topics/batch")
    @CacheEvict(value = "topicTree", allEntries = true)
    public ResponseEntity<BulkUploadDtos.BatchCreateTopicsResult> batchCreateTopics(
            @Valid @RequestBody BulkUploadDtos.BatchCreateTopicsRequest request) {
        List<String> created = new ArrayList<>();
        for (var req : request.topics()) {
            String slug = req.slug().trim().toLowerCase();
            if (topicRepo.existsBySlugIgnoreCase(slug)) {
                continue; // already exists
            }
            Topic topic = Topic.builder()
                    .name(req.name())
                    .slug(slug)
                    .description(req.description())
                    .colorHex(req.colorHex() != null ? req.colorHex() : "#6B7280")
                    .featured(req.featured())
                    .build();
            topicRepo.save(topic);
            created.add(slug);
        }
        return ResponseEntity.ok(new BulkUploadDtos.BatchCreateTopicsResult(created.size(), created));
    }

    // ── Bulk upload ─────────────────────────────────────────

    /**
     * Upload via JSON body:
     * POST /api/admin/upload/json
     * Body: { "questions": [ { "title": "...", ... }, ... ] }
     */
    @PostMapping("/upload/json")
    public ResponseEntity<BulkUploadDtos.BulkUploadResult> uploadJsonBody(
            @RequestBody BulkUploadDtos.JsonUploadRequest body) {
        return ResponseEntity.ok(uploadService.uploadJson(body.questions(), currentUser.getRequired().id()));
    }

    /**
     * Upload a .json file:
     * POST /api/admin/upload/json-file  (multipart)
     */
    @PostMapping(value = "/upload/json-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkUploadDtos.BulkUploadResult> uploadJsonFile(
            @RequestParam("file") MultipartFile file) throws Exception {
        validateFile(file, "json");
        return ResponseEntity.ok(uploadService.uploadJsonFile(file, currentUser.getRequired().id()));
    }

    /**
     * Upload a .csv file:
     * POST /api/admin/upload/csv  (multipart)
     */
    @PostMapping(value = "/upload/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkUploadDtos.BulkUploadResult> uploadCsv(
            @RequestParam("file") MultipartFile file) throws Exception {
        validateFile(file, "csv");
        return ResponseEntity.ok(uploadService.uploadCsv(file, currentUser.getRequired().id()));
    }

    /**
     * Upload a .xlsx file:
     * POST /api/admin/upload/excel  (multipart)
     */
    @PostMapping(value = "/upload/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BulkUploadDtos.BulkUploadResult> uploadExcel(
            @RequestParam("file") MultipartFile file) throws Exception {
        validateFile(file, "xlsx");
        return ResponseEntity.ok(uploadService.uploadExcel(file, currentUser.getRequired().id()));
    }

    // ── Template downloads ──────────────────────────────────

    @GetMapping("/templates/excel")
    public ResponseEntity<byte[]> downloadExcelTemplate() throws Exception {
        byte[] data = uploadService.generateExcelTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=prephub_upload_template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/templates/csv")
    public ResponseEntity<String> downloadCsvTemplate() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=prephub_upload_template.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(uploadService.generateCsvTemplate());
    }

    @GetMapping("/templates/json")
    public ResponseEntity<String> downloadJsonTemplate() throws Exception {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=prephub_upload_template.json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(uploadService.generateJsonTemplate());
    }

    // ── User management ─────────────────────────────────────

    @GetMapping("/users")
    public ResponseEntity<?> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(users.findAll(
                org.springframework.data.domain.PageRequest.of(page, size,
                        org.springframework.data.domain.Sort.by("createdAt").descending())
        ));
    }

    @PatchMapping("/users/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleUser(@PathVariable java.util.UUID id) {
        var user = users.findById(id)
                .orElseThrow(() -> new com.prephub.common.NotFoundException("User not found"));
        user.setEnabled(!user.isEnabled());
        users.save(user);
        return ResponseEntity.ok(Map.of(
                "userId", user.getId(),
                "username", user.getUsername(),
                "enabled", user.isEnabled()
        ));
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<Map<String, Object>> changeRole(
            @PathVariable java.util.UUID id,
            @RequestBody Map<String, String> body) {
        var user = users.findById(id)
                .orElseThrow(() -> new com.prephub.common.NotFoundException("User not found"));
        String newRole = body.get("role");
        user.setRole(com.prephub.common.Role.valueOf(newRole.toUpperCase()));
        users.save(user);
        return ResponseEntity.ok(Map.of(
                "userId", user.getId(),
                "username", user.getUsername(),
                "role", user.getRole()
        ));
    }

    // ── Helpers ─────────────────────────────────────────────

    private void validateFile(MultipartFile file, String expectedExt) {
        if (file.isEmpty()) throw new IllegalArgumentException("File is empty");
        String name = file.getOriginalFilename();
        if (name != null && !name.toLowerCase().endsWith("." + expectedExt)) {
            throw new IllegalArgumentException("Expected ." + expectedExt + " file, got: " + name);
        }
    }
}
