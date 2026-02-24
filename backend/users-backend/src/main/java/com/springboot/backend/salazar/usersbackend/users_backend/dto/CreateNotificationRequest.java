package com.springboot.backend.salazar.usersbackend.users_backend.dto;

import java.time.LocalDateTime;

import com.springboot.backend.salazar.usersbackend.users_backend.enums.NotificationType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateNotificationRequest {

    @NotNull
    private Long userId;

    @NotBlank
    @Size(max = 120)
    private String eventKey;

    @NotNull
    private NotificationType type;

    @NotBlank
    @Size(max = 180)
    private String title;

    @NotBlank
    @Size(max = 500)
    private String description;

    private LocalDateTime eventAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getEventKey() {
        return eventKey;
    }

    public void setEventKey(String eventKey) {
        this.eventKey = eventKey;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getEventAt() {
        return eventAt;
    }

    public void setEventAt(LocalDateTime eventAt) {
        this.eventAt = eventAt;
    }
}