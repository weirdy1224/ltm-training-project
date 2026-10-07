import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ConfirmDialogComponent } from '../confirm-dialog/confirm-dialog.component';
import { InstructorApplication } from '../../models/instructor-profile.model';
import { AdminService } from '../../services/admin.service';

/**
 * Admin-only page: instructors who registered themselves wait here until an admin approves (or rejects) them.
 *
 * @author Lokesh
 */
@Component({
  selector: 'app-manage-instructors',
  templateUrl: './manage-instructors.component.html',
  styleUrls: ['./manage-instructors.component.css']
})
export class ManageInstructorsComponent implements OnInit {
  instructors: InstructorApplication[] = [];
  /** Rows whose registration details are open. */
  private open = new Set<number>();
  loaded = false;
  errorMessage = '';

  constructor(private adminService: AdminService, private dialog: MatDialog, private snack: MatSnackBar) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.adminService.listInstructors().subscribe({
      next: (list) => { this.instructors = list || []; this.loaded = true; },
      error: () => { this.errorMessage = 'Could not load the instructors.'; this.loaded = true; }
    });
  }

  isApproved(i: InstructorApplication): boolean {
    return i.approved;
  }

  isVerified(i: InstructorApplication): boolean {
    return i.emailVerified;
  }

  isOpen(i: InstructorApplication): boolean {
    return this.open.has(i.userId);
  }

  toggle(i: InstructorApplication): void {
    if (!this.open.delete(i.userId)) { this.open.add(i.userId); }
  }

  approve(i: InstructorApplication): void {
    this.adminService.approveInstructor(i.userId).subscribe({
      next: () => { this.snack.open(`${i.username} approved`, 'OK', { duration: 3000 }); this.load(); },
      error: (err) => (this.errorMessage = err.error?.message || 'Could not approve the instructor.')
    });
  }

  reject(i: InstructorApplication): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Reject instructor?',
        message: `Remove the pending account of ${i.username} (${i.email})? They will have to register again.`,
        confirmText: 'Reject', cancelText: 'Cancel', icon: 'warning', danger: true
      }
    }).afterClosed().subscribe(yes => {
      if (!yes) { return; }
      this.adminService.rejectInstructor(i.userId).subscribe({
        next: () => { this.snack.open('Instructor rejected', 'OK', { duration: 3000 }); this.load(); },
        error: (err) => (this.errorMessage = err.error?.message || 'Could not reject the instructor.')
      });
    });
  }
}
