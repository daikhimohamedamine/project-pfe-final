package com.digitalassethub.medical.api.employee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<EmployeeEntity, Long> {
    Optional<EmployeeEntity> findByDossierNumber(String dossierNumber);
    
    List<EmployeeEntity> findByNomContainingIgnoreCaseOrPrenomContainingIgnoreCaseOrPosteTravailContainingIgnoreCaseOrDepartementContainingIgnoreCase(
        String nom, String prenom, String posteTravail, String departement
    );

    Page<EmployeeEntity> findByNomContainingIgnoreCaseAndPrenomContainingIgnoreCase(
            String nom, String prenom, Pageable pageable
    );

    Page<EmployeeEntity> findByMedecinIdAndNomContainingIgnoreCaseAndPrenomContainingIgnoreCase(
            Long medecinId, String nom, String prenom, Pageable pageable
    );

    long countByMedecinId(Long medecinId);
}
