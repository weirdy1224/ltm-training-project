import { HttpEventType } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ConfirmDialogComponent } from '../confirm-dialog/confirm-dialog.component';
import { ROLES } from '../../constant';
import { Course } from '../../models/course.model';
import { CourseVideo } from '../../models/video.model';
import { AuthService } from '../../services/auth.service';
import { CourseService } from '../../services/course.service';
import { VideoService } from '../../services/video.service';

/**
 * Add, rename, reorder and delete the video lessons of ONE course. Used by instructors (their own courses) and admins.
 * The file goes to the server, which saves it in a folder and writes its path into the database; this page only shows
 * the upload progress and the list of lessons.
 *
 * @author Lokesh
 */
@Component({
  selector: 'app-manage-videos',
  templateUrl: './manage-videos.component.html',
  styleUrls: ['./manage-videos.component.css']
})
export class ManageVideosComponent implements OnInit {
  courseId = '';
  course?: Course;
  videos: CourseVideo[] = [];
  loaded = false;
  errorMessage = '';

  uploadForm: FormGroup;
  file?: File;
  fileError = '';
  uploading = false;
  progress = 0;

  editing?: CourseVideo;
  editForm: FormGroup;
  savingEdit = false;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private courseService: CourseService,
    private videoService: VideoService,
    private auth: AuthService,
    private dialog: MatDialog,
    private snack: MatSnackBar
  ) {
    this.uploadForm = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(150)]],
      description: ['', [Validators.maxLength(1000)]]
    });
    this.editForm = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(150)]],
      description: ['', [Validators.maxLength(1000)]],
      orderIndex: [1, [Validators.required, Validators.min(1)]]
    });
  }

  get f() { return this.uploadForm.controls; }
  get e() { return this.editForm.controls; }

  /** Where "Back" goes: instructors to their courses, admins to the course list. */
  get backLink(): string {
    return this.auth.getRole() === ROLES.INSTRUCTOR ? '/instructor/courses' : '/view-courses';
  }

  ngOnInit(): void {
    this.courseId = this.route.snapshot.paramMap.get('courseId') || '';
    this.courseService.getCourseById(this.courseId).subscribe({
      next: (c) => (this.course = c),
      error: () => (this.errorMessage = 'This course could not be found.')
    });
    this.load();
  }

  load(): void {
    this.videoService.list(this.courseId).subscribe({
      next: (list) => { this.videos = list || []; this.loaded = true; },
      error: () => { this.errorMessage = this.errorMessage || 'Could not load the lessons.'; this.loaded = true; }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const picked = input.files?.[0];
    input.value = '';   // lets the same file be chosen again
    if (!picked) { return; }
    const problem = this.videoService.validate(picked);
    this.fileError = problem || '';
    this.file = problem ? undefined : picked;
    // a handy default: the file name (without extension) becomes the title when the title is still empty
    if (!problem && !this.f['title'].value) {
      this.f['title'].setValue(picked.name.replace(/\.[^.]+$/, '').slice(0, 150));
    }
  }

  upload(): void {
    if (this.uploadForm.invalid || !this.file || this.uploading) {
      if (!this.file) { this.fileError = 'Choose a video file'; }
      return;
    }
    this.uploading = true;
    this.progress = 0;
    this.errorMessage = '';
    const { title, description } = this.uploadForm.value;
    this.videoService.upload(this.courseId, this.file, title.trim(), description?.trim()).subscribe({
      next: (event) => {
        if (event.type === HttpEventType.UploadProgress && event.total) {
          this.progress = Math.round((100 * event.loaded) / event.total);
        } else if (event.type === HttpEventType.Response) {
          this.uploading = false;
          this.file = undefined;
          this.uploadForm.reset({ title: '', description: '' });
          this.snack.open('Video lesson added', 'OK', { duration: 3000 });
          this.load();
        }
      },
      error: (err) => {
        this.uploading = false;
        this.errorMessage = err.status === 413
          ? 'The video is larger than the server allows.'
          : (err.error?.message || 'Could not upload the video. Please try again.');
      }
    });
  }

  startEdit(video: CourseVideo): void {
    this.editing = video;
    this.editForm.setValue({ title: video.title || '', description: video.description || '', orderIndex: video.orderIndex || 1 });
  }

  cancelEdit(): void {
    this.editing = undefined;
  }

  saveEdit(): void {
    if (!this.editing || this.editForm.invalid || this.savingEdit || this.editing.videoId === undefined) { return; }
    this.savingEdit = true;
    const { title, description, orderIndex } = this.editForm.value;
    this.videoService.update(this.editing.videoId, { title: title.trim(), description: description?.trim(), orderIndex }).subscribe({
      next: () => { this.savingEdit = false; this.editing = undefined; this.snack.open('Lesson updated', 'OK', { duration: 2500 }); this.load(); },
      error: (err) => { this.savingEdit = false; this.errorMessage = err.error?.message || 'Could not update the lesson.'; }
    });
  }

  remove(video: CourseVideo): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Delete lesson?',
        message: `Delete "${video.title}"? The video file is removed from the server and this cannot be undone.`,
        confirmText: 'Delete', cancelText: 'Cancel', icon: 'warning', danger: true
      }
    }).afterClosed().subscribe(yes => {
      if (!yes || video.videoId === undefined) { return; }
      this.videoService.delete(video.videoId).subscribe({
        next: () => { this.snack.open('Lesson deleted', 'OK', { duration: 2500 }); this.load(); },
        error: (err) => (this.errorMessage = err.error?.message || 'Could not delete the lesson.')
      });
    });
  }

  /** "12.4 MB" for the list. */
  size(bytes?: number): string {
    const b = bytes || 0;
    return b >= 1024 * 1024 ? `${(b / (1024 * 1024)).toFixed(1)} MB` : `${Math.max(1, Math.round(b / 1024))} KB`;
  }
}
