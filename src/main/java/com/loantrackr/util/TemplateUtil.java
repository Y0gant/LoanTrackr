package com.loantrackr.util;

import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;


public class TemplateUtil {

    public static String loadTemplate(String path, Map<String, String> placeholders) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            String content = Files.readString(resource.getFile().toPath(), StandardCharsets.UTF_8);

            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                content = content.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }

            return content;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load email template: " + path, e);
        }
    }
}


