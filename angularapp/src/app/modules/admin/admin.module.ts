import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from 'src/app/shared/shared/shared.module';
import { ViewCoursesComponent } from '../../components/view-courses/view-courses.component'; 
import { ViewOrdersComponent } from '../../components/view-orders/view-orders.component';
import { ViewReviewComponent } from '../../components/view-review/view-review.component';
import { ConfirmDialogComponent } from 'src/app/components/confirm-dialog/confirm-dialog.component';
import { ManageAdminsComponent } from 'src/app/components/manage-admins/manage-admins.component';
import { ManageInstructorsComponent } from 'src/app/components/manage-instructors/manage-instructors.component';
import { AddCourseComponent } from '../../components/add-course/add-course.component';
import { EditCourseComponent } from '../../components/edit-course/edit-course.component';
import { ManageVideosComponent } from '../../components/manage-videos/manage-videos.component';

const routes: Routes = [
  { path: 'add-course', component: AddCourseComponent },
  { path: 'edit-course/:id', component: EditCourseComponent },
  { path: 'view-courses', component: ViewCoursesComponent },
  { path: 'view-orders', component: ViewOrdersComponent },
  { path: 'view-review', component: ViewReviewComponent },
  { path: 'manage-admins', component: ManageAdminsComponent },
  { path: 'manage-instructors', component: ManageInstructorsComponent },
  { path: 'manage-videos/:courseId', component: ManageVideosComponent }
];

/** Pages only an admin can open. Lazy loaded and protected by AuthGuard (role ADMIN) in app-routing. @author Sivamuthu */
@NgModule({
  declarations: [ViewCoursesComponent, ViewOrdersComponent, ViewReviewComponent, ManageAdminsComponent, ManageInstructorsComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class AdminModule {} 