package com.examly.springapp.controller;

import com.examly.springapp.dto.CourseVideoDTO;
import com.examly.springapp.dto.VideoUpdateRequest;
import com.examly.springapp.service.CourseVideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Video lessons. The video files are saved in a folder on the server and their path and details in the database.
 *
 * <p><b>How playing works.</b> A browser {@code <video>} tag cannot send the Authorization header, so playing takes two steps:
 * (1) POST /api/video/{id}/stream-token with the normal login token (the server checks that the caller may watch this
 * lesson) and (2) GET /api/video/{id}/stream?token=... which needs no login header because the signed, short-lived token
 * in the URL is the proof. The stream answers HTTP Range requests, so seeking in the player works.
 *
 * @author Lokesh
 */
@RestController
@RequestMapping("/api/video")
@Validated
@Tag(name = "Videos", description = "Video lessons of courses: upload, list, edit, delete and stream")
public class VideoController {
    private static final Logger log = LoggerFactory.getLogger(VideoController.class);

    private final CourseVideoService videoService;

    @Autowired
    public VideoController(CourseVideoService videoService) {
        this.videoService = videoService;
    }

    @Operation(summary = "List the video lessons of a course", description = "ADMIN, INSTRUCTOR and CUSTOMER. Each lesson says whether the caller may watch it (canWatch).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lessons in playing order (may be empty)"),
            @ApiResponse(responseCode = "400", description = "Invalid id"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "404", description = "Course not found")
    })
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<CourseVideoDTO>> list(
            @PathVariable @Min(value = 1, message = "must be a positive number") Long courseId) {
        log.trace("list called for courseId={}", courseId);
        return ResponseEntity.status(HttpStatus.OK).body(videoService.listForCourse(courseId));
    }

    @Operation(summary = "Upload a video lesson", description = "ADMIN, or the INSTRUCTOR who owns the course. Multipart form: file (MP4 or WebM), title, optional description and orderIndex.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Lesson saved"),
            @ApiResponse(responseCode = "400", description = "Missing title, wrong file type or file too large"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not your course"),
            @ApiResponse(responseCode = "404", description = "Course not found"),
            @ApiResponse(responseCode = "413", description = "The upload is larger than the server allows")
    })
    @PostMapping(value = "/course/{courseId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CourseVideoDTO> upload(
            @PathVariable @Min(value = 1, message = "must be a positive number") Long courseId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "orderIndex", required = false) Integer orderIndex) {
        log.trace("upload called for courseId={}", courseId);
        CourseVideoDTO saved = videoService.upload(courseId, file, title, description, orderIndex);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "Edit a video lesson", description = "ADMIN, or the INSTRUCTOR who owns the course. Changes title, description and position only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lesson updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed or invalid id"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not your course"),
            @ApiResponse(responseCode = "404", description = "Video not found")
    })
    @PutMapping("/{videoId}")
    public ResponseEntity<CourseVideoDTO> update(
            @PathVariable @Min(value = 1, message = "must be a positive number") Long videoId,
            @Valid @RequestBody VideoUpdateRequest request) {
        log.trace("update called for videoId={}", videoId);
        return ResponseEntity.status(HttpStatus.OK).body(videoService.update(videoId, request));
    }

    @Operation(summary = "Delete a video lesson", description = "ADMIN, or the INSTRUCTOR who owns the course. Removes the database row and the file.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lesson deleted"),
            @ApiResponse(responseCode = "400", description = "Invalid id"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not your course"),
            @ApiResponse(responseCode = "404", description = "Video not found")
    })
    @DeleteMapping("/{videoId}")
    public ResponseEntity<Void> delete(
            @PathVariable @Min(value = 1, message = "must be a positive number") Long videoId) {
        log.trace("delete called for videoId={}", videoId);
        videoService.delete(videoId);
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @Operation(summary = "Get a link to play a lesson", description = "ADMIN, the owner INSTRUCTOR, or a CUSTOMER who bought the course. Returns {token, streamPath}; the token works for a limited time and for this video only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token issued"),
            @ApiResponse(responseCode = "400", description = "Invalid id"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Enroll in the course first"),
            @ApiResponse(responseCode = "404", description = "Video not found")
    })
    @PostMapping("/{videoId}/stream-token")
    public ResponseEntity<Map<String, String>> streamToken(
            @PathVariable @Min(value = 1, message = "must be a positive number") Long videoId) {
        log.trace("streamToken called for videoId={}", videoId);
        String token = videoService.createStreamToken(videoId);
        Map<String, String> body = new LinkedHashMap<>();
        body.put("token", token);
        body.put("streamPath", "/api/video/" + videoId + "/stream?token=" + token);
        return ResponseEntity.status(HttpStatus.OK).body(body);
    }

    /**
     * Streams the file. Returning a Resource lets Spring answer "Range: bytes=..." with 206 Partial Content,
     * which the browser's player needs for seeking.
     */
    @Operation(summary = "Stream a lesson", description = "Public path, protected by the signed token from /stream-token. Supports HTTP Range requests.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The whole video"),
            @ApiResponse(responseCode = "206", description = "A part of the video (Range request)"),
            @ApiResponse(responseCode = "403", description = "The token is invalid or expired"),
            @ApiResponse(responseCode = "404", description = "Video not found")
    })
    @GetMapping("/{videoId}/stream")
    public ResponseEntity<Resource> stream(
            @PathVariable @Min(value = 1, message = "must be a positive number") Long videoId,
            @RequestParam("token") @NotBlank(message = "must not be blank") String token) {
        log.trace("stream called for videoId={}", videoId);
        CourseVideoService.VideoFile file = videoService.openForStreaming(videoId, token);
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(new FileSystemResource(file.path()));
    }
}
