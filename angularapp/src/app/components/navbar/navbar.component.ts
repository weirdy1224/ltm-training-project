import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ConfirmDialogComponent } from '../confirm-dialog/confirm-dialog.component';
import { Observable, Subscription } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';
import { AppNotification, NotificationType } from '../../models/notification.model';

@Component({
  selector: 'app-navbar',
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit, OnDestroy {
  role: string | null = null;
  notifications$: Observable<AppNotification[]>;
  unreadCount$: Observable<number>;

  private sub?: Subscription;

  constructor(
    private auth: AuthService,
    private router: Router,
    private dialog: MatDialog,
    private notifService: NotificationService,
    private snackBar: MatSnackBar
  ) {
    this.notifications$ = this.notifService.notifications$;
    this.unreadCount$ = this.notifService.unreadCount$;
  }

  ngOnInit(): void {
    this.sub = this.auth.role$.subscribe(r => (this.role = r));
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  markAsRead(event: Event, notif: AppNotification): void {
    event.stopPropagation();
    this.tryMarkRead(notif);
  }

  /** The server refuses to mark an admin's unhandled ticket notification as read; tell the admin why. */
  private tryMarkRead(notif: AppNotification): void {
    if (notif.read) {
      return;
    }
    this.notifService.markAsRead(notif.id).subscribe({
      error: err => this.snackBar.open(err?.error?.message || 'Could not mark the notification as read', 'OK', { duration: 5000 })
    });
  }

  markAllAsRead(event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    this.notifService.markAllAsRead().subscribe();
  }

  deleteNotification(event: Event, notif: AppNotification): void {
    event.stopPropagation();
    this.notifService.deleteNotification(notif.id).subscribe();
  }

  onNotificationClick(notif: AppNotification): void {
    if (!notif.read && notif.type !== NotificationType.TICKET_RAISED) {
      this.tryMarkRead(notif);
    }
    if (notif.redirectUrl) {
      this.router.navigateByUrl(notif.redirectUrl);
    }
  }

  getTypeIcon(type: NotificationType | string): string {
    switch (type) {
      case NotificationType.COURSE_PURCHASE:
      case 'COURSE_PURCHASE':
        return 'school';
      case NotificationType.NEW_STUDENT_ENROLLMENT:
      case 'NEW_STUDENT_ENROLLMENT':
        return 'person_add';
      case NotificationType.VIDEO_UPLOAD:
      case 'VIDEO_UPLOAD':
        return 'smart_display';
      case NotificationType.INSTRUCTOR_APPROVAL:
      case 'INSTRUCTOR_APPROVAL':
        return 'verified';
      case NotificationType.INSTRUCTOR_REJECTION:
      case 'INSTRUCTOR_REJECTION':
        return 'error_outline';
      case NotificationType.TICKET_CREATED:
      case NotificationType.TICKET_RAISED:
        return 'confirmation_number';
      case NotificationType.TICKET_RESPONSE:
        return 'support_agent';
      case NotificationType.TICKET_RESOLVED:
        return 'task_alt';
      default:
        return 'notifications';
    }
  }

  getTypeClass(type: NotificationType | string): string {
    switch (type) {
      case NotificationType.COURSE_PURCHASE:
      case 'COURSE_PURCHASE':
        return 'type-purchase';
      case NotificationType.NEW_STUDENT_ENROLLMENT:
      case 'NEW_STUDENT_ENROLLMENT':
        return 'type-enrollment';
      case NotificationType.VIDEO_UPLOAD:
      case 'VIDEO_UPLOAD':
        return 'type-video';
      case NotificationType.INSTRUCTOR_APPROVAL:
      case 'INSTRUCTOR_APPROVAL':
        return 'type-approved';
      case NotificationType.INSTRUCTOR_REJECTION:
      case 'INSTRUCTOR_REJECTION':
        return 'type-rejected';
      case NotificationType.TICKET_CREATED:
      case NotificationType.TICKET_RAISED:
      case NotificationType.TICKET_RESPONSE:
      case NotificationType.TICKET_RESOLVED:
        return 'type-enrollment';
      default:
        return 'type-default';
    }
  }

  askLogout(): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Log out?',
        message: 'Are you sure you want to logout?',
        confirmText: 'Yes, Logout',
        cancelText: 'Cancel',
        icon: 'logout'
      }
    }).afterClosed().subscribe(yes => {
      if (yes) {
        this.auth.logout();
        this.router.navigate(['/login']);
      }
    });
  }
}