package com.examly.springapp.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.examly.springapp.exception.InvalidRequestException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryVideoStorageService {

    private final Cloudinary cloudinary;

    public CloudinaryVideoStorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public record StoredVideo(
            String publicId,
            String url,
            String contentType,
            long size
    ) {}

    public StoredVideo store(
            MultipartFile file,
            Long courseId) {

        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException(
                    "Please choose a video file"
            );
        }

        try {

            Map<?, ?> result =
                    cloudinary.uploader().upload(
                            file.getBytes(),
                            ObjectUtils.asMap(
                                    "resource_type", "video",
                                    "folder", "courses/" + courseId
                            )
                    );

            return new StoredVideo(
                    result.get("public_id").toString(),
                    result.get("secure_url").toString(),
                    file.getContentType(),
                    file.getSize()
            );

        } catch (IOException e) {
            throw new RuntimeException(
                    "Video upload failed",
                    e
            );
        }
    }

    public void delete(String publicId) {

        try {

            cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap(
                            "resource_type",
                            "video"
                    )
            );

        } catch (Exception ignored) {
        }
    }
}