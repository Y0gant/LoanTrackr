package com.loantrackr.service;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir = "uploads";

    public String storeFile(MultipartFile file, String folder) {
        validateFile(file);

        String sanitizedFilename = sanitizeFilename(file.getOriginalFilename());
        String extension = FilenameUtils.getExtension(sanitizedFilename).toLowerCase();
        String fileName = UUID.randomUUID() + "-" + sanitizedFilename;

        Path targetLocation = resolveAndValidatePath(folder, fileName);

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return Paths.get(folder, fileName).toString(); // return relative path
        } catch (IOException e) {
            throw new RuntimeException("File storage failed", e);
        }
    }


    private void validateFile(MultipartFile file) {
        List<String> allowedTypes = List.of(
                "text/plain", "application/pdf", "image/jpeg", "image/png",
                "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        );
        List<String> allowedExtensions = List.of("pdf", "jpg", "jpeg", "png", "doc", "docx", "txt");

        String contentType = file.getContentType();
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());

        if (extension != null) {
            extension = extension.toLowerCase();
        }

        if (!allowedTypes.contains(contentType)) {
            throw new IllegalArgumentException("Unsupported file type: " + contentType);
        }

        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException("Unsupported file extension: " + extension);
        }
    }

    private String sanitizeFilename(String original) {
        return Paths.get(original).getFileName().toString()
                .replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private Path resolveAndValidatePath(String folder, String filename) {
        try {
            Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path directoryPath = uploadRoot.resolve(folder).normalize();
            Files.createDirectories(directoryPath);

            Path targetPath = directoryPath.resolve(filename).normalize();
            if (!targetPath.startsWith(uploadRoot)) {
                throw new SecurityException("Invalid path — potential path traversal detected.");
            }
            return targetPath;
        } catch (IOException e) {
            throw new RuntimeException("Directory resolution failed", e);
        }
    }


    public Resource loadFile(String filePath) {
        try {
            Path path = Paths.get(filePath).normalize().toAbsolutePath();
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists()) return resource;
            else throw new FileNotFoundException("File not found: " + path);
        } catch (Exception e) {
            throw new RuntimeException("Could not read file " + filePath, e);
        }
    }

}
