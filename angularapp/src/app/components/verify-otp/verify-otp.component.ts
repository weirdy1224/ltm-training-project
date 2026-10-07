import { Component, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

/**
 * Step 2 of registration: the user types the 6-digit OTP that the server emailed. The page can also be opened from the
 * login page (a correct password with an unverified email). "Resend" asks for a new OTP; the server allows one per minute,
 * so the button counts down.
 *
 * @author Lokesh
 */
@Component({
  selector: 'app-verify-otp',
  templateUrl: './verify-otp.component.html',
  styleUrls: ['./verify-otp.component.css']
})
export class VerifyOtpComponent implements OnInit, OnDestroy {
  form: FormGroup;
  loading = false;
  resending = false;
  errorMessage = '';
  infoMessage = '';
  /** Filled after a successful verification. */
  doneMessage = '';
  cooldown = 0;
  /** True when the user came from the instructor application page. */
  isInstructor = false;
  email = '';
  private timer?: ReturnType<typeof setInterval>;

  constructor(private fb: FormBuilder, private auth: AuthService, private route: ActivatedRoute, private router: Router) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      otp: ['', [Validators.required, Validators.pattern(/^[0-9]{6}$/)]]
    });
  }

  get f() { return this.form.controls; }

  ngOnInit(): void {
    const q = this.route.snapshot.queryParamMap;
    const email = q.get('email') || '';
    this.isInstructor = q.get('type') === 'instructor';
    this.email = email;
    this.f['email'].setValue(email);
    if (email) { this.f['email'].disable(); }   // the address came from the previous step; it cannot be changed here
    if (q.get('mail') === 'failed') {
      this.errorMessage = 'Your account was created, but we could not send the email. Press "Resend OTP".';
    } else if (q.get('reason') === 'login') {
      this.infoMessage = 'Please verify your email first. We have the OTP ready: check your inbox, or press "Resend OTP".';
    } else if (email) {
      this.infoMessage = `We sent a 6-digit code to ${email}. It is valid for 10 minutes.`;
      this.startCooldown(60);
    }
  }

  ngOnDestroy(): void {
    if (this.timer) { clearInterval(this.timer); }
  }

  /** Keeps only digits, so pasted text like "123 456" still works. */
  onOtpInput(): void {
    const digits = String(this.f['otp'].value || '').replace(/\D/g, '').slice(0, 6);
    if (digits !== this.f['otp'].value) { this.f['otp'].setValue(digits); }
  }

  verify(): void {
    if (this.form.invalid || this.loading) { return; }
    this.loading = true;
    this.errorMessage = '';
    const { email, otp } = this.form.getRawValue();
    this.auth.verifyOtp(email, otp).subscribe({
      next: (res) => { this.loading = false; this.doneMessage = res.message; },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.status === 429
          ? 'Too many attempts. Please wait a minute and try again.'
          : (err.error?.message || 'Could not verify the OTP. Please try again.');
      }
    });
  }

  resend(): void {
    const email = this.form.getRawValue().email;
    if (!email || this.f['email'].invalid || this.resending || this.cooldown > 0) { return; }
    this.resending = true;
    this.errorMessage = '';
    this.auth.resendOtp(email).subscribe({
      next: () => {
        this.resending = false;
        this.infoMessage = 'If this email is waiting for verification, a new OTP has been sent.';
        this.startCooldown(60);
      },
      error: (err) => {
        this.resending = false;
        if (err.status === 429) {
          const wait = Number(err.headers?.get('Retry-After')) || 60;
          this.errorMessage = `Please wait ${wait} seconds before asking for another OTP.`;
          this.startCooldown(wait);
        } else if (err.status === 502) {
          this.errorMessage = 'We could not send the email right now. Please try again later.';
        } else {
          this.errorMessage = err.error?.message || 'Could not send a new OTP.';
        }
      }
    });
  }

  private startCooldown(seconds: number): void {
    if (this.timer) { clearInterval(this.timer); }
    this.cooldown = seconds;
    this.timer = setInterval(() => {
      this.cooldown--;
      if (this.cooldown <= 0 && this.timer) { clearInterval(this.timer); }
    }, 1000);
  }

  /** After an instructor verified the email, the next step is waiting for the admin: the status page shows it. */
  goToStatus(): void {
    this.router.navigate(['/application-status'], { queryParams: { email: this.email || this.form.getRawValue().email } });
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}
