/** The details an instructor enters while registering (sent inside the register request as "instructorProfile"). */
export interface InstructorProfileForm {
    headline?: string;
    yearsOfExperience?: number;
    expertise?: string;
    qualification?: string;
    organization?: string;
    about?: string;
    experienceSummary?: string;
    linkedinUrl?: string;
    portfolioUrl?: string;
  }
  
  /** One row of the admin's instructor list (GET /api/admin/instructors): account state plus the registration details. */
  export interface InstructorApplication extends InstructorProfileForm {
    userId: number;
    username: string;
    email: string;
    mobileNumber: string;
    emailVerified: boolean;
    approved: boolean;
    hasProfile: boolean;
    submittedAt?: string;
  }
  
  /** Answer of POST /api/instructor-application/status: only the step, a message and the date (no personal data). */
  export interface ApplicationStatus {
    status: 'NOT_FOUND' | 'EMAIL_NOT_VERIFIED' | 'UNDER_REVIEW' | 'APPROVED';
    message: string;
    submittedAt?: string;
  }
  