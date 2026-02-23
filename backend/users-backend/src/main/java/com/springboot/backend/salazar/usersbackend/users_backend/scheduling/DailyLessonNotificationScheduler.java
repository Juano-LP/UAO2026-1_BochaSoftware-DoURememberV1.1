package com.springboot.backend.salazar.usersbackend.users_backend.scheduling;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.springboot.backend.salazar.usersbackend.users_backend.services.NotificationService;

@Component
public class DailyLessonNotificationScheduler {

    private final NotificationService notificationService;

    public DailyLessonNotificationScheduler(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "${notifications.daily-lesson.cron:0 0 8 * * *}")
    public void sendDailyLessonNotification() {
        notificationService.sendDailyLessonNotifications();
    }
}