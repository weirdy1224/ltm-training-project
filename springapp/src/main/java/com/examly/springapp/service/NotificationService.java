package com.examly.springapp.service;

import com.examly.springapp.model.Notification;
import com.examly.springapp.model.NotificationType;

import java.util.List;

public interface NotificationService {

    List<Notification> getNotificationsForUser(Long userId);

    long getUnreadCount(Long userId);

    Notification markAsRead(Long notificationId, Long userId);

    void markAllAsRead(Long userId);

    void deleteNotification(Long notificationId, Long userId);

    void deleteAllNotifications(Long userId);

    Notification createNotification(Long userId, String title, String message, NotificationType type, String redirectUrl);

    /** Same as above, for a notification about a support ticket. */
    Notification createNotification(Long userId, String title, String message, NotificationType type, String redirectUrl, Long ticketId);

    void notifyUsers(List<Long> userIds, String title, String message, NotificationType type, String redirectUrl);
}