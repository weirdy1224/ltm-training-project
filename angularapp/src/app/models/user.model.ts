import { InstructorProfileForm } from './instructor-profile.model';

export interface User {
  userId?: number;
  email?: string;
  password?: string;
  username?: string;
  mobileNumber?: string;
  role?: string;
  /** Set by the server: false until the emailed OTP is entered. */
  emailVerified?: boolean;
  /** Set by the server: an instructor must be approved by an admin before logging in. */
  approved?: boolean;
  /** Only sent when registering as an INSTRUCTOR. */
  instructorProfile?: InstructorProfileForm;
}
