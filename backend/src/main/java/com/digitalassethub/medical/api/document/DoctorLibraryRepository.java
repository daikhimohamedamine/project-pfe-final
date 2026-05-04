package com.digitalassethub.medical.api.document;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DoctorLibraryRepository extends JpaRepository<DoctorLibraryEntity, Long> {
    List<DoctorLibraryEntity> findByMedecinIdOrderByCreatedAtDesc(Long medecinId);
    List<DoctorLibraryEntity> findByMedecinIdAndCategorieOrderByCreatedAtDesc(Long medecinId, String categorie);
}
