import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MaterialModule } from 'src/app/material.module';
import { CourseCardComponent } from '../course-card/course-card.component';
import { StarRatingComponent } from '../star-rating/star-rating.component';
import { AddCourseComponent } from '../../components/add-course/add-course.component';
import { EditCourseComponent } from '../../components/edit-course/edit-course.component';
import { ManageVideosComponent } from '../../components/manage-videos/manage-videos.component';

import { RelativeTimePipe } from '../relative-time.pipe';

/**
 * Things every lazy-loaded feature module needs: Angular common pieces, forms, router links,
 * Angular Material and the small shared components.
 *
 * @author Team Lead
 */
@NgModule({
  declarations: [CourseCardComponent, StarRatingComponent, AddCourseComponent, EditCourseComponent, ManageVideosComponent, RelativeTimePipe],
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, MaterialModule],
  exports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule, MaterialModule, CourseCardComponent, StarRatingComponent, AddCourseComponent, EditCourseComponent, ManageVideosComponent, RelativeTimePipe]
})
export class SharedModule {}