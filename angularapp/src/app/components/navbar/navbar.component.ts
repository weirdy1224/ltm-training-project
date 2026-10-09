import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
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
    private notifService: NotificationService
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
    if (!notif.read) {
      this.notifService.markAsRead(notif.id).subscribe();
    }
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
    if (!notif.read) {
      this.notifService.markAsRead(notif.id).subscribe();
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