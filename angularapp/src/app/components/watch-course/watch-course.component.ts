import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ROLES } from '../../constant';
import { Course } from '../../models/course.model';
import { CourseVideo } from '../../models/video.model';
import { AuthService } from '../../services/auth.service';
import { CourseService } from '../../services/course.service';
import { VideoService } from '../../services/video.service';

/**
 * Video player page of one course: the lesson list on the side and a player. Before a lesson plays, the server is
 * asked for a short-lived signed link, because a <video> tag cannot send the login token.
 * Allowed for enrolled customers, the owner instructor and admins (the server decides, the page only shows its answer).
 *
 * @author Lokesh
 */
@Component({
  selector: 'app-watch-course',
  templateUrl: './watch-course.component.html',
  styleUrls: ['./watch-course.component.css']
})
export class WatchCourseComponent implements OnInit {
  course?: Course;
  videos: CourseVideo[] = [];
  current?: CourseVideo;
  src = '';
  loaded = false;
  loadingVideo = false;
  private retried = false;
  errorMessage = '';
  role: string | null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private courseService: CourseService,
    private videoService: VideoService,
    private auth: AuthService
  ) {
    this.role = this.auth.getRole();
  }

  get canManage(): boolean {
    return this.role === ROLES.ADMIN || this.role === ROLES.INSTRUCTOR;
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id') || '';
    this.courseService.getCourseById(id).subscribe({
      next: (c) => (this.course = c),
      error: () => (this.errorMessage = 'This course could not be found.')
    });
    this.videoService.list(id).subscribe({
      next: (list) => {
        this.videos = list || [];
        this.loaded = true;
        const wanted = Number(this.route.snapshot.queryParamMap.get('video'));
        const first = this.videos.find(v => v.videoId === wanted && v.canWatch) || this.videos.find(v => v.canWatch);
        if (first) { this.play(first); }
      },
      error: () => { this.errorMessage = this.errorMessage || 'Could not load the lessons.'; this.loaded = true; }
    });
  }

  /** True when the user can open at least one lesson. */
  get anyWatchable(): boolean {
    return this.videos.some(v => v.canWatch);
  }

  play(video: CourseVideo): void {
    if (!video.canWatch || video.videoId === undefined) { return; }
    if (this.current?.videoId !== video.videoId) { this.retried = false; }
    this.current = video;
    this.src = '';
    this.loadingVideo = true;
    this.errorMessage = '';
    this.videoService.streamUrl(video.videoId).subscribe({
      next: (url) => { this.src = url; this.loadingVideo = false; },
      error: () => { this.loadingVideo = false; this.errorMessage = 'This lesson could not be opened.'; }
    });
  }

  /** Plays the next lesson when one ends. */
  onEnded(): void {
    const i = this.videos.findIndex(v => v.videoId === this.current?.videoId);
    const next = this.videos.slice(i + 1).find(v => v.canWatch);
    if (next) { this.play(next); }
  }

  /** The signed link expires after a while: ask for a fresh one when the player fails. */
  onPlayerError(): void {
    if (this.current && this.src && !this.retried) {
      this.retried = true;   // only once, so a broken file cannot cause an endless loop
      this.play(this.current);
    } else if (this.current) {
      this.errorMessage = 'This lesson could not be played.';
    }
  }

  back(): void {
    this.router.navigate(['/course-details', this.course?.courseId]);
  }
}
