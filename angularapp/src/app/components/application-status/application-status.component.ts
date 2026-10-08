import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { ApplicationStatus } from '../../models/instructor-profile.model';
import { AuthService } from '../../services/auth.service';

/**
 * Public page (no login): an instructor types the email used to apply and sees how far the application has come:
 * submitted, email verified, under review by an admin, approved.
 *
 * @author Lokesh
 */
@Component({
  selector: 'app-application-status',
  templateUrl: './application-status.component.html',
  styleUrls: ['./application-status.component.css']
})
export class ApplicationStatusComponent implements OnInit {
  form: FormGroup;
  loading = false;
  errorMessage = '';
  result?: ApplicationStatus;
  /** The email the result belongs to (the box may be edited afterwards). */
  checkedEmail = '';

  constructor(private fb: FormBuilder, private auth: AuthService, private route: ActivatedRoute) {
    this.form = this.fb.group({ email: ['', [Validators.required, Validators.email]] });
  }

  get f() { return this.form.controls; }

  ngOnInit(): void {
    const email = this.route.snapshot.queryParamMap.get('email');
    if (email) {
      this.f['email'].setValue(email);
      this.check();
    }
  }

  /** The four steps of the progress bar; "done" steps are filled, "current" is the one waiting. */
  get steps(): { label: string; state: 'done' | 'current' | 'todo' }[] {
    const order = ['EMAIL_NOT_VERIFIED', 'UNDER_REVIEW', 'APPROVED'];
    const at = this.result ? order.indexOf(this.result.status) : -1;
    const state = (i: number): 'done' | 'current' | 'todo' => (this.result?.status === 'APPROVED' ? 'done' : i < at + 1 ? (i === at ? 'current' : 'done') : 'todo');
    return [
      { label: 'Application submitted', state: at >= 0 ? 'done' : 'todo' },
      { label: 'Email verified', state: at >= 1 ? 'done' : at === 0 ? 'current' : 'todo' },
      { label: 'Admin review', state: at >= 2 ? 'done' : at === 1 ? 'current' : 'todo' },
      { label: 'Approved: you can log in', state: at >= 2 ? 'done' : 'todo' }
    ];
  }

  check(): void {
    if (this.form.invalid || this.loading) { return; }
    this.loading = true;
    this.errorMessage = '';
    this.result = undefined;
    const email = String(this.f['email'].value).trim();
    this.auth.applicationStatus(email).subscribe({
      next: (res) => { this.loading = false; this.result = res; this.checkedEmail = email; },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.status === 429
          ? 'Too many checks. Please wait a minute and try again.'
          : (err.error?.message || 'Could not check the application. Please try again.');
      }
    });
  }
}
