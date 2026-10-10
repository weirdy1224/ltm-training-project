/**
 * Notification types enum and data model for the notification system.
 */
export enum NotificationType {
  COURSE_PURCHASE = 'COURSE_PURCHASE',
  NEW_STUDENT_ENROLLMENT = 'NEW_STUDENT_ENROLLMENT',
  VIDEO_UPLOAD = 'VIDEO_UPLOAD',
  INSTRUCTOR_APPROVAL = 'INSTRUCTOR_APPROVAL',
  INSTRUCTOR_REJECTION = 'INSTRUCTOR_REJECTION',
  TICKET_CREATED = 'TICKET_CREATED',
  TICKET_RAISED = 'TICKET_RAISED',
  TICKET_RESPONSE = 'TICKET_RESPONSE',
  TICKET_RESOLVED = 'TICKET_RESOLVED'
}

export interface AppNotification {
  id: number;
  userId: number;
  title: string;
  message: string;
  type: NotificationType;
  read: boolean;
  redirectUrl?: string;
  ticketId?: number;
  createdAt: string;
}