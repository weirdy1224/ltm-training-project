import { HttpClient } from '@angular/common/http';
import { ApplicationStatus } from '../models/instructor-profile.model';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, finalize, map, shareReplay, tap } from 'rxjs/operators';
import { API, API_URL, STORAGE } from '../constant';
import { Login } from '../models/login.model';
import { User } from '../models/user.model';
import { UserStoreService } from './user-store.service';

/**
 * Register, login, logout and session handling.
 *  - The JWT is saved in localStorage (as the SRS requires) and decoded by UserStoreService with jwt-decode.
 *  - The AuthInterceptor is the only place that puts the token into request headers.
 *  - A refresh cookie (HttpOnly, set by the server) lets /api/refresh swap an expired token for a new one.
 *
 * @author Suriya
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  public apiUrl = API_URL;

  /** Role of the logged-in user (null when logged out). Used by the navbar. */
  public role$ = this.userStore.user$.pipe(map(u => u?.role ?? null));
  public userId$ = this.userStore.user$.pipe(map(u => (u?.userId !== undefined ? String(u.userId) : null)));

  private refreshInFlight$: Observable<any> | null = null;

  constructor(private http: HttpClient, private userStore: UserStoreService) {
    this.userStore.restore();   // after a page reload, bring the user back from the saved token
  }

  googleLogin(credential: string): Observable<any> {
    return this.http.post<any>(
      `${this.apiUrl}${API.GOOGLE_LOGIN}`,
      { credential },
      { withCredentials: true }
    ).pipe(
      tap((res: any) => this.applySession(res))
    );
  }

  /** Public sign-up as a CUSTOMER or an INSTRUCTOR. The server emails a 6-digit OTP; the account works after verifyOtp. */
  register(user: User): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}${API.REGISTER}`, user);
  }

  /** Confirms the email address with the 6 digits from the email. */
  verifyOtp(email: string, otp: string): Observable<{ message: string; role: string; approved: boolean }> {
    return this.http.post<{ message: string; role: string; approved: boolean }>(`${this.apiUrl}${API.VERIFY_OTP}`, { email, otp });
  }

  /** Public: where is the instructor application of this email? (no login needed) */
  applicationStatus(email: string): Observable<ApplicationStatus> {
    return this.http.post<ApplicationStatus>(`${this.apiUrl}${API.INSTRUCTOR_APPLICATION_STATUS}`, { email });
  }

  /** Asks for a new OTP (the server waits about a minute between two mails). */
  resendOtp(email: string): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}${API.RESEND_OTP}`, { email });
  }

  login(login: Login): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}${API.LOGIN}`, login, { withCredentials: true }).pipe(
      tap((res: any) => this.applySession(res))
    );
  }

  forgotPassword(email: string): Observable<any> {
    return this.http.post<any>(
      `${this.apiUrl}${API.FORGOT_PASSWORD}`,
      { email }
    );
  }

  resetPassword(
    token: string,
    password: string
  ): Observable<any> {
  
    return this.http.post<any>(
      `${this.apiUrl}${API.RESET_PASSWORD}`,
      {
        token,
        password
      }
    );
  }

  /** Gets a new token using the refresh cookie. Concurrent callers share one request. */
  refresh(): Observable<any> {
    if (!this.refreshInFlight$) {
      this.refreshInFlight$ = this.http.post<any>(`${this.apiUrl}${API.REFRESH}`, {}, { withCredentials: true }).pipe(
        tap((res: any) => this.applySession(res)),
        finalize(() => (this.refreshInFlight$ = null)),
        shareReplay(1)
      );
    }
    return this.refreshInFlight$;
  }

  /** After a page reload: true when the saved token is still valid, or the refresh cookie gives a new one. */
  restoreSession(): Observable<boolean> {
    if (this.isLoggedIn()) {
      return of(true);
    }
    if (!this.hasSessionFlag()) {
      return of(false);
    }
    return this.refresh().pipe(
      map(() => true),
      catchError(() => {
        this.clearSession();
        return of(false);
      })
    );
  }

  logout(): void {
    this.http.post(`${this.apiUrl}${API.LOGOUT}`, {}, { withCredentials: true }).subscribe({ error: () => {} });
    this.clearSession();
  }

  clearSession(): void {
    [STORAGE.TOKEN, STORAGE.ROLE, STORAGE.USER_ID, STORAGE.CUSTOMER_ID, STORAGE.CART_ID, STORAGE.USERNAME, STORAGE.SESSION]
      .forEach(k => localStorage.removeItem(k));
    this.userStore.clear();
  }

  private applySession(res: any): void {
    localStorage.setItem(STORAGE.USERNAME, res.username);
    localStorage.setItem(STORAGE.TOKEN, res.token);
    localStorage.setItem(STORAGE.ROLE, res.role);
    localStorage.setItem(STORAGE.USER_ID, String(res.userId));
    localStorage.setItem(STORAGE.SESSION, '1');   // only a flag: a refresh cookie probably exists
    const user = this.userStore.setFromToken(res.token);
    if (!user) {
      // the token could not be decoded: fall back to what the login response says
      this.userStore.setUser({ userId: res.userId, username: res.username, email: res.email, role: res.role });
    }
  }

  hasSessionFlag(): boolean {
    return localStorage.getItem(STORAGE.SESSION) === '1';
  }

  isLoggedIn(): boolean {
    return !!this.getToken() && !!this.userStore.user && this.userStore.isTokenValid();
  }

  getToken(): string | null {
    return localStorage.getItem(STORAGE.TOKEN);
  }

  getRole(): string | null {
    return this.userStore.role || localStorage.getItem(STORAGE.ROLE);
  }

  getUserId(): string | null {
    return this.userStore.userId || localStorage.getItem(STORAGE.USER_ID);
  }
}