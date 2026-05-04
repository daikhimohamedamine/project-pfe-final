package com.digitalassethub.medical.api.drug;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DrugRepository extends JpaRepository<DrugEntity, Long> {
    @Query(value = "SELECT * FROM drug WHERE active = true AND (LOCATE(:q, drug_name) > 0 OR LOCATE(:q, generic_name) > 0 OR LOCATE(:q, sicknesses) > 0)", nativeQuery = true)
    Page<DrugEntity> searchFull(@Param("q") String q, Pageable pageable);

    Page<DrugEntity> findByDrugNameContainingIgnoreCaseOrGenericNameContainingIgnoreCase(
            String drugName, String genericName, Pageable pageable
    );

    @Query(value = "SELECT * FROM drug WHERE drug_name LIKE %:q% OR generic_name LIKE %:q% OR indications LIKE %:q% LIMIT 5", nativeQuery = true)
    java.util.List<DrugEntity> searchByKeyword(@Param("q") String q);
}
