package com.examly.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * One video lesson of a course. The video FILE lives in the video folder on the server (app.video.dir); this row keeps
 * its relative path and details. The entity is never sent to the browser directly (see CourseVideoDTO) so that the
 * server's file path stays private.
 *
 * @author Lokesh
 */
@Entity
@Table(name = "course_video")
public class CourseVideo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long videoId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnore
    private Course course;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 1000)
    private String description;

    /** Position in the course (1 = first lesson). */
    private int orderIndex;

    @Column(length = 255)
    private String originalFileName;

    /** Path of the saved file relative to the video folder, for example "course-5/3f2a....mp4". */
    @Column(nullable = false, length = 300)
    @JsonIgnore
    private String filePath;

    @Column(length = 60)
    private String contentType;

    private long sizeBytes;

    @ManyToOne
    @JoinColumn(name = "uploaded_by")
    @JsonIgnore
    private User uploadedBy;

    private LocalDateTime uploadedAt;

    /** No-arg constructor required by JPA. */
    public CourseVideo() {}

    /** New lesson (no id yet); chains to the no-arg constructor. */
    public CourseVideo(Course course, String title, String description, int orderIndex, String originalFileName,
                       String filePath, String contentType, long sizeBytes, User uploadedBy) {
        this();
        this.course = course;
        this.title = title;
        this.description = description;
        this.orderIndex = orderIndex;
        this.originalFileName = originalFileName;
        this.filePath = filePath;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = LocalDateTime.now();
    }

    public Long getVideoId() { return videoId; }
    public void setVideoId(Long videoId) { this.videoId = videoId; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
    public User getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(User uploadedBy) { this.uploadedBy = uploadedBy; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
