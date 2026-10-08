package com.examly.springapp.service;

import com.examly.springapp.exception.InvalidRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Keeps video files in a folder on the server (app.video.dir) and nowhere else. The folder is NOT the public
 * "uploads" folder: a video can only be played through the signed streaming endpoint, so access rules cannot be bypassed
 * by guessing a file name.
 *
 * <p>Safety: the type (MP4 or WebM) is detected from the first bytes of the file (the name and Content-Type sent by the
 * browser are never trusted), the size is limited, and the stored name is a random UUID chosen by the server.
 *
 * @author Lokesh
 */
@Service
public class VideoStorageService {
    private static final Logger log = LoggerFactory.getLogger(VideoStorageService.class);

    /** What was saved: the path relative to the video folder, the detected media type and the size in bytes. */
    public record StoredVideo(String relativePath, String contentType, long size) {
    }

    private final Path root;
    private final long maxBytes;

    public VideoStorageService(@Value("${app.video.dir:video-storage}") String dir,
                               @Value("${app.video.max-bytes:524288000}") long maxBytes) throws IOException {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
        Files.createDirectories(this.root);
        log.info("Video folder: {}", this.root);
    }

    /** Checks and saves the file under course-{id}/{uuid}.{ext}. */
    public StoredVideo store(MultipartFile file, Long courseId) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Please choose a video file");
        }
        if (file.getSize() > maxBytes) {
            throw new InvalidRequestException("The video is too large. The limit is " + (maxBytes / (1024 * 1024)) + " MB");
        }
        String[] type = detectType(file);   // {extension, media type}
        if (type == null) {
            throw new InvalidRequestException("Only MP4 or WebM videos are allowed");
        }
        String relative = "course-" + courseId + "/" + UUID.randomUUID() + "." + type[0];
        Path target = resolve(relative);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target);
            }
        } catch (IOException e) {
            log.error("Could not save an uploaded video", e);
            throw new UncheckedIOException("Could not save the video", e);
        }
        log.info("Video saved as {} ({} bytes)", relative, file.getSize());
        return new StoredVideo(relative, type[1], file.getSize());
    }

    /** Turns a stored relative path into the real file path, refusing anything that would leave the video folder. */
    public Path resolve(String relativePath) {
        Path p = root.resolve(relativePath).normalize();
        if (!p.startsWith(root)) {
            throw new InvalidRequestException("Invalid file path");
        }
        return p;
    }

    /** Removes a stored file. A failure is logged but never thrown: the database row is what matters to the user. */
    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(relativePath));
            log.debug("Video file {} deleted", relativePath);
        } catch (IOException | RuntimeException e) {
            log.warn("Could not delete the video file {}: {}", relativePath, e.getMessage());
        }
    }

    /** Looks at the first bytes: MP4 has "ftyp" at byte 4, WebM starts with 1A 45 DF A3. Null when it is neither. */
    private String[] detectType(MultipartFile file) {
        byte[] h = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(h, 0, h.length);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the video", e);
        }
        if (read < 12) {
            return null;
        }
        if (h[4] == 'f' && h[5] == 't' && h[6] == 'y' && h[7] == 'p') {
            return new String[] { "mp4", "video/mp4" };
        }
        if ((h[0] & 0xFF) == 0x1A && (h[1] & 0xFF) == 0x45 && (h[2] & 0xFF) == 0xDF && (h[3] & 0xFF) == 0xA3) {
            return new String[] { "webm", "video/webm" };
        }
        return null;
    }
}
