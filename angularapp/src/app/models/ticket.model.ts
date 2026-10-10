export type TicketStatus = 'OPEN' | 'PENDING' | 'CLOSED' | 'RESOLVED';

export type TicketCategory =
  | 'LOGIN_ACCOUNT' | 'PAYMENT_BILLING' | 'COURSE_ACCESS' | 'VIDEO_PLAYBACK'
  | 'INSTRUCTOR_APPLICATION' | 'TECHNICAL_BUG' | 'OTHER';

/** The problem types offered in the "raise a ticket" dropdown. */
export const TICKET_CATEGORIES: { value: TicketCategory; label: string }[] = [
  { value: 'LOGIN_ACCOUNT', label: 'Login or account' },
  { value: 'PAYMENT_BILLING', label: 'Payment or billing' },
  { value: 'COURSE_ACCESS', label: 'Course access' },
  { value: 'VIDEO_PLAYBACK', label: 'Video playback' },
  { value: 'INSTRUCTOR_APPLICATION', label: 'Instructor application' },
  { value: 'TECHNICAL_BUG', label: 'Technical bug' },
  { value: 'OTHER', label: 'Something else' }
];

export function categoryLabel(value: string): string {
  return TICKET_CATEGORIES.find(c => c.value === value)?.label ?? value;
}

export interface Ticket {
  id: number;
  ticketCode: string;
  userId: number;
  username: string | null;
  email: string | null;
  role: string | null;
  category: TicketCategory;
  description: string;
  screenshotUrl: string | null;
  status: TicketStatus;
  adminResponse: string | null;
  createdAt: string;
  respondedAt: string | null;
  feedbackRating: number | null;
  feedbackUsefulness: number | null;
  feedbackComment: string | null;
}

/** One container on the admin's ticket page: a user and how many tickets they have in each state. */
export interface UserTicketSummary {
  userId: number;
  username: string;
  email: string | null;
  role: string | null;
  total: number;
  open: number;
  pending: number;
  resolved: number;
  closed: number;
  lastTicketAt: string;
}

export interface TicketFeedback {
  rating: number;
  usefulness: number;
  comment: string;
}