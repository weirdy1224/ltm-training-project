import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from 'src/app/shared/shared/shared.module';
import { AddCourseComponent } from '../../components/add-course/add-course.component';
import { EditCourseComponent } from '../../components/edit-course/edit-course.component';
import { ManageVideosComponent } from '../../components/manage-videos/manage-videos.component';
import { InstructorDashboardComponent } from '../../components/instructor-dashboard/instructor-dashboard.component';
import { InstructorCoursesComponent } from 'src/app/components/instructor-courses/instructor-courses.component';
const routes: Routes = [
  { path: 'dashboard', component: InstructorDashboardComponent },
  { path: 'courses', component: InstructorCoursesComponent },
  { path: 'add-course', component: AddCourseComponent },
  { path: 'edit-course/:id', component: EditCourseComponent },
  { path: 'videos/:courseId', component: ManageVideosComponent }
];

/** Pages only an instructor can open (under /instructor). Lazy loaded and protected by AuthGuard (role INSTRUCTOR). @author Lokesh */
@NgModule({
  declarations: [InstructorDashboardComponent, InstructorCoursesComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class InstructorModule {}
