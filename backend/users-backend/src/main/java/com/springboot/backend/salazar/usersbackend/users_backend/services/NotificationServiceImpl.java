package com.springboot.backend.salazar.usersbackend.users_backend.services;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.springboot.backend.salazar.usersbackend.users_backend.entities.Notification;
import com.springboot.backend.salazar.usersbackend.users_backend.entities.User;
import com.springboot.backend.salazar.usersbackend.users_backend.enums.NotificationType;
import com.springboot.backend.salazar.usersbackend.users_backend.repositories.NotificationRepository;
import com.springboot.backend.salazar.usersbackend.users_backend.repositories.UserRepository;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationServiceImpl.class);
    private static final Long SSE_TIMEOUT = 60L * 60L * 1000L;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${notifications.mail.from:}")
    private String fromEmail;

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    public NotificationServiceImpl(NotificationRepository notificationRepository, UserRepository userRepository,
            JavaMailSender mailSender) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
    }

    @Override
    @Transactional
    public Notification createIfNotExists(Long userId, String eventKey, NotificationType type, String title,
            String description, LocalDateTime eventAt) {
        Optional<Notification> existing = notificationRepository.findByUserIdAndEventKey(userId, eventKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setEventKey(eventKey);
        notification.setType(type);
        notification.setTitle(title);
        notification.setDescription(description);
        notification.setEventAt(eventAt == null ? LocalDateTime.now() : eventAt);

        try {
            Notification saved = notificationRepository.save(notification);
            sendEmail(saved);
            pushRealtimeNotification(saved);
            return saved;
        } catch (DataIntegrityViolationException ex) {
            return notificationRepository.findByUserIdAndEventKey(userId, eventKey)
                    .orElseThrow(() -> ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> listByUser(Long userId, boolean unreadOnly) {
        if (unreadOnly) {
            return notificationRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId);
        }
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Backward-compatible alias for previous controller implementations.
     */
    @Transactional(readOnly = true)
    public List<Notification> getByUser(Long userId, boolean unreadOnly) {
        return listByUser(userId, unreadOnly);
    }

    /**
     * Backward-compatible helper that creates a reminder notification.
     */
    @Transactional
    public Notification createNotification(Long userId, String title, String description) {
        String eventKey = "MANUAL:" + userId + ":" + System.currentTimeMillis();
        return createIfNotExists(userId, eventKey, NotificationType.REMINDER, title, description, LocalDateTime.now());
    }

    @Override
    @Transactional
    public Notification markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with ID: " + notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Notification does not belong to user ID: " + userId);
        }

        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
            pushRealtimeNotification(notification);
        }

        return notification;
    }

    @Override
    public void sendDailyLessonEmailToAddress(String destinationEmail) {
        String title = "Recordatorio: lección diaria pendiente";
        String description = "Es momento de completar tu lección diaria para mantener tu progreso.";
        sendEmailOrThrow(destinationEmail, title, description);
    }

    @Override
    @Transactional
    public int sendDailyLessonNotifications() {
        LocalDate currentDate = LocalDate.now();
        String title = "Recordatorio: lección diaria pendiente";
        String description = "Es momento de completar tu lección diaria para mantener tu progreso.";
        int createdNotifications = 0;

        for (User user : userRepository.findAll()) {
            String eventKey = "DAILY_LESSON:" + currentDate + ":user-" + user.getId();
            boolean existed = notificationRepository.findByUserIdAndEventKey(user.getId(), eventKey).isPresent();

            createIfNotExists(user.getId(), eventKey, NotificationType.DAILY_LESSON, title, description,
                    currentDate.atStartOfDay());

            if (!existed) {
                createdNotifications++;
            }
        }

        return createdNotifications;
    }

    @Override
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emittersByUser.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError((ex) -> removeEmitter(userId, emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("subscription-ready"));
        } catch (IOException ex) {
            removeEmitter(userId, emitter);
        }

        return emitter;
    }

    private void sendEmailOrThrow(String destination, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (fromEmail != null && !fromEmail.isBlank()) {
                message.setFrom(fromEmail);
            }

            message.setTo(destination);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            LOGGER.error("Daily lesson email could not be sent to {}", destination, ex);
            throw new IllegalStateException("Email dispatch failed for destination: " + destination, ex);
        }
    }

    /**
     * Backward-compatible helper for controllers that send ad-hoc emails.
     */
    public void sendEmail(String destination, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (fromEmail != null && !fromEmail.isBlank()) {
                message.setFrom(fromEmail);
            }

            message.setTo(destination);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception ex) {
            LOGGER.warn("Ad-hoc email could not be sent to {}: {}", destination, ex.getMessage());
        }
    }

    private void sendEmail(Notification notification) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (fromEmail != null && !fromEmail.isBlank()) {
                message.setFrom(fromEmail);
            }

            message.setTo(notification.getUser().getEmail());
            message.setSubject(notification.getTitle());
            message.setText(buildEmailBody(notification));
            mailSender.send(message);

            notification.setEmailSentAt(LocalDateTime.now());
            notificationRepository.save(notification);
        } catch (Exception ex) {
            LOGGER.warn("Email for notification {} could not be sent: {}", notification.getId(), ex.getMessage());
        }
    }

    private String buildEmailBody(Notification notification) {
        return "Descripción del evento: " + notification.getDescription() + "\n"
                + "Fecha y hora: "
                + notification.getEventAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private void pushRealtimeNotification(Notification notification) {
        Long userId = notification.getUser().getId();
        List<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(notification));
            } catch (IOException ex) {
                removeEmitter(userId, emitter);
            }
        }
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null) {
            return;
        }

        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByUser.remove(userId);
        }
    }
}
