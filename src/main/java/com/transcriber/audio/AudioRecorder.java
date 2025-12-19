package com.transcriber.audio;

import com.transcriber.audit.AuditLogger;
import com.transcriber.config.Config;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Audio recording utilities using Java Sound API.
 * Threaded WAV recorder that streams microphone audio to disk.
 */
public class AudioRecorder {
    
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            .withZone(ZoneId.systemDefault());
    
    private TargetDataLine line;
    private Thread recordingThread;
    private final AtomicBoolean recording = new AtomicBoolean(false);
    private Path currentFile;
    private ByteArrayOutputStream audioBuffer;
    
    /**
     * Get the current recording file path.
     */
    public Path getCurrentFile() {
        return currentFile;
    }
    
    /**
     * Check if currently recording.
     */
    public boolean isRecording() {
        return recording.get();
    }
    
    /**
     * Start recording audio from the microphone.
     * 
     * @return Path to the recording file
     * @throws RuntimeException if recording is already in progress or setup fails
     */
    public Path start() {
        if (recording.get()) {
            throw new RuntimeException("Recording already in progress");
        }
        
        try {
            Files.createDirectories(Config.RECORDINGS_DIR);
            String timestamp = TIMESTAMP_FORMATTER.format(Instant.now());
            currentFile = Config.RECORDINGS_DIR.resolve("recording_" + timestamp + ".wav");
            
            // Audio format: 16kHz, mono, 16-bit PCM
            AudioFormat format = new AudioFormat(
                Config.SAMPLE_RATE,
                16, // bits per sample
                Config.CHANNELS,
                true, // signed
                false // little-endian
            );
            
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                throw new RuntimeException("Audio format not supported: " + format);
            }
            
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            audioBuffer = new ByteArrayOutputStream();
            recording.set(true);
            
            recordingThread = new Thread(this::record, "AudioRecorder");
            recordingThread.setDaemon(true);
            recordingThread.start();
            
            AuditLogger.log("record_start", currentFile, "", "Recording started");
            return currentFile;
            
        } catch (LineUnavailableException | IOException e) {
            recording.set(false);
            throw new RuntimeException("Failed to start recording: " + e.getMessage(), e);
        }
    }
    
    /**
     * Internal recording method that runs in a separate thread.
     */
    private void record() {
        line.start();
        byte[] buffer = new byte[4096];
        
        try {
            while (recording.get()) {
                int bytesRead = line.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    audioBuffer.write(buffer, 0, bytesRead);
                }
            }
        } catch (Exception e) {
            System.err.println("Error during recording: " + e.getMessage());
        } finally {
            line.stop();
            line.close();
            saveRecording();
        }
    }
    
    /**
     * Save the recorded audio to a WAV file.
     */
    private void saveRecording() {
        try {
            byte[] audioData = audioBuffer.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
            
            AudioFormat format = new AudioFormat(
                Config.SAMPLE_RATE,
                16,
                Config.CHANNELS,
                true,
                false
            );
            
            AudioInputStream audioInputStream = new AudioInputStream(bais, format, audioData.length / format.getFrameSize());
            AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, currentFile.toFile());
            audioInputStream.close();
            
        } catch (IOException e) {
            System.err.println("Failed to save recording: " + e.getMessage());
        }
    }
    
    /**
     * Stop recording.
     * 
     * @return Path to the recording file, or null if no recording was in progress
     */
    public Path stop() {
        if (!recording.get()) {
            return currentFile;
        }
        
        recording.set(false);
        
        if (recordingThread != null) {
            try {
                recordingThread.join(2000); // Wait up to 2 seconds for thread to finish
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        AuditLogger.log("record_stop", currentFile != null ? currentFile : Path.of(""), "", "Recording stopped");
        return currentFile;
    }
}


