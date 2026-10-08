export interface Course {
  courseId?: number;
  courseType?: string;
  courseImageUrl?: string;
  courseDetails?: string;
  coursePrice?: number;
  /** Read-only, set by the server for courses created by an instructor. */
  instructorId?: number;
  instructorName?: string;
}