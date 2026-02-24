package com.springboot.backend.salazar.usersbackend.users_backend.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.springboot.backend.salazar.usersbackend.users_backend.entities.Notification;
import com.springboot.backend.salazar.usersbackend.users_backend.enums.NotificationType;

public interface NotificationService {

    Notification createIfNotExists(Long userId, String eventKey, NotificationType type, String title, String description,
            LocalDateTime eventAt);

    List<Notification> listByUser(Long userId, boolean unreadOnly);

    Notification markAsRead(Long notificationId, Long userId);

    int sendDailyLessonNotifications();

    void sendDailyLessonEmailToAddress(String destinationEmail);

    SseEmitter subscribe(Long userId);
}
