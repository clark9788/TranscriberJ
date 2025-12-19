package com.transcriber.file;

import com.transcriber.audit.AuditLogger;
import com.transcriber.config.Config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Transcription file management and secure deletion helpers.
 */
public class FileManager {
    
    private static final Pattern SANITIZE_PATTERN = Pattern.compile("[^A-Za-z0-9_-]+");
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            .withZone(ZoneId.systemDefault());
    private static final SecureRandom secureRandom = new SecureRandom();
    
    static {
        // Ensure directories exist
        try {
            Files.createDirectories(Config.TRANSCRIPTIONS_DIR);
            Files.createDirectories(Config.RECORDINGS_DIR);
        } catch (IOException e) {
            System.err.println("Failed to create directories: " + e.getMessage());
        }
    }
    
    /**
     * Sanitize a component (patient name or DOB) for use in filenames.
     */
    public static String sanitizeComponent(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "unknown";
        }
        String cleaned = SANITIZE_PATTERN.matcher(value.trim()).replaceAll("_");
        return cleaned.isEmpty() ? "unknown" : cleaned;
    }
    
    /**
     * Generate a filename for a transcription based on patient name and DOB.
     */
    public static Path generateFilename(String patient, String dob) {
        String timestamp = TIMESTAMP_FORMATTER.format(Instant.now());
        String safePatient = sanitizeComponent(patient);
        String safeDob = sanitizeComponent(dob);
        return Config.TRANSCRIPTIONS_DIR.resolve(String.format("%s_%s_%s.txt", safePatient, safeDob, timestamp));
    }
    
    /**
     * List all transcription files, sorted by modification time (newest first).
     */
    public static List<Path> listTranscriptions() {
        try {
            List<Path> files = new ArrayList<>();
            if (Files.exists(Config.TRANSCRIPTIONS_DIR)) {
                try (var stream = Files.list(Config.TRANSCRIPTIONS_DIR)) {
                    stream.filter(Files::isRegularFile)
                          .filter(p -> p.toString().endsWith(".txt"))
                          .forEach(files::add);
                }
            }
            files.sort(Comparator.comparing((Path p) -> {
                try {
                    return Files.getLastModifiedTime(p).toInstant();
                } catch (IOException e) {
                    return Instant.EPOCH;
                }
            }).reversed());
            return files;
        } catch (IOException e) {
            System.err.println("Failed to list transcriptions: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Save transcription content to a file.
     */
    public static void saveTranscription(Path path, String content) throws IOException {
        Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        AuditLogger.log("save_transcription", path, "", "Saved transcription");
    }
    
    /**
     * Load transcription content from a file.
     */
    public static String loadTranscription(Path path) throws IOException {
        return Files.readString(path);
    }
    
    /**
     * Securely delete a file by overwriting it multiple times with random data,
     * then deleting it. HIPAA-compliant disposal.
     * 
     * @param filePath The file to securely delete
     * @param patient Patient identifier for audit logging
     */
    public static void secureDelete(Path filePath, String patient) {
        if (filePath == null || !Files.exists(filePath)) {
            return;
        }
        
        try {
            long size = Files.size(filePath);
            if (size > 0) {
                byte[] randomData = new byte[(int) Math.min(size, 8192)]; // 8KB buffer
                
                for (int pass = 0; pass < Config.SECURE_OVERWRITE_PASSES; pass++) {
                    try (java.nio.channels.FileChannel channel = 
                            java.nio.channels.FileChannel.open(filePath, StandardOpenOption.WRITE)) {
                        long written = 0;
                        while (written < size) {
                            secureRandom.nextBytes(randomData);
                            int toWrite = (int) Math.min(randomData.length, size - written);
                            channel.write(java.nio.ByteBuffer.wrap(randomData, 0, toWrite));
                            written += toWrite;
                        }
                        channel.force(true); // Force write to disk
                    }
                }
            }
            
            Files.delete(filePath);
            AuditLogger.log("secure_delete", filePath, patient != null ? patient : "",
                    String.format("Overwritten %d passes and deleted", Config.SECURE_OVERWRITE_PASSES));
        } catch (IOException e) {
            System.err.println("Failed to securely delete file: " + e.getMessage());
            AuditLogger.log("secure_delete_failed", filePath, patient != null ? patient : "",
                    "Error: " + e.getMessage());
        }
    }
}


