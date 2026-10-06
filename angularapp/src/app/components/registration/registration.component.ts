import { Component } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, FormGroupDirective, NgForm, ValidationErrors, Validators } from '@angular/forms';
import { ErrorStateMatcher } from '@angular/material/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
/** Username: no spaces anywhere. */
function noSpaces(control: AbstractControl): ValidationErrors | null {
  return /\s/.test(control.value || '') ? { spaces: true } : null;
}
/** Password: one upper case, one lower case, one number, one special character, and no spaces. */
export const PASSWORD_RULES = [
  { key: 'length', label: '8 to 72 characters', test: (v: string) => v.length >= 8 && v.length <= 72 },
  { key: 'upper', label: 'One upper case letter', test: (v: string) => /[A-Z]/.test(v) },
  { key: 'lower', label: 'One lower case letter', test: (v: string) => /[a-z]/.test(v) },
  { key: 'digit', label: 'One number', test: (v: string) => /[0-9]/.test(v) },
  { key: 'special', label: 'One special character (for example @ # $ % !)', test: (v: string) => /[^A-Za-z0-9\s]/.test(v) },
  { key: 'space', label: 'No spaces', test: (v: string) => v.length > 0 && !/\s/.test(v) }
];
function strongPassword(control: AbstractControl): ValidationErrors | null {
  const v: string = control.value || '';
  return v && PASSWORD_RULES.some(r => !r.test(v)) ? { weak: true } : null;
}
/** Email: name, one @, then a domain with a dot followed by at least 2 letters (a@b.com works, a@b does not). */
const EMAIL_PATTERN = /^[A-Za-z0-9._%+\-]+@[A-Za-z0-9\-]+(\.[A-Za-z0-9\-]+)*\.[A-Za-z]{2,}$/;
/** Mobile: 10 digits, the first one is 6, 7, 8 or 9. */
const MOBILE_PATTERN = /^[6-9][0-9]{9}$/;
function passwordsMatch(group: AbstractControl): ValidationErrors | null {
  const p = group.get('password')?.value;
  const c = group.get('confirmPassword')?.value;
  return p && c && p !== c ? { mismatch: true } : null;
}
/** Shows the "Password do not match" error on the confirm field, even though the rule lives on the whole form. */
class ConfirmMatcher implements ErrorStateMatcher {
  isErrorState(control: AbstractControl | null, form: FormGroupDirective | NgForm | null): boolean {
    return !!control && (control.touched || control.dirty) && (control.invalid || !!form?.form.hasError('mismatch'));
  }
}
@Component({
  selector: 'app-registration',
  templateUrl: './registration.component.html',
  styleUrls: ['./registration.component.css']
})
export class RegistrationComponent {
  registerForm: FormGroup;
  errorMessage = '';
  loading = false;
  showPassword = false;
  passwordFocused = false;
  confirmMatcher = new ConfirmMatcher();
  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router) {
    this.registerForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3), noSpaces]],
      email: ['', [Validators.required, Validators.pattern(EMAIL_PATTERN)]],
      password: ['', [Validators.required, strongPassword]],
      confirmPassword: ['', [Validators.required]],
      mobileNumber: ['', [Validators.required, Validators.pattern(MOBILE_PATTERN)]],
      role: ['Customer'] // fixed: public sign-up always creates a customer
    }, { validators: passwordsMatch });
  }
  get f() { return this.registerForm.controls; }
  /** Live checklist under the password field. */
  get passwordChecks() {
    const v: string = this.f['password'].value || '';
    return PASSWORD_RULES.map(r => ({ label: r.label, ok: r.test(v) }));
  }
  onSubmit(): void {
    if (this.registerForm.invalid || this.loading) { return; }
    this.loading = true;
    this.errorMessage = '';
    const { confirmPassword, ...user } = this.registerForm.value;
    this.auth.register(user).subscribe({
      next: () => { this.loading = false; this.router.navigate(['/login']); },
      error: (err) => {
        this.loading = false;
        if (err.status === 409) {
          this.errorMessage = 'A user with this email already exists';
        } else if (err.status === 429) {
          this.errorMessage = 'Too many attempts. Please wait a minute and try again.';
        } else if (err.status === 400) {
          this.errorMessage = err.error?.message || 'Please check the entered details';
        } else {
          this.errorMessage = 'Registration failed. Please try again later.';
        }
      }
    });
  }
}
