import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { SharedModule } from 'src/app/shared/shared/shared.module';
import { WatchCourseComponent } from '../../components/watch-course/watch-course.component';
import { CourseDetailsComponent } from '../../components/course-details/course-details.component';

const routes: Routes = [
  { path: 'course-details/:id', component: CourseDetailsComponent },
  { path: 'watch-course/:id', component: WatchCourseComponent }
];

/** One course in full. Both admins and customers can open it. @author Amogh */
@NgModule({
  declarations: [CourseDetailsComponent, WatchCourseComponent],
  imports: [SharedModule, RouterModule.forChild(routes)]
})
export class CourseDetailsModule {}