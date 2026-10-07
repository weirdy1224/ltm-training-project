/** One video lesson of a course (the server never sends the file path). */
export interface CourseVideo {
    videoId?: number;
    courseId?: number;
    title?: string;
    description?: string;
    orderIndex?: number;
    originalFileName?: string;
    contentType?: string;
    sizeBytes?: number;
    uploadedAt?: string;
    /** True when the logged-in user may play it (enrolled customer, owner instructor or admin). */
    canWatch?: boolean;
  }
  