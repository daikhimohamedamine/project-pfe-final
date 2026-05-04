package com.digitalassethub.medical.api.document;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DoctorLibraryDocumentRepository extends JpaRepository<DoctorLibraryDocumentEntity, Long> {
    List<DoctorLibraryDocumentEntity> findByMedecinIdOrderByUploadedAtDesc(Long medecinId);
    List<DoctorLibraryDocumentEntity> findByMedecinIdAndCategorieOrderByUploadedAtDesc(Long medecinId, String categorie);
    List<DoctorLibraryDocumentEntity> findAllByOrderByUploadedAtDesc();
    List<DoctorLibraryDocumentEntity> findByCategorieOrderByUploadedAtDesc(String categorie);
}
