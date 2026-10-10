import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { TicketService } from '../../services/ticket.service';
import { Ticket, UserTicketSummary, categoryLabel } from '../../models/ticket.model';

/**
 * Admin ticket list in two levels (same component, two routes):
 *  - /admin/tickets             one container per user who raised tickets, with counts per status
 *  - /admin/tickets/user/:id    all tickets of that one user
 */
@Component({
  selector: 'app-admin-tickets',
  templateUrl: './admin-tickets.component.html',
  styleUrls: ['./admin-tickets.component.css']
})
export class AdminTicketsComponent implements OnInit {
  users: UserTicketSummary[] = [];
  tickets: Ticket[] = [];
  userId: number | null = null;
  username = '';
  loaded = false;
  errorMessage = '';
  readonly label = categoryLabel;

  constructor(private route: ActivatedRoute, private service: TicketService) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(p => {
      const id = p.get('userId');
      this.userId = id ? Number(id) : null;
      this.load();
    });
  }

  private load(): void {
    this.loaded = false;
    this.errorMessage = '';
    const fail = (err: any) => { this.loaded = true; this.errorMessage = err?.error?.message || 'Could not load the tickets.'; };
    if (this.userId === null) {
      this.service.adminUsers().subscribe({ next: u => { this.users = u; this.loaded = true; }, error: fail });
    } else {
      this.service.adminTicketsOf(this.userId).subscribe({
        next: t => { this.tickets = t; this.username = t.length ? (t[0].username ?? 'User ' + this.userId) : 'User ' + this.userId; this.loaded = true; },
        error: fail
      });
    }
  }
}