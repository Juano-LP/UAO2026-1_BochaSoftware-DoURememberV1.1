package com.springboot.backend.salazar.usersbackend.users_backend.controllers;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.springboot.backend.salazar.usersbackend.users_backend.dto.CreateNotificationRequest;
import com.springboot.backend.salazar.usersbackend.users_backend.entities.Notification;
import com.springboot.backend.salazar.usersbackend.users_backend.services.NotificationService;

import jakarta.validation.Valid;

@CrossOrigin(origins = {"http://localhost:4200"})
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<?> createNotification(@Valid @RequestBody CreateNotificationRequest request,
            BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(result.getAllErrors());
        }

        LocalDateTime eventAt = request.getEventAt() == null ? LocalDateTime.now() : request.getEventAt();
        Notification notification = notificationService.createIfNotExists(request.getUserId(), request.getEventKey(),
                request.getType(), request.getTitle(), request.getDescription(), eventAt);

        return ResponseEntity.ok(notification);
    }

    @GetMapping("/user/{userId}")
    public List<Notification> listByUser(@PathVariable Long userId,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return notificationService.listByUser(userId, unreadOnly);
    }

    @PutMapping("/{notificationId}/read")
    public Notification markAsRead(@PathVariable Long notificationId, @RequestParam Long userId) {
        return notificationService.markAsRead(notificationId, userId);
    }

    @GetMapping(value = "/stream/{userId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable Long userId) {
        return notificationService.subscribe(userId);
    }

    @PostMapping("/daily-lesson/run")
    public ResponseEntity<?> triggerDailyLesson() {
        notificationService.sendDailyLessonNotifications();
        return ResponseEntity.ok().build();
    }
}