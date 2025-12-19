package com.transcriber.audit;

import com.transcriber.config.Config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
//import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * HIPAA-aligned audit logging utilities.
 */
public class AuditLogger {
    
    private static final Path LOG_FILE = Config.AUDIT_LOG_DIR.resolve("audit_log.csv");
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    
    static {
        // Ensure audit log directory exists
        try {
            Files.createDirectories(Config.AUDIT_LOG_DIR);
            ensureHeader();
        } catch (IOException e) {
            System.err.println("Failed to initialize audit log directory: " + e.getMessage());
        }
    }
    
    /**
     * Ensures the CSV file has a header row.
     */
    private static void ensureHeader() throws IOException {
        if (Files.exists(LOG_FILE)) {
            return;
        }
        String header = "timestamp,action,file,patient,details\n";
        Files.writeString(LOG_FILE, header, StandardOpenOption.CREATE);
    }
    
    /**
     * Append an audit row with UTC timestamp and metadata.
     * 
     * @param action The action being logged (e.g., "record_start", "save_transcription", "secure_delete")
     * @param filePath The file path involved in the action
     * @param patient Patient identifier (optional, can be empty string)
     * @param details Additional details about the action
     */
    public static void log(String action, Path filePath, String patient, String details) {
        try {
            ensureHeader();
            String timestamp = ISO_FORMATTER.format(Instant.now());
            String normalized = filePath != null ? filePath.toString() : "";
            String csvRow = String.format("%s,%s,%s,%s,%s%n", 
                escapeCsv(timestamp),
                escapeCsv(action),
                escapeCsv(normalized),
                escapeCsv(patient != null ? patient : ""),
                escapeCsv(details != null ? details : ""));
            
            Files.writeString(LOG_FILE, csvRow, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (IOException e) {
            System.err.println("Failed to write audit log: " + e.getMessage());
        }
    }
    
    /**
     * Overload for logging with String file path.
     */
    public static void log(String action, String filePath, String patient, String details) {
        log(action, filePath != null ? Path.of(filePath) : null, patient, details);
    }
    
    /**
     * Escape CSV special characters.
     */
    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        // If value contains comma, quote, or newline, wrap in quotes and escape quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}


