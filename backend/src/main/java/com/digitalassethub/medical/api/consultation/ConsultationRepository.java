package com.digitalassethub.medical.api.consultation;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultationRepository extends JpaRepository<ConsultationEntity, Long> {
    Page<ConsultationEntity> findByEmployeeIdAndTypeContainingIgnoreCase(Long employeeId, String type, Pageable pageable);
    Page<ConsultationEntity> findByTypeContainingIgnoreCase(String type, Pageable pageable);
    
    Page<ConsultationEntity> findByMedecinIdAndTypeContainingIgnoreCase(Long medecinId, String type, Pageable pageable);
    Page<ConsultationEntity> findByMedecinIdAndEmployeeIdAndTypeContainingIgnoreCase(Long medecinId, Long employeeId, String type, Pageable pageable);

    Optional<ConsultationEntity> findTopByEmployeeIdAndTypeOrderByDateConsultationDesc(Long employeeId, String type);
    
    java.util.List<ConsultationEntity> findTop10ByEmployeeIdOrderByDateConsultationDesc(Long employeeId);
    long countByMedecinId(Long medecinId);
}
