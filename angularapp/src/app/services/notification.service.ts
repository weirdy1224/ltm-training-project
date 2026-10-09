import { Injectable, OnDestroy } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';
import { API, API_URL, STORAGE } from '../constant';
import { AppNotification, NotificationType } from '../models/notification.model';
import { AuthService } from './auth.service';
import { MatSnackBar } from '@angular/material/snack-bar';

@Injectable({
  providedIn: 'root'
})
export class NotificationService implements OnDestroy {
  private notificationsSubject = new BehaviorSubject<AppNotification[]>([]);
  public notifications$ = this.notificationsSubject.asObservable();

  private unreadCountSubject = new BehaviorSubject<number>(0);
  public unreadCount$ = this.unreadCountSubject.asObservable();

  private loadingSubject = new BehaviorSubject<boolean>(false);
  public loading$ = this.loadingSubject.asObservable();

  private socket: WebSocket | null = null;
  private reconnectTimer: any = null;
  private authSub: Subscription;
  private isDestroyed = false;

  constructor(
    private http: HttpClient,
    private auth: AuthService,
    private snackBar: MatSnackBar
  ) {
    // When user logs in, load notifications and connect WebSocket; when logged out, disconnect and clear
    this.authSub = this.auth.role$.subscribe(role => {
      if (role) {
        this.loadNotifications();
        this.connectWebSocket();
      } else {
        this.disconnectWebSocket();
        this.notificationsSubject.next([]);
        this.unreadCountSubject.next(0);
      }
    });
  }

  ngOnDestroy(): void {
    this.isDestroyed = true;
    this.authSub?.unsubscribe();
    this.disconnectWebSocket();
  }

  public loadNotifications(): void {
    if (!this.auth.isLoggedIn()) {
      return;
    }
    this.loadingSubject.next(true);
    this.http.get<AppNotification[]>(`${API_URL}${API.NOTIFICATIONS}`).pipe(
      tap(items => {
        this.notificationsSubject.next(items || []);
        const unread = (items || []).filter(n => !n.read).length;
        this.unreadCountSubject.next(unread);
        this.loadingSubject.next(false);
      }),
      catchError(err => {
        this.loadingSubject.next(false);
        throw err;
      })
    ).subscribe({
      error: () => {}
    });
  }

  public refreshUnreadCount(): void {
    if (!this.auth.isLoggedIn()) {
      return;
    }
    this.http.get<{ count: number }>(`${API_URL}${API.NOTIFICATIONS_UNREAD_COUNT}`).subscribe({
      next: res => {
        if (res && res.count !== undefined) {
          this.unreadCountSubject.next(res.count);
        }
      },
      error: () => {}
    });
  }

  public markAsRead(id: number): Observable<AppNotification> {
    return this.http.patch<AppNotification>(`${API_URL}${API.NOTIFICATIONS}/${id}/read`, {}).pipe(
      tap(updated => {
        const current = this.notificationsSubject.value;
        const index = current.findIndex(n => n.id === id);
        if (index !== -1) {
          const list = [...current];
          list[index] = { ...list[index], read: true };
          this.notificationsSubject.next(list);
          const unread = list.filter(n => !n.read).length;
          this.unreadCountSubject.next(unread);
        }
      })
    );
  }

  public markAllAsRead(): Observable<any> {
    return this.http.patch(`${API_URL}${API.NOTIFICATIONS_READ_ALL}`, {}).pipe(
      tap(() => {
        const list = this.notificationsSubject.value.map(n => ({ ...n, read: true }));
        this.notificationsSubject.next(list);
        this.unreadCountSubject.next(0);
      })
    );
  }

  public deleteNotification(id: number): Observable<any> {
    return this.http.delete(`${API_URL}${API.NOTIFICATIONS}/${id}`).pipe(
      tap(() => {
        const list = this.notificationsSubject.value.filter(n => n.id !== id);
        this.notificationsSubject.next(list);
        const unread = list.filter(n => !n.read).length;
        this.unreadCountSubject.next(unread);
      })
    );
  }

  public deleteAllNotifications(): Observable<any> {
    return this.http.delete(`${API_URL}${API.NOTIFICATIONS}`).pipe(
      tap(() => {
        this.notificationsSubject.next([]);
        this.unreadCountSubject.next(0);
      })
    );
  }

  /**
   * Real-time WebSocket connection to push notifications instantly.
   */
  public connectWebSocket(): void {
    if (this.socket && (this.socket.readyState === WebSocket.OPEN || this.socket.readyState === WebSocket.CONNECTING)) {
      return;
    }

    const token = this.auth.getToken();
    const userId = this.auth.getUserId();
    if (!token && !userId) {
      return;
    }

    try {
      const wsUrl = this.buildWebSocketUrl(token, userId);
      this.socket = new WebSocket(wsUrl);

      this.socket.onopen = () => {
        // Send heartbeat ping periodically
        this.startHeartbeat();
      };

      this.socket.onmessage = (event) => {
        if (!event.data || event.data === 'pong') {
          return;
        }
        try {
          const notif: AppNotification = JSON.parse(event.data);
          if (notif && notif.id) {
            this.handleIncomingNotification(notif);
          }
        } catch {
          // Ignore non-json frames
        }
      };

      this.socket.onclose = () => {
        this.stopHeartbeat();
        this.socket = null;
        if (!this.isDestroyed && this.auth.isLoggedIn()) {
          this.scheduleReconnect();
        }
      };

      this.socket.onerror = () => {
        if (this.socket) {
          this.socket.close();
        }
      };
    } catch {
      this.scheduleReconnect();
    }
  }

  public disconnectWebSocket(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    this.stopHeartbeat();
    if (this.socket) {
      this.socket.onclose = null;
      this.socket.onerror = null;
      this.socket.close();
      this.socket = null;
    }
  }

  private handleIncomingNotification(notification: AppNotification): void {
    const current = this.notificationsSubject.value;
    // Prevent duplicate entries
    if (!current.some(n => n.id === notification.id)) {
      this.notificationsSubject.next([notification, ...current]);
      if (!notification.read) {
        this.unreadCountSubject.next(this.unreadCountSubject.value + 1);
      }
      this.snackBar.open(`${notification.title}: ${notification.message}`, 'View', {
        duration: 5000,
        horizontalPosition: 'right',
        verticalPosition: 'bottom'
      });
    }
  }

  private heartbeatTimer: any = null;

  private startHeartbeat(): void {
    this.stopHeartbeat();
    this.heartbeatTimer = setInterval(() => {
      if (this.socket && this.socket.readyState === WebSocket.OPEN) {
        this.socket.send('ping');
      }
    }, 25000);
  }

  private stopHeartbeat(): void {
    if (this.heartbeatTimer) {
      clearInterval(this.heartbeatTimer);
      this.heartbeatTimer = null;
    }
  }

  private scheduleReconnect(): void {
    if (this.reconnectTimer || this.isDestroyed || !this.auth.isLoggedIn()) {
      return;
    }
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      if (!this.isDestroyed && this.auth.isLoggedIn()) {
        this.connectWebSocket();
      }
    }, 5000);
  }

  private buildWebSocketUrl(token: string | null, userId: string | null): string {
    const isHttps = window.location.protocol === 'https:' || API_URL.startsWith('https:');
    const wsScheme = isHttps ? 'wss:' : 'ws:';

    let host = window.location.host;
    if (API_URL.startsWith('http')) {
      try {
        const url = new URL(API_URL);
        host = url.host;
      } catch {}
    }

    const query = token ? `token=${encodeURIComponent(token)}` : `userId=${encodeURIComponent(userId || '')}`;
    return `${wsScheme}//${host}/ws/notifications?${query}`;
  }
}

