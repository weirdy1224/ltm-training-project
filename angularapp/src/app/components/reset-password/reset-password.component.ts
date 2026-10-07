import { Component } from '@angular/core';

import {
  FormBuilder,
  FormGroup,
  Validators
} from '@angular/forms';

import {
  ActivatedRoute,
  Router
} from '@angular/router';

import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-reset-password',
  templateUrl: './reset-password.component.html',
  styleUrls: ['./reset-password.component.css']
})
export class ResetPasswordComponent {

  resetForm: FormGroup;

  token = '';

  loading = false;
  errorMessage = '';

  showPassword = false;
  showConfirmPassword = false;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private auth: AuthService
  ) {

    this.token =
      this.route.snapshot.queryParamMap.get('token') || '';

    this.resetForm = this.fb.group({

      password: [
        '',
        [
          Validators.required,
          Validators.pattern(
            /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s])\S{8,72}$/
          )
        ]
      ],

      confirmPassword: [
        '',
        Validators.required
      ]
    });
  }

  get password() {
    return this.resetForm.get('password');
  }

  get confirmPassword() {
    return this.resetForm.get('confirmPassword');
  }

  get passwordValue(): string {
    return this.password?.value || '';
  }

  get hasLength(): boolean {
    return this.passwordValue.length >= 8 &&
           this.passwordValue.length <= 72;
  }

  get hasUppercase(): boolean {
    return /[A-Z]/.test(this.passwordValue);
  }

  get hasLowercase(): boolean {
    return /[a-z]/.test(this.passwordValue);
  }

  get hasNumber(): boolean {
    return /\d/.test(this.passwordValue);
  }

  get hasSpecial(): boolean {
    return /[^A-Za-z0-9\s]/.test(this.passwordValue);
  }

  get hasNoSpaces(): boolean {
    return this.passwordValue.length > 0 &&
           !/\s/.test(this.passwordValue);
  }

  onSubmit(): void {

    this.errorMessage = '';

    if (!this.token) {
      this.errorMessage =
        'This password reset link is invalid.';
      return;
    }

    if (this.resetForm.invalid) {
      return;
    }

    if (
      this.password?.value !==
      this.confirmPassword?.value
    ) {

      this.errorMessage =
        'Passwords do not match.';

      return;
    }

    this.loading = true;

    this.auth
      .resetPassword(
        this.token,
        this.password?.value
      )
      .subscribe({

        next: () => {

          this.loading = false;

          this.router.navigate(
            ['/login'],
            {
              queryParams: {
                reset: 'success'
              }
            }
          );
        },

        error: (err) => {

          this.loading = false;

          this.errorMessage =
            err.error?.message ||
            'The password reset link is invalid or has expired.';
        }
      });
  }
}