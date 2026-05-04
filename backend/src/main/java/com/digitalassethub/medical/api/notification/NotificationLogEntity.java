package com.digitalassethub.medical.api.notification;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "notification_log")
public class NotificationLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String destinataireEmail;
    private String sujet;
    private String contenu;
    private LocalDateTime dateEnvoi;
    private String statut;

    @PrePersist
    void prePersist() {
        dateEnvoi = LocalDateTime.now();
    }
}
