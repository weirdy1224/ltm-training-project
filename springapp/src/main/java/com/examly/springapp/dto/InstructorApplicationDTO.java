package com.examly.springapp.dto;

import com.examly.springapp.model.InstructorProfile;
import com.examly.springapp.model.User;
import java.time.LocalDateTime;

/**
 * One row of the admin's instructor list: the account state plus the details the instructor entered while registering.
 * The profile fields are null for instructor accounts created before profiles existed.
 *
 * @author Lokesh
 */
public class InstructorApplicationDTO {
    private Long userId;
    private String username;
    private String email;
    private String mobileNumber;
    private boolean emailVerified;
    private boolean approved;
    private boolean hasProfile;
    private String headline;
    private Integer yearsOfExperience;
    private String expertise;
    private String qualification;
    private String organization;
    private String about;
    private String experienceSummary;
    private String linkedinUrl;
    private String portfolioUrl;
    private LocalDateTime submittedAt;

    /** No-arg constructor required by Jackson. */
    public InstructorApplicationDTO() {}

    /** Builds the row from the account and its (possibly missing) profile; chains to the no-arg constructor. */
    public InstructorApplicationDTO(User user, InstructorProfile profile) {
        this();
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.mobileNumber = user.getMobileNumber();
        this.emailVerified = user.hasVerifiedEmail();
        this.approved = user.hasBeenApproved();
        this.hasProfile = profile != null;
        if (profile != null) {
            this.headline = profile.getHeadline();
            this.yearsOfExperience = profile.getYearsOfExperience();
            this.expertise = profile.getExpertise();
            this.qualification = profile.getQualification();
            this.organization = profile.getOrganization();
            this.about = profile.getAbout();
            this.experienceSummary = profile.getExperienceSummary();
            this.linkedinUrl = profile.getLinkedinUrl();
            this.portfolioUrl = profile.getPortfolioUrl();
            this.submittedAt = profile.getSubmittedAt();
        }
    }

    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getMobileNumber() { return mobileNumber; }
    public boolean isEmailVerified() { return emailVerified; }
    public boolean isApproved() { return approved; }
    public boolean isHasProfile() { return hasProfile; }
    public String getHeadline() { return headline; }
    public Integer getYearsOfExperience() { return yearsOfExperience; }
    public String getExpertise() { return expertise; }
    public String getQualification() { return qualification; }
    public String getOrganization() { return organization; }
    public String getAbout() { return about; }
    public String getExperienceSummary() { return experienceSummary; }
    public String getLinkedinUrl() { return linkedinUrl; }
    public String getPortfolioUrl() { return portfolioUrl; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
}
