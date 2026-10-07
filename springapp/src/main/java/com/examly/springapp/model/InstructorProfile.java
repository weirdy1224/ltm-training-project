package com.examly.springapp.model;

import com.examly.springapp.dto.InstructorProfileRequest;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * What an instructor told us about themselves when registering (experience, skills, background). One row per instructor;
 * admins read it before approving the account.
 *
 * @author Lokesh
 */
@Entity
@Table(name = "instructor_profiles")
public class InstructorProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 100)
    private String headline;

    @Column(nullable = false)
    private int yearsOfExperience;

    @Column(nullable = false, length = 255)
    private String expertise;

    @Column(nullable = false, length = 150)
    private String qualification;

    @Column(length = 120)
    private String organization;

    @Column(nullable = false, length = 1000)
    private String about;

    @Column(nullable = false, length = 2000)
    private String experienceSummary;

    @Column(length = 200)
    private String linkedinUrl;

    @Column(length = 200)
    private String portfolioUrl;

    @Column(nullable = false)
    private LocalDateTime submittedAt;

    /** No-arg constructor required by JPA. */
    public InstructorProfile() {}

    /** Creates a profile for a user from the registration form; chains to the no-arg constructor. */
    public InstructorProfile(User user, InstructorProfileRequest request) {
        this();
        this.user = user;
        applyFrom(request);
    }

    /** Copies the (already validated) form values, trimming spaces; empty optional values become null. */
    public final void applyFrom(InstructorProfileRequest r) {
        this.headline = r.getHeadline().trim();
        this.yearsOfExperience = r.getYearsOfExperience();
        this.expertise = r.getExpertise().trim();
        this.qualification = r.getQualification().trim();
        this.organization = blankToNull(r.getOrganization());
        this.about = r.getAbout().trim();
        this.experienceSummary = r.getExperienceSummary().trim();
        this.linkedinUrl = blankToNull(r.getLinkedinUrl());
        this.portfolioUrl = blankToNull(r.getPortfolioUrl());
        this.submittedAt = LocalDateTime.now();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getHeadline() { return headline; }
    public int getYearsOfExperience() { return yearsOfExperience; }
    public String getExpertise() { return expertise; }
    public String getQualification() { return qualification; }
    public String getOrganization() { return organization; }
    public String getAbout() { return about; }
    public String getExperienceSummary() { return experienceSummary; }
    public String getLinkedinUrl() { return linkedinUrl; }
    public String getPortfolioUrl() { return portfolioUrl; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
}
