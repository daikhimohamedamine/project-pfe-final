package com.digitalassethub.medical.api.notification;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLogEntity, Long> {
}
