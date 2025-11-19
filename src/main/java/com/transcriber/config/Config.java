package com.transcriber.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Central configuration for the Medical Transcriber application.
 */
public class Config {
    
    // Base paths
    private static final Path BASE_DIR = Paths.get(System.getProperty("user.dir"));
    public static final Path TRANSCRIPTIONS_DIR = BASE_DIR.resolve("transcriptions");
    public static final Path RECORDINGS_DIR = BASE_DIR.resolve("recordings");
    public static final Path AUDIT_LOG_DIR = BASE_DIR.resolve("audit_logs");
    public static final Path TEMPLATES_DIR = BASE_DIR.resolve("src").resolve("main").resolve("resources").resolve("templates");
    
    // Google Cloud
    public static final String GCS_BUCKET = "transcribe_bucket9788";
    public static final String LANGUAGE_CODE = "en-US";
    public static final String GCS_MODEL = "medical_conversation";
    public static final int POLL_INTERVAL_SEC = 5;
    
    // Audio recording defaults
    public static final int SAMPLE_RATE = 16_000;
    public static final int CHANNELS = 1;
    public static final String AUDIO_SUBTYPE = "PCM_SIGNED";
    
    // Security / deletion
    public static final int SECURE_OVERWRITE_PASSES = 3;
    
    // Transcription cleaning - filler words to remove
    public static final List<String> FILLER_WORDS = Arrays.asList(
        "um",
        "umm",
        "uh",
        "er",
        "ah",
        "eh",
        "a",  // Standalone "a" (will be handled carefully to avoid removing valid uses)
        "like",
        "you know",
        "well",
        "so",
        "actually",
        "basically"
    );
    
    private Config() {
        // Utility class - prevent instantiation
    }
}


