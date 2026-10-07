import { Component } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  Validators
} from '@angular/forms';

import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-forgot-password',
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.css']
})
export class ForgotPasswordComponent {

  forgotForm: FormGroup;

  loading = false;
  submitted = false;
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private auth: AuthService
  ) {

    this.forgotForm = this.fb.group({
      email: [
        '',
        [
          Validators.required,
          Validators.email
        ]
      ]
    });
  }

  get email() {
    return this.forgotForm.get('email');
  }

  onSubmit(): void {

    if (this.forgotForm.invalid || this.loading) {
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.auth
      .forgotPassword(this.email?.value)
      .subscribe({

        next: () => {
          this.loading = false;
          this.submitted = true;
        },

        error: () => {

          this.loading = false;

          this.errorMessage =
            'Unable to send the reset link. Please try again later.';
        }
      });
  }
}