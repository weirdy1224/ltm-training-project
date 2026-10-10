import { Component, OnInit } from '@angular/core';
import { TicketService } from '../../services/ticket.service';
import { Ticket, categoryLabel } from '../../models/ticket.model';

/** Every ticket the logged-in user has raised, newest first. */
@Component({
  selector: 'app-my-tickets',
  templateUrl: './my-tickets.component.html',
  styleUrls: ['./my-tickets.component.css']
})
export class MyTicketsComponent implements OnInit {
  tickets: Ticket[] = [];
  loaded = false;
  errorMessage = '';
  readonly label = categoryLabel;

  constructor(private service: TicketService) {}

  ngOnInit(): void {
    this.service.mine().subscribe({
      next: list => { this.tickets = list; this.loaded = true; },
      error: err => { this.loaded = true; this.errorMessage = err?.error?.message || 'Could not load your tickets.'; }
    });
  }
}