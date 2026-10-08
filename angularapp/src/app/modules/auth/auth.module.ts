import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from 'src/app/shared/shared/shared.module';
import { LoginComponent } from '../../components/login/login.component';
import { RegistrationComponent } from '../../components/registration/registration.component';
import { ForgotPasswordComponent } from '../../components/forgot-password/forgot-password.component';
import { VerifyOtpComponent } from '../../components/verify-otp/verify-otp.component';
import { InstructorRegisterComponent } from '../../components/instructor-register/instructor-register.component';
import { ApplicationStatusComponent } from '../../components/application-status/application-status.component';
import { ResetPasswordComponent } from '../../components/reset-password/reset-password.component';

const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: 'registration',
    component: RegistrationComponent
  },
  {
    path: 'instructor-register',
    component: InstructorRegisterComponent
  },
  {
    path: 'application-status',
    component: ApplicationStatusComponent
  },
  {
    path: 'verify-otp',
    component: VerifyOtpComponent
  },
  {
    path: 'forgot-password',
    component: ForgotPasswordComponent
  },
  {
    path: 'reset-password',
    component: ResetPasswordComponent
  }
];

/** Login, registration, email OTP and password reset. Lazy loaded: nobody needs this code until they open the login page. @author Suriya */
@NgModule({
  declarations: [LoginComponent, RegistrationComponent, VerifyOtpComponent, ForgotPasswordComponent, ResetPasswordComponent, InstructorRegisterComponent, ApplicationStatusComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class AuthModule {} 