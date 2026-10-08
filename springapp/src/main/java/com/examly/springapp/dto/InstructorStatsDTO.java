package com.examly.springapp.dto;

/**
 * Numbers for the instructor dashboard (only the instructor's own courses are counted).
 *
 * @author Lokesh
 */
public class InstructorStatsDTO {
    private long totalCourses;
    private long totalLessons;
    private long totalEnrollments;
    private long totalLearners;
    private double totalRevenue;

    public InstructorStatsDTO() {}

    /** All-fields constructor; chains to the no-arg constructor. */
    public InstructorStatsDTO(long totalCourses, long totalLessons, long totalEnrollments, long totalLearners, double totalRevenue) {
        this();
        this.totalCourses = totalCourses;
        this.totalLessons = totalLessons;
        this.totalEnrollments = totalEnrollments;
        this.totalLearners = totalLearners;
        this.totalRevenue = totalRevenue;
    }

    public long getTotalCourses() { return totalCourses; }
    public long getTotalLessons() { return totalLessons; }
    public long getTotalEnrollments() { return totalEnrollments; }
    public long getTotalLearners() { return totalLearners; }
    public double getTotalRevenue() { return totalRevenue; }
}
