import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API, API_URL } from '../constant';
import { Ticket, TicketFeedback, UserTicketSummary } from '../models/ticket.model';

/** Support tickets: the user side (/api/tickets) and the admin side (/api/admin/tickets). The token is added by the AuthInterceptor. */
@Injectable({
  providedIn: 'root'
})
export class TicketService {
  private user = `${API_URL}${API.TICKETS}`;
  private admin = `${API_URL}${API.ADMIN_TICKETS}`;

  constructor(private http: HttpClient) {}

  // user side
  create(userId: number, category: string, description: string, screenshot: File | null): Observable<Ticket> {
    const form = new FormData();
    form.append('userId', String(userId));
    form.append('category', category);
    form.append('description', description);
    if (screenshot) {
      form.append('screenshot', screenshot);
    }
    return this.http.post<Ticket>(this.user, form);
  }

  mine(): Observable<Ticket[]> {
    return this.http.get<Ticket[]>(this.user);
  }

  getMine(id: number): Observable<Ticket> {
    return this.http.get<Ticket>(`${this.user}/${id}`);
  }

  sendFeedback(id: number, feedback: TicketFeedback): Observable<Ticket> {
    return this.http.post<Ticket>(`${this.user}/${id}/feedback`, feedback);
  }

  // admin side
  adminUsers(): Observable<UserTicketSummary[]> {
    return this.http.get<UserTicketSummary[]>(`${this.admin}/users`);
  }

  adminTicketsOf(userId: number): Observable<Ticket[]> {
    return this.http.get<Ticket[]>(`${this.admin}/user/${userId}`);
  }

  adminGet(id: number): Observable<Ticket> {
    return this.http.get<Ticket>(`${this.admin}/${id}`);
  }

  adminRespond(id: number, response: string): Observable<Ticket> {
    return this.http.post<Ticket>(`${this.admin}/${id}/respond`, { response });
  }
}