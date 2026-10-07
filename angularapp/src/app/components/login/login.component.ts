import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CustomerService } from '../../services/customer.service';
import { AfterViewInit } from '@angular/core';
import { environment } from 'src/environments/environment.prod';
import { NgZone } from '@angular/core';

@Component({
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements AfterViewInit{
  loginForm: FormGroup;
  errorMessage = '';
  loading = false;
  showPassword = false;

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

    const google = (window as any).google;
  
    if (!google) {
      return;
    }
  
    google.accounts.id.initialize({
      client_id: environment.googleClientId,
      callback: (response: any) => {
        this.handleGoogleLogin(response);
      }
    });
  
    google.accounts.id.renderButton(
      document.getElementById('googleButton'),
      {
        theme: 'outline',
        size: 'large',
        width: 320
      }
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
  
        if (res.role === 'ADMIN') {
  
          this.ngZone.run(() => {
            this.router.navigate(['/dashboard']);
          });
  
          return;
        }
  
        this.customerService
          .viewCustomerByUserId()
          .subscribe({
  
            next: () => {
  
              this.ngZone.run(() => {
                this.router.navigate(['/dashboard']);
              });
  
            },
  
            error: (err) => {
  
              if (err.status === 404) {
  
                this.ngZone.run(() => {
                  this.router.navigate([
                    '/customerdashboard'
                  ]);
                });
  
                return;
              }
  
              this.errorMessage =
                'Unable to load profile';
            }
  
          });
  
      },
  
      error: (err) => {
  
        this.loading = false;
  
        if (err.status === 401) {
  
          this.errorMessage =
            'Invalid email or password';
  
        } else if (err.status === 429) {
  
          this.errorMessage =
            'Too many attempts. Please wait a while and try again.';
  
        } else {
  
          this.errorMessage =
            'Unable to reach the server. Please try again later.';
        }
      }
    });
  }
  handleGoogleLogin(response: any): void {

    this.auth.googleLogin(response.credential)
      .subscribe({
  
        next: (res: any) => {
  
          if (res.role === 'ADMIN') {
  
            this.ngZone.run(() => {
              this.router.navigate(['/dashboard']);
            });
  
            return;
          }
  
          this.customerService.viewCustomerByUserId().subscribe({

            next: () => {
          
              this.ngZone.run(() => {
                this.router.navigate([
                  '/dashboard'
                ]);
              });
          
            },
          
            error: (err) => {
          
              if (err.status === 404) {
          
                this.ngZone.run(() => {
                  this.router.navigate([
                    '/customerdashboard'
                  ]);
                });
          
                return;
              }
          
              this.errorMessage =
                'Unable to load profile';
            }
          
          });
        },
  
        error: (err) => {
  
          console.error(err);
  
          this.errorMessage =
            'Google sign in failed';
        }
      });
  }
}