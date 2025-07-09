package com.loantrackr.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TemplateUtilTest {

    @Test
    @DisplayName("Template file should exist and be readable")
    void testTemplateFileExists() throws Exception {
        ClassPathResource resource = new ClassPathResource("templates/emails/lenderRejection.html");
        assertTrue(resource.exists(), "Template file does not exist");
        String content = Files.readString(resource.getFile().toPath(), StandardCharsets.UTF_8);
        assertNotNull(content, "Template file is empty");
        assertTrue(content.contains("{{organizationName}}"), "Missing placeholder: organizationName");
        assertTrue(content.contains("{{rejectionReason}}"), "Missing placeholder: rejectionReason");
    }

    @Test
    @DisplayName("Template placeholders should be replaced correctly")
    void testTemplateReplacement() {
        Map<String, String> replacements = Map.of(
                "organizationName", "XYZ Capital Pvt Ltd",
                "rejectionReason", "Invalid business registration number"
        );

        String result = TemplateUtil.loadTemplate("templates/emails/lenderRejection.html", replacements);

        assertNotNull(result);
        assertTrue(result.contains("XYZ Capital Pvt Ltd"), "organizationName not replaced");
        assertTrue(result.contains("Invalid business registration number"), "rejectionReason not replaced");

        assertFalse(result.contains("{{organizationName}}"), "organizationName placeholder not replaced");
        assertFalse(result.contains("{{rejectionReason}}"), "rejectionReason placeholder not replaced");
    }
}


