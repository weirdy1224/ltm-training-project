import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API, API_URL } from '../constant';
import { InstructorStats } from '../models/stats.model';

/**
 * Numbers for the instructor dashboard. The token is added by the AuthInterceptor.
 *
 * @author Lokesh
 */
@Injectable({
  providedIn: 'root'
})
export class InstructorService {
  public apiUrl = API_URL;

  constructor(private http: HttpClient) {}

  getStats(): Observable<InstructorStats> {
    return this.http.get<InstructorStats>(`${this.apiUrl}${API.DASHBOARD_INSTRUCTOR}`);
  }
}
