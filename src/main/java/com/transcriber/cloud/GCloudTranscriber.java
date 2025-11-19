package com.transcriber.cloud;

import com.google.api.gax.longrunning.OperationFuture;
import com.google.cloud.speech.v1.*;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.transcriber.audit.AuditLogger;
import com.transcriber.config.Config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Google Cloud Storage + Speech-to-Text integration.
 */
public class GCloudTranscriber {
    
    private static final SpeechClient speechClient;
    private static final Storage storageClient;
    
    static {
        try {
            speechClient = SpeechClient.create();
            storageClient = StorageOptions.getDefaultInstance().getService();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize Google Cloud clients: " + e.getMessage(), e);
        }
    }
    
    /**
     * Upload an audio file, run transcription, return the transcript text.
     * 
     * @param audioPath Path to the audio file
     * @param patient Patient identifier for audit logging
     * @param statusCallback Optional callback for status updates
     * @return The transcribed text
     * @throws IOException If file operations fail
     * @throws RuntimeException If transcription fails
     */
    public static String uploadAndTranscribe(Path audioPath, String patient, Consumer<String> statusCallback) 
            throws IOException {
        if (!Files.exists(audioPath)) {
            throw new FileNotFoundException("Audio file not found: " + audioPath);
        }
        
        setStatus(statusCallback, "Uploading…");
        
        // Upload to Google Cloud Storage
        Bucket bucket = storageClient.get(Config.GCS_BUCKET);
        if (bucket == null) {
            throw new RuntimeException("Bucket not found: " + Config.GCS_BUCKET);
        }
        
        Blob blob = bucket.create(audioPath.getFileName().toString(), 
                Files.readAllBytes(audioPath), 
                "audio/wav");
        
        AuditLogger.log("gcs_upload", audioPath, patient != null ? patient : "", "Uploaded to GCS");
        
        String gcsUri = "gs://" + Config.GCS_BUCKET + "/" + audioPath.getFileName().toString();
        
        // Configure recognition
        RecognitionConfig config = RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                .setSampleRateHertz(Config.SAMPLE_RATE)
                .setLanguageCode(Config.LANGUAGE_CODE)
                .setModel(Config.GCS_MODEL)
                .setEnableAutomaticPunctuation(true)
                .build();
        
        RecognitionAudio audio = RecognitionAudio.newBuilder()
                .setUri(gcsUri)
                .build();
        
        setStatus(statusCallback, "Transcribing…");
        
        // Start long-running recognition
        OperationFuture<LongRunningRecognizeResponse, LongRunningRecognizeMetadata> operation =
                speechClient.longRunningRecognizeAsync(config, audio);
        
        // Poll for completion
        while (!operation.isDone()) {
            try {
                Thread.sleep(Config.POLL_INTERVAL_SEC * 1000);
                setStatus(statusCallback, "Transcribing…");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Transcription interrupted", e);
            }
        }
        
        setStatus(statusCallback, "Processing result…");
        
        // Get results
        LongRunningRecognizeResponse response;
        try {
            response = operation.get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get transcription result: " + e.getMessage(), e);
        }
        
        // Extract transcript text
        StringBuilder transcript = new StringBuilder();
        for (SpeechRecognitionResult result : response.getResultsList()) {
            if (result.getAlternativesCount() > 0) {
                SpeechRecognitionAlternative alternative = result.getAlternatives(0);
                String text = alternative.getTranscript().trim();
                if (!text.isEmpty()) {
                    if (transcript.length() > 0) {
                        transcript.append("\n");
                    }
                    transcript.append(text);
                }
            }
        }
        
        // Delete blob from GCS
        blob.delete();
        AuditLogger.log("gcs_delete", audioPath.getFileName().toString(), 
                patient != null ? patient : "", "Deleted blob after transcription");
        
        setStatus(statusCallback, "Completed");
        return transcript.toString();
    }
    
    /**
     * Helper to set status via callback if provided.
     */
    private static void setStatus(Consumer<String> callback, String message) {
        if (callback != null) {
            callback.accept(message);
        }
    }
}

