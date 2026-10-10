package com.examly.springapp.service;

import com.examly.springapp.exception.InvalidRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Saves an uploaded picture in the uploads folder and returns its public URL. Shared by the course image upload and
 * the ticket screenshots. The file type is detected from the first bytes (the name and Content-Type sent by the
 * browser are never trusted), the size is limited and the stored name is a random UUID chosen by the server.
 */
@Service
public class ImageStorageService {
    private static final Logger log = LoggerFactory.getLogger(ImageStorageService.class);
    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5 MB
    private final Path uploadDir;

    public ImageStorageService(@Value("${app.upload.dir:uploads}") String dir) throws IOException {
        this.uploadDir = Paths.get(dir).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDir);
    }

    /** Validates and stores the image; returns the public URL (/uploads/...). */
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Please choose an image file");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new InvalidRequestException("The image is too large. The limit is 5 MB");
        }
        String extension = detectExtension(file);
        if (extension == null) {
            throw new InvalidRequestException("Only PNG, JPG, GIF or WEBP images are allowed");
        }
        String storedName = UUID.randomUUID() + "." + extension;
        Path target = uploadDir.resolve(storedName).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new InvalidRequestException("Invalid file name");
        }
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Could not save an uploaded image", e);
            throw new UncheckedIOException("Could not save the image", e);
        }
        log.info("Image saved as {}", storedName);
        return ServletUriComponentsBuilder.fromCurrentContextPath().path("/uploads/").path(storedName).toUriString();
    }

    /** Returns png, jpg, gif or webp from the first bytes of the file; null when it is none of these. */
    private String detectExtension(MultipartFile file) {
        byte[] h = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(h, 0, h.length);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the image", e);
        }
        if (read < 12) {
            return null;
        }
        if ((h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') {
            return "png";
        }
        if ((h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8') {
            return "gif";
        }
        if (h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F' && h[8] == 'W' && h[9] == 'E' && h[10] == 'B'
                && h[11] == 'P') {
            return "webp";
        }
        return null;
    }
}