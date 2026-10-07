package com.examly.springapp.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body of PUT /api/video/{videoId}: rename a lesson, change its description or move it in the playing order.
 *
 * @author Lokesh
 */
public class VideoUpdateRequest {
    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title is too long (maximum 150 characters)")
    private String title;

    @Size(max = 1000, message = "Description is too long (maximum 1000 characters)")
    private String description;

    @Min(value = 1, message = "Order must be 1 or more")
    private Integer orderIndex;

    public VideoUpdateRequest() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getOrderIndex() { return orderIndex; }
    public void setOrderIndex(Integer orderIndex) { this.orderIndex = orderIndex; }
}
