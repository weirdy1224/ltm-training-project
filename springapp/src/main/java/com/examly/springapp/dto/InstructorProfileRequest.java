package com.examly.springapp.dto;

import jakarta.validation.constraints.*;

/**
 * The details an instructor fills in while registering. An admin reads them before approving the account.
 * Sent inside the register request as "instructorProfile"; required for the INSTRUCTOR role only.
 *
 * @author Lokesh
 */
public class InstructorProfileRequest {
    private static final String URL = "^$|^https://[^\\s]{3,190}$";

    @NotBlank(message = "Professional headline is required")
    @Size(max = 100, message = "Headline must be at most 100 characters")
    private String headline;

    @NotNull(message = "Years of experience is required")
    @Min(value = 0, message = "Years of experience cannot be negative")
    @Max(value = 60, message = "Years of experience must be 60 or less")
    private Integer yearsOfExperience;

    @NotBlank(message = "Subjects or skills you teach are required")
    @Size(max = 255, message = "Skills must be at most 255 characters")
    private String expertise;

    @NotBlank(message = "Highest qualification is required")
    @Size(max = 150, message = "Qualification must be at most 150 characters")
    private String qualification;

    @Size(max = 120, message = "Organization must be at most 120 characters")
    private String organization;

    @NotBlank(message = "Please tell us about yourself")
    @Size(min = 30, max = 1000, message = "About you must be 30 to 1000 characters")
    private String about;

    @NotBlank(message = "Please describe your work experience")
    @Size(min = 30, max = 2000, message = "Work experience must be 30 to 2000 characters")
    private String experienceSummary;

    @Pattern(regexp = URL, message = "LinkedIn link must start with https://")
    private String linkedinUrl;

    @Pattern(regexp = URL, message = "Portfolio link must start with https://")
    private String portfolioUrl;

    /** No-arg constructor required by Jackson. */
    public InstructorProfileRequest() {}

    /** All-fields constructor; chains to the no-arg constructor. */
    public InstructorProfileRequest(String headline, Integer yearsOfExperience, String expertise, String qualification,
                                    String organization, String about, String experienceSummary,
                                    String linkedinUrl, String portfolioUrl) {
        this();
        this.headline = headline;
        this.yearsOfExperience = yearsOfExperience;
        this.expertise = expertise;
        this.qualification = qualification;
        this.organization = organization;
        this.about = about;
        this.experienceSummary = experienceSummary;
        this.linkedinUrl = linkedinUrl;
        this.portfolioUrl = portfolioUrl;
    }

    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }
    public Integer getYearsOfExperience() { return yearsOfExperience; }
    public void setYearsOfExperience(Integer yearsOfExperience) { this.yearsOfExperience = yearsOfExperience; }
    public String getExpertise() { return expertise; }
    public void setExpertise(String expertise) { this.expertise = expertise; }
    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }
    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }
    public String getAbout() { return about; }
    public void setAbout(String about) { this.about = about; }
    public String getExperienceSummary() { return experienceSummary; }
    public void setExperienceSummary(String experienceSummary) { this.experienceSummary = experienceSummary; }
    public String getLinkedinUrl() { return linkedinUrl; }
    public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }
    public String getPortfolioUrl() { return portfolioUrl; }
    public void setPortfolioUrl(String portfolioUrl) { this.portfolioUrl = portfolioUrl; }
}
