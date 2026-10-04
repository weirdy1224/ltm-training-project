import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { HomeComponent } from './components/home/home.component';
import { LoginComponent } from './components/login/login.component';
import { RegistrationComponent } from './components/registration/registration.component';
import { NavbarComponent } from './components/navbar/navbar.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { CustomerdashboardComponent } from './components/customerdashboard/customerdashboard.component';
import { CustomerProfileComponent } from './components/customer-profile/customer-profile.component';
import { CustomerViewCoursesComponent } from './components/customer-view-courses/customer-view-courses.component';
import { CourseDetailsComponent } from './components/course-details/course-details.component';
import { MyCartComponent } from './components/my-cart/my-cart.component';
import { PlaceOrderComponent } from './components/place-order/place-order.component';
import { MyOrdersComponent } from './components/my-orders/my-orders.component';
import { OrderDetailsComponent } from './components/order-details/order-details.component';
import { ViewOrdersComponent } from './components/view-orders/view-orders.component';
import { ViewCoursesComponent } from './components/view-courses/view-courses.component';
import { AddCourseComponent } from './components/add-course/add-course.component';
import { EditCourseComponent } from './components/edit-course/edit-course.component';
import { AddReviewComponent } from './components/add-review/add-review.component';
import { ViewReviewComponent } from './components/view-review/view-review.component';
import { ManageAdminsComponent } from './components/manage-admins/manage-admins.component';
import { ConfirmDialogComponent } from './components/confirm-dialog/confirm-dialog.component';
import { ErrorComponent } from './components/error/error.component';
import { CourseCardComponent } from './shared/course-card/course-card.component';
import { StarRatingComponent } from './shared/star-rating/star-rating.component';

@NgModule({
  declarations: [
    AppComponent,
    HomeComponent,
    LoginComponent,
    RegistrationComponent,
    NavbarComponent,
    DashboardComponent,
    CustomerdashboardComponent,
    CustomerProfileComponent,
    CustomerViewCoursesComponent,
    CourseDetailsComponent,
    MyCartComponent,
    PlaceOrderComponent,
    MyOrdersComponent,
    OrderDetailsComponent,
    ViewOrdersComponent,
    ViewCoursesComponent,
    AddCourseComponent,
    EditCourseComponent,
    AddReviewComponent,
    ViewReviewComponent,
    ManageAdminsComponent,
    ConfirmDialogComponent,
    ErrorComponent,
    CourseCardComponent,
    StarRatingComponent
  ],
  imports: [
    BrowserModule,
    AppRoutingModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule { }
