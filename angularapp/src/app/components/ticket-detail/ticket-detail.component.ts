import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TicketService } from '../../services/ticket.service';
import { Ticket, categoryLabel } from '../../models/ticket.model';

/**
 * One of the user's tickets: its status, the original description and screenshot, the admin's response and, once the
 * ticket is RESOLVED or CLOSED, a feedback form (shown until the user has sent feedback once).
 */
@Component({
  selector: 'app-ticket-detail',
  templateUrl: './ticket-detail.component.html',
  styleUrls: ['./ticket-detail.component.css']
})
export class TicketDetailComponent implements OnInit {
  ticket?: Ticket;
  loading = true;
  errorMessage = '';
  sending = false;
  feedbackError = '';
  form: FormGroup;
  readonly stars = [1, 2, 3, 4, 5];
  readonly label = categoryLabel;

  constructor(private route: ActivatedRoute, private service: TicketService, private fb: FormBuilder, private snack: MatSnackBar) {
    this.form = this.fb.group({
      rating: [0, [Validators.required, Validators.min(1)]],
      usefulness: [0, [Validators.required, Validators.min(1)]],
      comment: ['', Validators.maxLength(1000)]
    });
  }

  ngOnInit(): void {
    this.route.paramMap.subscribe(p => this.load(Number(p.get('id'))));
  }

  private load(id: number): void {
    this.loading = true;
    this.errorMessage = '';
    this.service.getMine(id).subscribe({
      next: t => { this.ticket = t; this.loading = false; },
      error: err => { this.loading = false; this.errorMessage = err?.error?.message || 'Could not load this ticket.'; }
    });
  }

  get canGiveFeedback(): boolean {
    return !!this.ticket && (this.ticket.status === 'RESOLVED' || this.ticket.status === 'CLOSED') && this.ticket.feedbackRating == null;
  }

  setStars(control: 'rating' | 'usefulness', value: number): void {
    this.form.get(control)?.setValue(value);
  }

  sendFeedback(): void {
    if (!this.ticket || this.form.invalid || this.sending) {
      return;
    }
    this.sending = true;
    this.feedbackError = '';
    this.service.sendFeedback(this.ticket.id, this.form.getRawValue()).subscribe({
      next: t => { this.ticket = t; this.sending = false; this.snack.open('Thank you for your feedback', 'OK', { duration: 4000 }); },
      error: err => { this.sending = false; this.feedbackError = err?.error?.message || 'Could not send your feedback.'; }
    });
  }
}