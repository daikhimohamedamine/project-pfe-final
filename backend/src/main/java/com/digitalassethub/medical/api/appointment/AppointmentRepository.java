package com.digitalassethub.medical.api.appointment;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentRepository extends JpaRepository<AppointmentEntity, Long> {
    List<AppointmentEntity> findByDateDebutBetween(LocalDateTime from, LocalDateTime to);
    List<AppointmentEntity> findByMedecinIdAndDateDebutBetween(Long medecinId, LocalDateTime from, LocalDateTime to);
}
