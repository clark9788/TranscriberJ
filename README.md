# TranscriberJ - Medical Transcriber Application (Java)

A HIPAA-compliant medical transcription application converted from Python to Java. This application provides audio recording, Google Cloud Speech-to-Text integration, template management, and secure file deletion features.

## Features

- **Audio Recording** - Record audio from microphone, save as WAV files (16kHz, mono, 16-bit PCM)
- **Google Cloud Speech-to-Text** - Upload audio to GCS, transcribe using medical_conversation model
- **Template System** - Apply templates with placeholders ({{TRANSCRIPT}}, {{PATIENT}}, {{DOB}})
- **File Management** - Save/load transcriptions with patient name and DOB in filename
- **Secure Deletion** - HIPAA-compliant file deletion with multiple overwrite passes
- **Audit Logging** - Log all file operations for HIPAA compliance
- **Transcription Cleaning** - Remove filler words from transcriptions
- **JavaFX GUI** - Modern, user-friendly interface

## Prerequisites

- **Java 17** or higher
- **Maven** 3.6+ (for building)
- **Google Cloud Account** with:
  - Speech-to-Text API enabled
  - Cloud Storage API enabled
  - Service account with appropriate permissions
- **Microphone access** (for audio recording)
- **Google Cloud credentials** - Set `GOOGLE_APPLICATION_CREDENTIALS` environment variable pointing to your service account JSON key file

## Setup

### 1. Google Cloud Credentials Setup

1. Create a Google Cloud project (if you don't have one)
2. Enable the Speech-to-Text API and Cloud Storage API
3. Create a service account and download the JSON key file
4. Store the key file in a secure location (e.g., `C:\Users\<YourUsername>\.credentials\gcp-service-account-key.json`)
5. Set the `GOOGLE_APPLICATION_CREDENTIALS` environment variable:
   - Windows: System Properties → Environment Variables → New User Variable
   - Variable name: `GOOGLE_APPLICATION_CREDENTIALS`
   - Variable value: Full path to your JSON key file

### 2. Build the Project

```bash
mvn clean compile
```

### 3. Run the Application

```bash
mvn javafx:run
```

Or build an executable JAR:

```bash
mvn clean package
java -jar target/transcriberj-1.0.1.jar
```

## Usage

### Basic Workflow

1. Enter **Patient Name** and **DOB** (YYYYMMDD format)
2. Optionally select a **Template** from the dropdown
3. Click **"Record"** to start recording
4. Click **"Stop"** when finished
5. Click **"Send to Google"** to transcribe
6. Review and edit the transcription in the editor
7. Click **"Clean Transcription"** to remove filler words (optional)
8. Click **"Save"** to save the transcription

### File Management

- Load existing transcriptions from the left panel file browser
- Click on a file name to load it into the editor
- Click **"Delete Transcription"** to securely delete a loaded file
- Click **"Delete Recording"** to securely delete the current audio recording

## Project Structure

```
TranscriberJ/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── transcriber/
│       │           ├── Main.java                    # Application entry point
│       │           ├── TranscriberApp.java          # Main GUI controller
│       │           ├── config/
│       │           │   └── Config.java              # Configuration constants
│       │           ├── audio/
│       │           │   └── AudioRecorder.java       # Audio recording
│       │           ├── cloud/
│       │           │   └── GCloudTranscriber.java   # Google Cloud integration
│       │           ├── file/
│       │           │   └── FileManager.java         # File operations & secure deletion
│       │           ├── template/
│       │           │   └── TemplateManager.java     # Template loading and application
│       │           ├── audit/
│       │           │   └── AuditLogger.java         # HIPAA audit logging
│       │           └── text/
│       │               └── TranscriptionCleaner.java # Filler word removal
│       └── resources/
│           └── templates/                           # Template files
│           |   └── default_template.txt
|           |__ styles/
|               |__ app.css                          # css file for gui style
├── transcriptions/                                  # Generated: saved transcriptions
├── recordings/                                      # Generated: temporary audio files
├── audit_logs/                                      # Generated: audit log CSV files
├── pom.xml                                          # Maven build configuration
├── README.md
└── Plan.md
```

## Configuration

Configuration is managed in `com.transcriber.config.Config`:

- **GCS Bucket**: `transcribe_bucket9788` (update if needed)
- **Language Code**: `en-US`
- **Model**: `medical_conversation`
- **Audio Format**: 16kHz, mono, 16-bit PCM
- **Secure Deletion**: 3 overwrite passes

## HIPAA Compliance Features

- **Secure Deletion**: Audio files overwritten multiple times before deletion
- **Audit Logging**: All file operations logged to `audit_logs/audit_log.csv`
- **Retention Awareness**: Transcriptions saved in dedicated directory (subject to state retention laws)

## Dependencies

- Google Cloud Speech-to-Text API (v4.0.0)
- Google Cloud Storage API (v2.20.0)
- JavaFX (v17.0.2)
- Java Sound API (built-in)

## Troubleshooting

- **"Failed to initialize Google Cloud clients"**: Check that `GOOGLE_APPLICATION_CREDENTIALS` is set correctly
- **Audio recording issues**: Check microphone permissions in Windows Settings
- **Transcription fails**: Verify internet connection and Google Cloud API access
- **File not found errors**: Ensure the application has write permissions in its directory

## License

[Add your license here]

## Version

1.0.1


"# TranscriberJ" 
