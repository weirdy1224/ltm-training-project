package com.examly.springapp.service;

import com.examly.springapp.exception.DatabaseOperationException;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.model.Notification;
import com.examly.springapp.model.NotificationType;
import com.examly.springapp.repository.NotificationRepo;
import com.examly.springapp.websocket.NotificationWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepo notificationRepo;
    private final NotificationWebSocketHandler webSocketHandler;

    public NotificationServiceImpl(NotificationRepo notificationRepo,
                                   NotificationWebSocketHandler webSocketHandler) {
        this.notificationRepo = notificationRepo;
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public List<Notification> getNotificationsForUser(Long userId) {
        log.trace("getNotificationsForUser entered for userId={}", userId);
        return DatabaseOperationException.guard("loading notifications",
                () -> notificationRepo.findByUserIdOrderByCreatedAtDesc(userId));
    }

    @Override
    public long getUnreadCount(Long userId) {
        log.trace("getUnreadCount entered for userId={}", userId);
        return DatabaseOperationException.guard("counting unread notifications",
                () -> notificationRepo.countByUserIdAndIsReadFalse(userId));
    }

    @Override
    @Transactional
    public Notification markAsRead(Long notificationId, Long userId) {
        log.trace("markAsRead entered for notificationId={}, userId={}", notificationId, userId);
        Notification notification = DatabaseOperationException.guard("finding notification",
                () -> notificationRepo.findByIdAndUserId(notificationId, userId))
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id " + notificationId));

        if (!notification.isRead()) {
            notification.setRead(true);
            return DatabaseOperationException.guard("updating notification read state",
                    () -> notificationRepo.save(notification));
        }
        return notification;
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        log.trace("markAllAsRead entered for userId={}", userId);
        DatabaseOperationException.guard("marking all notifications as read",
                () -> notificationRepo.markAllAsRead(userId));
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        log.trace("deleteNotification entered for notificationId={}, userId={}", notificationId, userId);
        Notification notification = DatabaseOperationException.guard("finding notification to delete",
                () -> notificationRepo.findByIdAndUserId(notificationId, userId))
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id " + notificationId));

        DatabaseOperationException.guardVoid("deleting notification",
                () -> notificationRepo.delete(notification));
    }

    @Override
    @Transactional
    public void deleteAllNotifications(Long userId) {
        log.trace("deleteAllNotifications entered for userId={}", userId);
        DatabaseOperationException.guardVoid("deleting all notifications",
                () -> notificationRepo.deleteAllByUserId(userId));
    }

    @Override
    @Transactional
    public Notification createNotification(Long userId, String title, String message, NotificationType type, String redirectUrl) {
        log.trace("createNotification entered for userId={}, type={}", userId, type);
        Notification notification = new Notification(userId, title, message, type, redirectUrl);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);

        Notification saved = DatabaseOperationException.guard("saving notification",
                () -> notificationRepo.save(notification));

        // Push real-time notification via WebSocket
        try {
            webSocketHandler.sendNotification(userId, saved);
        } catch (Exception ex) {
            log.warn("Failed to push real-time notification to user {} over WebSocket: {}", userId, ex.getMessage());
        }

        return saved;
    }

    @Override
    @Transactional
    public void notifyUsers(List<Long> userIds, String title, String message, NotificationType type, String redirectUrl) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        for (Long userId : userIds) {
            try {
                createNotification(userId, title, message, type, redirectUrl);
            } catch (Exception e) {
                log.error("Failed to create notification for userId {}: {}", userId, e.getMessage());
            }
        }
    }
}

