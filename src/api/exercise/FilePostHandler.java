package exercise;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FilePostHandler implements HttpHandler {

    private final List<DateTimeFormatter> dateFormats =
            Stream.of(
                            "MM-dd-yyyy",   // 01-02-3000
                            "MMM dd, yyyy",        // Jan 02, 3000
                            "MMMM dd, yyyy",      // January 02, 3000

                            "dd/MM/yyyy",   //02/01/3000
                            "d/M/yyyy", //2/1/3000
                            "dd-MM-yyyy", //02-01-3000
                            "dd.MM.yyyy", //02.01.3000
                            "ddMMyyyy", //02013000
                            "dd MMM yyyy",  //02 Jan 3000
                            "dd MMMM yyyy",  //02 January 3000

                            "yyyy-MM-dd",  //3000-01-02
                            "yyyy/MM/dd",  //3000/01/02
                            "yyyy.MM.dd",  //3000.01.02
                            "yyyyMMdd"  //30000102
                    )
                    .map(DateTimeFormatter::ofPattern).toList();

    private LocalDate parseDate(String date) throws Exception {
        return dateFormats.stream()
                .map(dateTimeFormatter -> {
                    try {
                        return LocalDate.parse(date, dateTimeFormatter);
                    } catch (DateTimeParseException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new Exception("Unable to parse date"));
    }


    private void addStandardHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
    }

    private boolean isPostRequest(HttpExchange exchange) {
        return "POST".equalsIgnoreCase(exchange.getRequestMethod());
    }

    private boolean isMultipartRequest(HttpExchange exchange) {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        return contentType != null && contentType.contains("multipart/form-data");
    }

    private byte[] readRequestBody(HttpExchange exchange) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        exchange.getRequestBody().transferTo(buffer);
        return buffer.toByteArray();
    }

    private String extractFilename(byte[] data) {
        String body = new String(data, StandardCharsets.UTF_8);
        int filenameIndex = body.indexOf("filename=\"");
        if (filenameIndex != -1) {
            int start = filenameIndex + 10;
            int end = body.indexOf("\"", start);
            if (end > start) {
                return body.substring(start, end);
            }
        }
        return "";
    }

    private boolean isValidCsvFormat(Path file) {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",", -1);
                if (parts.length != 4) {
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Path saveTempFile(byte[] data) throws IOException {
        Path uploads = Path.of("uploads");
        Files.createDirectories(uploads);
        String fileName = "employees-" + System.currentTimeMillis() + ".csv";
        Path tempFile = uploads.resolve(fileName);
        Files.copy(new ByteArrayInputStream(data), tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return tempFile;
    }

    private List<EmployeeRecord> parseCsvToRecords(Path file) throws IOException {
        List<EmployeeRecord> records = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split(",", -1);

                if (parts.length != 4) continue;

                try {
                    int empId = Integer.parseInt(parts[0].trim());
                    int projectId = Integer.parseInt(parts[1].trim());

                    LocalDate dateFrom = parseDate(parts[2].trim());

                    LocalDate dateTo = parts[3].trim().equalsIgnoreCase("NULL")
                            ? LocalDate.now()
                            : parseDate(parts[3].trim());

                    records.add(new EmployeeRecord(empId, projectId, dateFrom, dateTo));
                } catch (Exception e) {

                }
            }
        }
        return records;
    }

    private void deleteFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            System.err.println("Failed to delete file: " + path);
        }
    }

    private String errorJson(String message, String type) {
        return String.format("{\"error\":\"%s\",\"type\":\"%s\"}", message, type);
    }

    private void reply(HttpExchange exchange, String message, int code) throws IOException {
        try (OutputStream os = exchange.getResponseBody()) {
            byte[] responseBytes = message.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(code, responseBytes.length);
            os.write(responseBytes);
        }
    }


    public static List<PairWork> calculateCollaborations(List<EmployeeRecord> records) {
        Map<Integer, List<EmployeeRecord>> recordsByProject = new HashMap<>();
        List<PairWork> summaries = new ArrayList<>();

        for (EmployeeRecord record : records) {
            recordsByProject
                    .computeIfAbsent(record.projectId(), id -> new ArrayList<>())
                    .add(record);
        }

        for (Map.Entry<Integer, List<EmployeeRecord>> entry : recordsByProject.entrySet()) {
            int projectId = entry.getKey();
            List<EmployeeRecord> employees = entry.getValue();

            for (int i = 0; i < employees.size(); i++) {
                EmployeeRecord empA = employees.get(i);

                for (int j = i + 1; j < employees.size(); j++) {
                    EmployeeRecord empB = employees.get(j);

                    LocalDate overlapStart = max(empA.dateFrom(), empB.dateFrom());
                    LocalDate overlapEnd = min(empA.dateTo(), empB.dateTo());

                    if (!overlapStart.isAfter(overlapEnd)) {
                        long overlapDays = ChronoUnit.DAYS.between(overlapStart, overlapEnd) + 1;
                        summaries.add(new PairWork(empA.empId(), empB.empId(), projectId, overlapDays));
                    }
                }
            }
        }

        return summaries;
    }

    public static Optional<PairWork> findLongestPair(List<PairWork> summaries) {
        return summaries.stream()
                .max(Comparator.comparingLong(PairWork::days));
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return (a.isAfter(b)) ? a : b;
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return (a.isBefore(b)) ? a : b;
    }

    private List<PairWork> getAllProjectsForTopPair(List<PairWork> allPairs, int emp1, int emp2) {
        return allPairs.stream()
                .filter(p -> isSamePair(p, emp1, emp2))
                .toList();
    }

    private boolean isSamePair(PairWork p, int a, int b) {
        return (p.emp1() == a && p.emp2() == b) || (p.emp1() == b && p.emp2() == a);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        addStandardHeaders(exchange);

        if (!isPostRequest(exchange)) {
            reply(exchange, errorJson("This is a POST only method", "user"), 405);
            return;
        }

        if (!isMultipartRequest(exchange)) {
            reply(exchange, errorJson("Expected multipart/form-data", "user"), 400);
            return;
        }

        byte[] requestData = readRequestBody(exchange);
        String filename = extractFilename(requestData);

        if (!filename.toLowerCase().endsWith(".csv")) {
            reply(exchange, errorJson("The input file is not .csv", "user"), 400);
            return;
        }

        Path tempFile = saveTempFile(requestData);

        try {

            if (!isValidCsvFormat(tempFile)) {
                reply(exchange, errorJson("The input file does not have 4 columns", "user"), 400);
                return;
            }

            List<EmployeeRecord> records = parseCsvToRecords(tempFile);
            List<PairWork> collaborations = calculateCollaborations(records);

            Optional<PairWork> topPair = findLongestPair(collaborations);

            if (topPair.isPresent()) {
                PairWork result = topPair.get();
                List<PairWork> sharedProjects = getAllProjectsForTopPair(collaborations, result.emp1(), result.emp2());

                String jsonArray = sharedProjects.stream()
                        .map(pair -> String.format("""
                                {
                                  "emp1": %d,
                                  "emp2": %d,
                                  "projectId": %d,
                                  "days": %d
                                }""", pair.emp1(), pair.emp2(), pair.projectId(), pair.days()))
                        .collect(Collectors.joining(",\n", "[\n", "\n]"));

                reply(exchange, jsonArray, 200);
            } else {
                reply(exchange, errorJson("No overlapping was found", "data"), 404);
            }
        } finally {
            deleteFile(tempFile);
        }
    }
}