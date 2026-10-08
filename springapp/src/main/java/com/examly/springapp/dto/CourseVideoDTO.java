package com.examly.springapp.dto;

import com.examly.springapp.model.CourseVideo;
import java.time.LocalDateTime;

/**
 * What the browser gets for a lesson. It has no file path; canWatch tells the page whether the caller may play it
 * (enrolled customer, owner instructor or admin) so locked lessons can be shown with a lock.
 *
 * @author Lokesh
 */
public class CourseVideoDTO {
    private Long videoId;
    private Long courseId;
    private String title;
    private String description;
    private int orderIndex;
    private String originalFileName;
    private String contentType;
    private long sizeBytes;
    private LocalDateTime uploadedAt;
    private boolean canWatch;
    private String videoUrl;

    public String getVideoUrl() {
        return videoUrl;
    }

    public CourseVideoDTO() {}

    /** Builds the DTO of a stored lesson for a caller who may or may not watch it. */
    public static CourseVideoDTO from(CourseVideo v, boolean canWatch) {
        CourseVideoDTO d = new CourseVideoDTO();
    
        d.videoId = v.getVideoId();
        d.courseId = v.getCourse() == null
                ? null
                : v.getCourse().getCourseId();
    
        d.title = v.getTitle();
        d.description = v.getDescription();
        d.orderIndex = v.getOrderIndex();
        d.originalFileName = v.getOriginalFileName();
        d.contentType = v.getContentType();
        d.sizeBytes = v.getSizeBytes();
        d.uploadedAt = v.getUploadedAt();
        d.canWatch = canWatch;
        d.videoUrl = v.getVideoUrl();
    
        return d;
    }

    public Long getVideoId() { return videoId; }
    public Long getCourseId() { return courseId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getOrderIndex() { return orderIndex; }
    public String getOriginalFileName() { return originalFileName; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public boolean isCanWatch() { return canWatch; }
}
