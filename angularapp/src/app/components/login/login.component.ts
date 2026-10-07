import { AfterViewInit, Component, NgZone, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CustomerService } from '../../services/customer.service';
import { environment } from 'src/environments/environment.prod';
import { ROLES } from '../../constant';

/**
 * Email + password login and "Sign in with Google". After a successful login the user goes to the page of their role:
 * admin -> dashboard, instructor -> instructor dashboard, customer -> dashboard (or the "fill your details" page when the
 * customer profile does not exist yet). A 403 from the server explains why a correct password is not enough yet:
 * email not verified (we send the user to the OTP page) or instructor not approved.
 *
 * @author Suriya
 */
@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements AfterViewInit, OnDestroy {
  loginForm: FormGroup;
  errorMessage = '';
  loading = false;
  showPassword = false;
  private googleTimer?: ReturnType<typeof setTimeout>;

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private customerService: CustomerService,
    private router: Router,
    private ngZone: NgZone
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]]
    });
  }

  ngAfterViewInit(): void {
    this.initGoogleButton(0);
  }

  ngOnDestroy(): void {
    if (this.googleTimer) { clearTimeout(this.googleTimer); }
  }

  /**
   * The Google script in index.html loads with async/defer, so it may not be ready when this page opens.
   * Instead of giving up (the old code never showed the button on a fast first load) we try again for a few seconds.
   */
  private initGoogleButton(attempt: number): void {
    const google = (window as any).google;
    if (!google?.accounts?.id) {
      if (attempt < 20) {
        this.googleTimer = setTimeout(() => this.initGoogleButton(attempt + 1), 250);
      }
      return;
    }
    google.accounts.id.initialize({
      client_id: environment.googleClientId,
      callback: (response: any) => this.handleGoogleLogin(response)
    });
    google.accounts.id.renderButton(
      document.getElementById('googleButton'),
      { theme: 'outline', size: 'large', width: 320 }
    );
  }

  get email() { return this.loginForm.get('email'); }
  get password() { return this.loginForm.get('password'); }

  onSubmit(): void {
    if (this.loginForm.invalid || this.loading) {
      return;
    }
    this.loading = true;
    this.errorMessage = '';

    this.auth.login(this.loginForm.value).subscribe({
      next: (res: any) => {
        this.loading = false;
        this.goToHome(res.role);
      },
      error: (err) => {
        this.loading = false;
        if (err.status === 401) {
          this.errorMessage = 'Invalid email or password';
        } else if (err.status === 403 && err.error?.code === 'EMAIL_NOT_VERIFIED') {
          // right password, but the OTP step was never finished: take the user there
          this.ngZone.run(() => this.router.navigate(['/verify-otp'], { queryParams: { email: this.email?.value, reason: 'login' } }));
        } else if (err.status === 403) {
          this.errorMessage = err.error?.message || 'This account cannot log in yet.';
        } else if (err.status === 429) {
          this.errorMessage = 'Too many attempts. Please wait a while and try again.';
        } else {
          this.errorMessage = 'Unable to reach the server. Please try again later.';
        }
      }
    });
  }

  handleGoogleLogin(response: any): void {
    this.auth.googleLogin(response.credential).subscribe({
      next: (res: any) => this.goToHome(res.role),
      error: (err) => {
        this.ngZone.run(() => {
          this.errorMessage = err.status === 403
            ? (err.error?.message || 'This account cannot log in yet.')
            : 'Google sign in failed';
        });
      }
    });
  }

  /** Sends the user to the first page of their role (used by both login methods). */
  private goToHome(role: string): void {
    if (role === ROLES.ADMIN) {
      this.ngZone.run(() => this.router.navigate(['/dashboard']));
      return;
    }
    if (role === ROLES.INSTRUCTOR) {
      this.ngZone.run(() => this.router.navigate(['/instructor/dashboard']));
      return;
    }
    this.customerService.viewCustomerByUserId().subscribe({
      next: () => this.ngZone.run(() => this.router.navigate(['/dashboard'])),
      error: (err) => {
        if (err.status === 404) {
          // no customer profile yet: the "Fill Your Details" page comes first
          this.ngZone.run(() => this.router.navigate(['/customerdashboard']));
          return;
        }
        this.ngZone.run(() => (this.errorMessage = 'Unable to load profile'));
      }
    });
  }
}
