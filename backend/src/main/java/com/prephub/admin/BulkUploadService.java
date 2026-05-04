package com.prephub.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prephub.answer.Answer;
import com.prephub.answer.AnswerRepository;
import com.prephub.common.Difficulty;
import com.prephub.common.QuestionStatus;
import com.prephub.embedding.EmbeddingService;
import com.prephub.question.Question;
import com.prephub.question.QuestionRepository;
import com.prephub.search.SearchIndexer;
import com.prephub.topic.Topic;
import com.prephub.topic.TopicRepository;
import com.prephub.user.PortfolioRepository;
import com.prephub.user.User;
import com.prephub.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkUploadService {

    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final TopicRepository topics;
    private final UserRepository users;
    private final PortfolioRepository portfolios;
    private final SearchIndexer indexer;
    private final EmbeddingService embeddingService;
    private final ObjectMapper mapper;

    // ── JSON upload ─────────────────────────────────────────

    @Transactional
    public BulkUploadDtos.BulkUploadResult uploadJson(List<BulkUploadDtos.QuestionRow> rows, UUID uploaderId) {
        return processRows(rows, uploaderId);
    }

    @Transactional
    public BulkUploadDtos.BulkUploadResult uploadJsonFile(MultipartFile file, UUID uploaderId) throws IOException {
        List<BulkUploadDtos.QuestionRow> rows = mapper.readValue(
                file.getInputStream(),
                new TypeReference<>() {}
        );
        return processRows(rows, uploaderId);
    }

    // ── CSV upload ──────────────────────────────────────────

    @Transactional
    public BulkUploadDtos.BulkUploadResult uploadCsv(MultipartFile file, UUID uploaderId) throws IOException {
        List<BulkUploadDtos.QuestionRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return new BulkUploadDtos.BulkUploadResult(0, 0, 0, 0, List.of());
            }

            // Parse header to find column indices (flexible ordering)
            String[] headers = parseCsvLine(headerLine);
            Map<String, Integer> colMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                colMap.put(headers[i].trim().toLowerCase().replaceAll("[^a-z_]", ""), i);
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] cols = parseCsvLine(line);
                rows.add(new BulkUploadDtos.QuestionRow(
                        getCol(cols, colMap, "title", ""),
                        getCol(cols, colMap, "content", ""),
                        getCol(cols, colMap, "topic_slug", getCol(cols, colMap, "topicslug", "")),
                        getCol(cols, colMap, "difficulty", "MEDIUM"),
                        getCol(cols, colMap, "tags", ""),
                        getCol(cols, colMap, "author_username", getCol(cols, colMap, "authorusername", "")),
                        getCol(cols, colMap, "answer", "")
                ));
            }
        }
        return processRows(rows, uploaderId);
    }

    // ── Excel upload ────────────────────────────────────────

    @Transactional
    public BulkUploadDtos.BulkUploadResult uploadExcel(MultipartFile file, UUID uploaderId) throws IOException {
        List<BulkUploadDtos.QuestionRow> rows = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) {
                return new BulkUploadDtos.BulkUploadResult(0, 0, 0,0, List.of());
            }

            // Parse header row
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return new BulkUploadDtos.BulkUploadResult(0, 0, 0, 0, List.of());
            }

            Map<String, Integer> colMap = new HashMap<>();
            for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                Cell cell = headerRow.getCell(i);
                if (cell != null) {
                    colMap.put(getCellString(cell).trim().toLowerCase().replaceAll("[^a-z_]", ""), i);
                }
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                rows.add(new BulkUploadDtos.QuestionRow(
                        getExcelCol(row, colMap, "title", ""),
                        getExcelCol(row, colMap, "content", ""),
                        getExcelCol(row, colMap, "topic_slug", getExcelCol(row, colMap, "topicslug", "")),
                        getExcelCol(row, colMap, "difficulty", "MEDIUM"),
                        getExcelCol(row, colMap, "tags", ""),
                        getExcelCol(row, colMap, "author_username", getExcelCol(row, colMap, "authorusername", "")),
                        getExcelCol(row, colMap, "answer", "")
                ));
            }
        }
        return processRows(rows, uploaderId);
    }

    // ── Template generation ─────────────────────────────────

    public byte[] generateExcelTemplate() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Questions");

            // Header style
            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Headers
            String[] headers = {"title", "content", "topic_slug", "difficulty", "tags", "author_username"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Example rows
            Object[][] examples = {
                    {"What is the difference between HashMap and HashTable?",
                     "Explain the key differences in terms of synchronization, null handling, and performance.",
                     "java", "MEDIUM", "collections, concurrency, hashmap", ""},
                    {"Explain Spring Boot auto-configuration",
                     "How does @SpringBootApplication trigger auto-configuration? Walk through the mechanism.",
                     "spring-boot", "HARD", "spring, autoconfiguration, annotations", ""},
                    {"What is a Binary Search Tree?",
                     "Describe BST properties, insertion, deletion, and time complexity analysis.",
                     "dsa", "EASY", "trees, binary-search, data-structures", ""},
            };

            for (int r = 0; r < examples.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < examples[r].length; c++) {
                    row.createCell(c).setCellValue((String) examples[r][c]);
                }
            }

            // Instructions sheet
            Sheet instr = wb.createSheet("Instructions");
            String[][] instructions = {
                    {"Column", "Required", "Description", "Values"},
                    {"title", "YES", "Question title (max 300 chars)", "Free text"},
                    {"content", "YES", "Full question body", "Free text, can be multi-line"},
                    {"topic_slug", "YES", "Topic identifier", "java, spring-boot, csharp-dotnet, aws, terraform, system-design, dsa, sql, microservices, kafka"},
                    {"difficulty", "NO", "Difficulty level", "EASY, MEDIUM (default), HARD"},
                    {"tags", "NO", "Comma-separated tags", "e.g. collections, concurrency"},
                    {"author_username", "NO", "Assign to user", "Leave blank = uses admin account"},
            };
            for (int r = 0; r < instructions.length; r++) {
                Row row = instr.createRow(r);
                for (int c = 0; c < instructions[r].length; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(instructions[r][c]);
                    if (r == 0) cell.setCellStyle(headerStyle);
                }
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            for (int i = 0; i < 4; i++) instr.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    public String generateCsvTemplate() {
        return """
                title,content,topic_slug,difficulty,tags,author_username
                "What is the difference between HashMap and HashTable?","Explain the key differences in terms of synchronization, null handling, and performance.",java,MEDIUM,"collections, concurrency, hashmap",
                "Explain Spring Boot auto-configuration","How does @SpringBootApplication trigger auto-configuration?",spring-boot,HARD,"spring, autoconfiguration",
                "What is a Binary Search Tree?","Describe BST properties, insertion, deletion, and time complexity.",dsa,EASY,"trees, binary-search",
                """;
    }

    public String generateJsonTemplate() throws IOException {
        List<Map<String, String>> template = List.of(
                Map.of(
                        "title", "What is the difference between HashMap and HashTable?",
                        "content", "Explain the key differences in terms of synchronization, null handling, and performance.",
                        "topicSlug", "java",
                        "difficulty", "MEDIUM",
                        "tags", "collections, concurrency, hashmap",
                        "authorUsername", ""
                ),
                Map.of(
                        "title", "Explain Spring Boot auto-configuration",
                        "content", "How does @SpringBootApplication trigger auto-configuration?",
                        "topicSlug", "spring-boot",
                        "difficulty", "HARD",
                        "tags", "spring, autoconfiguration",
                        "authorUsername", ""
                )
        );
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(template);
    }

    // ── Validation (pre-upload dry run) ───────────────────

    public BulkUploadDtos.ValidationResult validate(List<BulkUploadDtos.QuestionRow> rows) {
        Map<String, Topic> topicMap = topics.findAll().stream()
                .collect(Collectors.toMap(t -> t.getSlug().toLowerCase(), t -> t, (a, b) -> a));

        Set<String> unknownTopics = new LinkedHashSet<>();
        List<String> duplicateTitles = new ArrayList<>();
        List<BulkUploadDtos.RowError> errors = new ArrayList<>();
        int validRows = 0;

        // Batch check for existing titles
        List<String> allTitles = rows.stream()
                .map(r -> r.title() != null ? r.title().trim().toLowerCase() : "")
                .filter(t -> !t.isEmpty())
                .toList();
        Set<String> existingTitles = new HashSet<>(questions.findExistingTitles(allTitles));

        for (int i = 0; i < rows.size(); i++) {
            BulkUploadDtos.QuestionRow row = rows.get(i);

            if (row.title() == null || row.title().isBlank()) {
                errors.add(new BulkUploadDtos.RowError(i + 1, "(empty)", "Title is required"));
                continue;
            }
            if (row.content() == null || row.content().isBlank()) {
                errors.add(new BulkUploadDtos.RowError(i + 1, row.title(), "Content is required"));
                continue;
            }

            String slug = row.topicSlug() != null ? row.topicSlug().trim().toLowerCase() : "";
            if (slug.isEmpty() || !topicMap.containsKey(slug)) {
                unknownTopics.add(slug.isEmpty() ? "(empty)" : row.topicSlug().trim());
            }

            if (existingTitles.contains(row.title().trim().toLowerCase())) {
                duplicateTitles.add(row.title().trim());
            } else {
                validRows++;
            }
        }

        return new BulkUploadDtos.ValidationResult(
                rows.size(), validRows, duplicateTitles.size(),
                unknownTopics, topicMap.keySet(),
                duplicateTitles, errors
        );
    }

    // ── Core processing ─────────────────────────────────────

    private BulkUploadDtos.BulkUploadResult processRows(List<BulkUploadDtos.QuestionRow> rows, UUID uploaderId) {
        User uploader = users.findById(uploaderId).orElseThrow();
        int success = 0;
        int skippedDuplicates = 0;
        List<BulkUploadDtos.RowError> errors = new ArrayList<>();

        // Pre-load topics for performance
        Map<String, Topic> topicMap = topics.findAll().stream()
                .collect(Collectors.toMap(t -> t.getSlug().toLowerCase(), t -> t, (a, b) -> a));

        // Batch check for existing titles
        List<String> allTitles = rows.stream()
                .map(r -> r.title() != null ? r.title().trim().toLowerCase() : "")
                .filter(t -> !t.isEmpty())
                .toList();
        Set<String> existingTitles = new HashSet<>(questions.findExistingTitles(allTitles));

        for (int i = 0; i < rows.size(); i++) {
            BulkUploadDtos.QuestionRow row = rows.get(i);
            try {
                // Validate
                if (row.title() == null || row.title().isBlank()) {
                    errors.add(new BulkUploadDtos.RowError(i + 1, row.title(), "Title is required"));
                    continue;
                }
                if (row.content() == null || row.content().isBlank()) {
                    errors.add(new BulkUploadDtos.RowError(i + 1, row.title(), "Content is required"));
                    continue;
                }

                // Skip duplicates
                String titleLower = row.title().trim().toLowerCase();
                if (existingTitles.contains(titleLower)) {
                    skippedDuplicates++;
                    continue;
                }

                // Resolve topic
                String slug = row.topicSlug() != null ? row.topicSlug().trim().toLowerCase() : "";
                Topic topic = topicMap.get(slug);
                if (topic == null) {
                    errors.add(new BulkUploadDtos.RowError(i + 1, row.title(),
                            "Unknown topic_slug: '" + row.topicSlug() + "'"));
                    continue;
                }

                // Resolve author
                User author = uploader;
                if (row.authorUsername() != null && !row.authorUsername().isBlank()) {
                    author = users.findByUsernameIgnoreCase(row.authorUsername().trim())
                            .orElse(uploader);
                }

                // Parse difficulty
                Difficulty diff = Difficulty.MEDIUM;
                if (row.difficulty() != null && !row.difficulty().isBlank()) {
                    try { diff = Difficulty.valueOf(row.difficulty().trim().toUpperCase()); }
                    catch (IllegalArgumentException ignored) {}
                }

                // Parse tags
                Set<String> tags = new HashSet<>();
                if (row.tags() != null && !row.tags().isBlank()) {
                    tags = Arrays.stream(row.tags().split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toSet());
                }

                // Create question
                Question q = Question.builder()
                        .title(row.title().trim())
                        .content(row.content().trim())
                        .topic(topic)
                        .author(author)
                        .difficulty(diff)
                        .status(QuestionStatus.PUBLISHED)
                        .tags(tags)
                        .build();
                q = questions.save(q);

                // Track this title to prevent duplicates within the same batch
                existingTitles.add(titleLower);

                // Create official answer if provided
                if (row.answer() != null && !row.answer().isBlank()) {
                    Answer ans = Answer.builder()
                            .question(q)
                            .author(author)
                            .content(row.answer().trim())
                            .official(true)
                            .build();
                    answers.save(ans);
                    q.setAnswerCount(1);
                    questions.save(q);
                }

                // Update counters
                topics.adjustQuestionCount(topic.getId(), 1);
                portfolios.incrementPosts(author.getId(), 1, 2);

                // Index asynchronously
                indexer.indexQuestion(q);
                embeddingService.embedQuestion(q);

                success++;
            } catch (Exception e) {
                errors.add(new BulkUploadDtos.RowError(i + 1, row.title(),
                        "Unexpected error: " + e.getMessage()));
            }
        }

        log.info("Bulk upload: {} rows, {} success, {} duplicates skipped, {} errors",
                rows.size(), success, skippedDuplicates, errors.size());
        return new BulkUploadDtos.BulkUploadResult(rows.size(), success, skippedDuplicates, errors.size(), errors);
    }

    // ── CSV helpers ─────────────────────────────────────────

    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString().trim());
        return result.toArray(String[]::new);
    }

    private String getCol(String[] cols, Map<String, Integer> colMap, String key, String defaultVal) {
        Integer idx = colMap.get(key);
        if (idx == null || idx >= cols.length) return defaultVal;
        String val = cols[idx].trim();
        return val.isEmpty() ? defaultVal : val;
    }

    private String getExcelCol(Row row, Map<String, Integer> colMap, String key, String defaultVal) {
        Integer idx = colMap.get(key);
        if (idx == null) return defaultVal;
        Cell cell = row.getCell(idx);
        if (cell == null) return defaultVal;
        String val = getCellString(cell).trim();
        return val.isEmpty() ? defaultVal : val;
    }

    private String getCellString(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }
}
