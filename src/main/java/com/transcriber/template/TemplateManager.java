package com.transcriber.template;

import com.transcriber.config.Config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities for loading and applying transcription templates.
 */
public class TemplateManager {
    
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*([A-Z0-9_]+)\\s*\\}\\}");
    
    /**
     * Load all template files from the templates directory.
     * Returns a mapping of template name (filename without extension) to file path.
     */
    public static Map<String, Path> loadTemplates() {
        ensureTemplateDirectory();

        Map<String, Path> templates = new HashMap<>();
        if (!Files.exists(Config.TEMPLATES_DIR)) {
            return templates;
        }
        
        try {
            Files.list(Config.TEMPLATES_DIR)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".txt"))
                .forEach(path -> {
                    String name = getFileNameWithoutExtension(path);
                    templates.put(name, path);
                });
        } catch (IOException e) {
            System.err.println("Failed to load templates: " + e.getMessage());
        }
        
        return templates;
    }
    
    /**
     * Apply a template by replacing placeholders with values from the context.
     * 
     * @param templatePath Path to the template file
     * @param transcript The transcription text to insert at {{TRANSCRIPT}}
     * @param context Additional context values (e.g., PATIENT, DOB)
     * @return The processed template with placeholders replaced
     * @throws IOException If the template file cannot be read
     */
    public static String applyTemplate(Path templatePath, String transcript, Map<String, String> context) 
            throws IOException {
        String raw = Files.readString(templatePath);
        Map<String, String> replacements = new HashMap<>(context != null ? context : Map.of());
        replacements.put("TRANSCRIPT", transcript != null ? transcript : "");
        
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(raw);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = replacements.getOrDefault(key, "");
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }

    /**
     * Ensure the template directory exists and the default template is available.
     */
    private static void ensureTemplateDirectory() {
        try {
            Files.createDirectories(Config.TEMPLATES_DIR);
            Path defaultTemplatePath = Config.TEMPLATES_DIR.resolve("default_template.txt");
            if (!Files.exists(defaultTemplatePath)) {
                try (InputStream in = TemplateManager.class.getClassLoader()
                        .getResourceAsStream("templates/default_template.txt")) {
                    if (in != null) {
                        Files.copy(in, defaultTemplatePath);
                    } else {
                        System.err.println("Default template resource not found in classpath.");
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to initialize template directory: " + e.getMessage());
        }
    }
    
    /**
     * Get filename without extension.
     */
    private static String getFileNameWithoutExtension(Path path) {
        String fileName = path.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }
}


