package com.examly.springapp.controller;

import com.examly.springapp.service.ImageStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Course image upload. The admin or instructor picks a picture in the browser, this endpoint saves it in the uploads
 * folder (see {@link ImageStorageService}) and answers with its public URL. That URL is then stored in the existing
 * Course.courseImageUrl field, so the Course model and the course endpoints do not change.
 *
 * @author Team Lead
 */
@RestController
@RequestMapping("/api/upload")
@Tag(name = "Uploads", description = "Course image upload")
public class UploadController {
    private final ImageStorageService images;

    public UploadController(ImageStorageService images) {
        this.images = images;
    }

    @Operation(summary = "Upload a course image", description = "ADMIN or INSTRUCTOR. PNG, JPG, GIF or WEBP, up to 5 MB. Returns the public URL.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image saved, {\"url\": \"...\"} returned"),
            @ApiResponse(responseCode = "400", description = "Empty file, wrong type or too large"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not allowed")
    })
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(Map.of("url", images.store(file)));
    }
}