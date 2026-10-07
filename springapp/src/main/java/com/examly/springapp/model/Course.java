package com.examly.springapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

/**
 * A course that customers can browse, search, add to the cart and buy. A course created by an instructor remembers
 * its owner; courses created by an admin have no instructor.
 *
 * @author Sivamuthu
 */
@Entity
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long courseId;

    @NotBlank(message = "Course type is required")
    @Size(max = 100, message = "Course type is too long")
    private String courseType;

    @Pattern(regexp = "^$|^https?://.+", message = "Image URL must start with http:// or https://")
    @Size(max = 2000, message = "Image URL is too long")
    @Column(length = 2000)
    private String courseImageUrl;

    @NotBlank(message = "Course details are required")
    @Size(max = 2000, message = "Course details are too long")
    @Column(length = 2000)
    private String courseDetails;

    @NotNull(message = "Course price is required")
    @PositiveOrZero(message = "Course price cannot be negative")
    private Double coursePrice;

    /** The instructor who owns the course (null for admin-created courses). Never sent to the browser as an object: it holds an email address. */
    @ManyToOne
    @JoinColumn(name = "instructor_id")
    @JsonIgnore
    private User instructor;

    /** No-arg constructor required by JPA and Jackson. */
    public Course() {}

    /** Convenience constructor for a new course (no id yet); chains to the all-fields constructor. */
    public Course(String courseType, String courseImageUrl, String courseDetails, Double coursePrice) {
        this(null, courseType, courseImageUrl, courseDetails, coursePrice);
    }

    /** All-fields constructor; chains to the no-arg constructor. */
    public Course(Long courseId, String courseType, String courseImageUrl, String courseDetails, Double coursePrice) {
        this();
        this.courseId = courseId;
        this.courseType = courseType;
        this.courseImageUrl = courseImageUrl;
        this.courseDetails = courseDetails;
        this.coursePrice = coursePrice;
    }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public String getCourseType() { return courseType; }
    public void setCourseType(String courseType) { this.courseType = courseType; }
    public String getCourseImageUrl() { return courseImageUrl; }
    public void setCourseImageUrl(String courseImageUrl) { this.courseImageUrl = courseImageUrl; }
    public String getCourseDetails() { return courseDetails; }
    public void setCourseDetails(String courseDetails) { this.courseDetails = courseDetails; }
    public Double getCoursePrice() { return coursePrice; }
    public void setCoursePrice(Double coursePrice) { this.coursePrice = coursePrice; }
    public User getInstructor() { return instructor; }
    public void setInstructor(User instructor) { this.instructor = instructor; }

    /** Sent to the browser instead of the whole instructor; read-only, so a request body cannot set it. */
    @JsonProperty(value = "instructorId", access = JsonProperty.Access.READ_ONLY)
    public Long getInstructorId() { return instructor == null ? null : instructor.getUserId(); }

    /** The display name shown on the course page; read-only. */
    @JsonProperty(value = "instructorName", access = JsonProperty.Access.READ_ONLY)
    public String getInstructorName() { return instructor == null ? null : instructor.getUsername(); }
}