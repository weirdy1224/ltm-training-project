import { Component, OnInit } from '@angular/core';
import { InstructorStats } from '../../models/stats.model';
import { InstructorService } from '../../services/instructor.service';
import { UserStoreService } from '../../services/user-store.service';

/**
 * Landing page of an instructor: the numbers of their own courses and a shortcut to create a course.
 *
 * @author Lokesh
 */
@Component({
  selector: 'app-instructor-dashboard',
  templateUrl: './instructor-dashboard.component.html',
  styleUrls: ['./instructor-dashboard.component.css']
})
export class InstructorDashboardComponent implements OnInit {
  name = '';
  stats?: InstructorStats;
  statsFailed = false;

  constructor(private instructorService: InstructorService, private userStore: UserStoreService) {}

  ngOnInit(): void {
    this.name = this.userStore.user?.username || '';
    this.instructorService.getStats().subscribe({
      next: (s) => (this.stats = s),
      error: () => (this.statsFailed = true)
    });
  }
}
