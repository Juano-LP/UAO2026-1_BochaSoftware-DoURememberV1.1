package com.springboot.backend.salazar.usersbackend.users_backend.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.springboot.backend.salazar.usersbackend.users_backend.entities.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(Long userId);

    Optional<Notification> findByUserIdAndEventKey(Long userId, String eventKey);
}