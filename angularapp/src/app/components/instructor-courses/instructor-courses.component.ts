import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ConfirmDialogComponent } from '../confirm-dialog/confirm-dialog.component';
import { Course } from '../../models/course.model';
import { CourseService } from '../../services/course.service';

/**
 * The courses created by the logged-in instructor, with Edit, Videos and Delete for each.
 *
 * @author Lokesh
 */
@Component({
  selector: 'app-instructor-courses',
  templateUrl: './instructor-courses.component.html',
  styleUrls: ['./instructor-courses.component.css']
})
export class InstructorCoursesComponent implements OnInit {
  courses: Course[] = [];
  loaded = false;
  errorMessage = '';

  constructor(private courseService: CourseService, private router: Router, private dialog: MatDialog, private snack: MatSnackBar) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.errorMessage = '';
    this.courseService.myCourses().subscribe({
      next: (list) => { this.courses = list || []; this.loaded = true; },
      error: () => { this.errorMessage = 'Could not load your courses.'; this.loaded = true; }
    });
  }

  edit(course: Course): void {
    this.router.navigate(['/instructor/edit-course', course.courseId]);
  }

  videos(course: Course): void {
    this.router.navigate(['/instructor/videos', course.courseId]);
  }

  delete(course: Course): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete course?',
        message: `Delete "${course.courseType}" and all its video lessons? This cannot be undone.`,
        confirmText: 'Delete', cancelText: 'Cancel', icon: 'warning', danger: true
      }
    }).afterClosed().subscribe(yes => {
      if (!yes) { return; }
      this.courseService.deleteCourse(String(course.courseId)).subscribe({
        next: () => { this.snack.open('Course deleted', 'OK', { duration: 3000 }); this.load(); },
        error: (err) => (this.errorMessage = err.error?.message || 'Could not delete the course.')
      });
    });
  }
}
