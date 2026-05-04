package com.digitalassethub.medical.api.document;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {
    List<DocumentEntity> findByConsultationId(Long consultationId);
    List<DocumentEntity> findByEmployeeId(Long employeeId);
}
