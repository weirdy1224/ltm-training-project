import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../../services/auth.service';
import { TicketService } from '../../services/ticket.service';
import { Ticket, TICKET_CATEGORIES } from '../../models/ticket.model';

const MAX_IMAGE_BYTES = 5 * 1024 * 1024;
const IMAGE_TYPES = ['image/png', 'image/jpeg', 'image/gif', 'image/webp'];

/**
 * Help page for customers and instructors. Common issues are listed first (click one to read the usual fix); when none
 * of them matches, the user raises a ticket: own user id, a category, a description and an optional screenshot.
 */
@Component({
  selector: 'app-support',
  templateUrl: './support.component.html',
  styleUrls: ['./support.component.css']
})
export class SupportComponent {
  readonly categories = TICKET_CATEGORIES;
  readonly faqs = [
    { q: 'I forgot my password', a: 'Open the login page and choose "Forgot password". Enter your email, type the OTP we send you and set a new password. If the email does not arrive within a few minutes, check your spam folder and request a new OTP.' },
    { q: 'My payment was deducted but the course is not in My Enrollments', a: 'Wait a few minutes and refresh My Enrollments, because confirmation can take a short while. If the course is still missing after 30 minutes, raise a ticket with the payment category and mention the time of the payment.' },
    { q: 'A video does not play or keeps buffering', a: 'Check your internet connection, refresh the page and try another browser. Disable ad blockers or VPNs for this site. If only one lesson fails, raise a ticket with the course name and lesson title.' },
    { q: 'I cannot log in even though my password is correct', a: 'Too many wrong attempts lock the account for 15 minutes; wait and try again. Make sure you verified your email with the OTP after registering. Instructors can also log in only after an admin approves the application.' },
    { q: 'I did not receive the OTP email', a: 'Look in the spam or promotions folder, make sure the email address is spelled correctly and ask for a new OTP after one minute. A new OTP makes the earlier one invalid.' },
    { q: 'How do I download my invoice?', a: 'Open My Enrollments, choose the order and use the invoice option on the order details page.' }
  ];
  openIndex: number | null = null;

  form: FormGroup;
  screenshot: File | null = null;
  previewUrl: string | null = null;
  fileError = '';
  submitting = false;
  errorMessage = '';
  created?: Ticket;

  constructor(private fb: FormBuilder, private auth: AuthService, private tickets: TicketService, private snack: MatSnackBar) {
    this.form = this.fb.group({
      userId: [{ value: this.auth.getUserId() ?? '', disabled: true }],
      category: ['', Validators.required],
      description: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(2000)]]
    });
  }

  get f() { return this.form.controls; }

  toggle(i: number): void {
    this.openIndex = this.openIndex === i ? null : i;
  }

  onFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files && input.files.length ? input.files[0] : null;
    this.fileError = '';
    if (!file) {
      this.clearFile(input);
      return;
    }
    if (!IMAGE_TYPES.includes(file.type)) {
      this.fileError = 'Only PNG, JPG, GIF or WEBP images are allowed';
      this.clearFile(input);
      return;
    }
    if (file.size > MAX_IMAGE_BYTES) {
      this.fileError = 'The image is too large. The limit is 5 MB';
      this.clearFile(input);
      return;
    }
    this.clearFile(input, false);
    this.screenshot = file;
    this.previewUrl = URL.createObjectURL(file);
  }

  removeFile(input: HTMLInputElement): void {
    this.fileError = '';
    this.clearFile(input);
  }

  private clearFile(input: HTMLInputElement, resetInput = true): void {
    if (this.previewUrl) {
      URL.revokeObjectURL(this.previewUrl);
    }
    this.screenshot = null;
    this.previewUrl = null;
    if (resetInput) {
      input.value = '';
    }
  }

  submit(fileInput: HTMLInputElement): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    this.errorMessage = '';
    const v = this.form.getRawValue();
    this.tickets.create(Number(v.userId), v.category, v.description.trim(), this.screenshot).subscribe({
      next: t => {
        this.submitting = false;
        this.created = t;
        this.form.reset({ userId: v.userId, category: '', description: '' });
        this.clearFile(fileInput);
        this.snack.open(`Ticket ${t.ticketCode} raised`, 'OK', { duration: 5000 });
      },
      error: err => {
        this.submitting = false;
        this.errorMessage = err?.error?.message || 'Could not raise the ticket. Please try again.';
      }
    });
  }
}