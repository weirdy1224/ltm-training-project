import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CustomerService } from '../../services/customer.service';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {

  loginForm: FormGroup;
  errorMessage = '';
  loading = false;
  showPassword = false;

  constructor(
    private fb: FormBuilder,
    private auth: AuthService,
    private customerService: CustomerService,
    private router: Router
  ) {
    this.loginForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]]
    });
  }

  get email() {
    return this.loginForm.get('email');
  }

  get password() {
    return this.loginForm.get('password');
  }

  onSubmit(): void {

    if (this.loginForm.invalid || this.loading) {
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.auth.login(this.loginForm.value).subscribe({
      next: (res: any) => {

        if (res.role === 'ADMIN') {
          this.loading = false;
          this.router.navigate(['/dashboard']);
          return;
        }

        // Customers must have completed their profile first
        this.customerService.viewCustomerByUserId().subscribe({
          next: () => {
            this.loading = false;
            this.router.navigate(['/dashboard']);
          },
          error: (err) => {
            this.loading = false;

            if (err.status === 404) {
              this.router.navigate(['/customerdashboard']);
            } else {
              this.errorMessage =
                'Could not load your profile. Please try again.';
            }
          }
        });
      },

      error: (err) => {
        this.loading = false;

        if (err.status === 401) {
          this.errorMessage = 'Invalid email or password';
        }
        else if (
          err.status === 403 &&
          String(err.error?.message || '').includes(
            'verify your mobile number'
          )
        ) {
          this.router.navigate(['/verify-phone'], {
            queryParams: {
              email: this.loginForm.value.email
            }
          });
        }
        else if (err.status === 429) {
          this.errorMessage =
            'Unable to reach the server. Please try again later.';
        }
        else {
          this.errorMessage = 'Login failed. Please try again.';
        }
      }
    });
  }
}