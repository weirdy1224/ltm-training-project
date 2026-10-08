import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API,API_URL } from '../constant';
import { User } from '../models/user.model';
import { InstructorApplication } from '../models/instructor-profile.model';

/**
 * Admin account management (list, add, remove admins; approve or reject instructors). The token is added by the AuthInterceptor.
 *
 * @author Suriya
 */
@Injectable({
  providedIn: 'root'
})
export class AdminService {
  public apiUrl = API_URL;

  constructor(private http: HttpClient) {}

  /** Tells whether the logged-in admin is the main admin (the only one who may add or remove admins). */
  whoAmI(): Observable<{ email: string; mainAdmin: boolean }> {
    return this.http.get<{ email: string; mainAdmin: boolean }>(`${this.apiUrl}${API.ADMIN_USERS}/me`);
  }

  listAdmins(): Observable<User[]> {
    return this.http.get<User[]>(`${this.apiUrl}${API.ADMIN_USERS}`);
  }

  createAdmin(admin: User): Observable<User> {
    return this.http.post<User>(`${this.apiUrl}${API.ADMIN_USERS}`, admin);
  }

  deleteAdmin(userId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}${API.ADMIN_USERS}/${userId}`);
  }

  /** Every instructor account with its state (emailVerified / approved) and the details entered while registering. */
  listInstructors(): Observable<InstructorApplication[]> {
    return this.http.get<InstructorApplication[]>(`${this.apiUrl}${API.ADMIN_INSTRUCTORS}`);
  }

  approveInstructor(userId: number): Observable<User> {
    return this.http.put<User>(`${this.apiUrl}${API.ADMIN_INSTRUCTORS}/${userId}/approve`, {});
  }

  /** Removes an instructor who has not been approved yet. */
  rejectInstructor(userId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}${API.ADMIN_INSTRUCTORS}/${userId}`);
  }
}
