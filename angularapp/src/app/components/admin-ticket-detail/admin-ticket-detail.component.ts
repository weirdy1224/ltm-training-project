import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TicketService } from '../../services/ticket.service';
import { Ticket, categoryLabel } from '../../models/ticket.model';

/**
 * The admin's view of one ticket. Opening an OPEN ticket makes it PENDING. The admin can type a response (submit =
 * RESOLVED) or leave the box empty (submit = CLOSED). A ticket gets one response only.
 */
@Component({
  selector: 'app-admin-ticket-detail',
  templateUrl: './admin-ticket-detail.component.html',
  styleUrls: ['./admin-ticket-detail.component.css']
})
export class AdminTicketDetailComponent implements OnInit {
  ticket?: Ticket;
  response = '';
  loading = true;
  saving = false;
  errorMessage = '';
  readonly label = categoryLabel;

  constructor(private route: ActivatedRoute, private service: TicketService, private snack: MatSnackBar) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(p => this.load(Number(p.get('id'))));
  }

  private load(id: number): void {
    this.loading = true;
    this.errorMessage = '';
    this.service.adminGet(id).subscribe({
      next: t => { this.ticket = t; this.loading = false; },
      error: err => { this.loading = false; this.errorMessage = err?.error?.message || 'Could not load this ticket.'; }
    });
  }

  get answered(): boolean {
    return !!this.ticket && (this.ticket.status === 'RESOLVED' || this.ticket.status === 'CLOSED');
  }

  submit(): void {
    if (!this.ticket || this.saving) {
      return;
    }
    this.saving = true;
    this.errorMessage = '';
    this.service.adminRespond(this.ticket.id, this.response.trim()).subscribe({
      next: t => {
        this.ticket = t;
        this.saving = false;
        this.snack.open(t.status === 'RESOLVED' ? 'Ticket resolved' : 'Ticket closed', 'OK', { duration: 4000 });
      },
      error: err => { this.saving = false; this.errorMessage = err?.error?.message || 'Could not save the response.'; }
    });
  }
}