package com.digitalassethub.medical.api.drug;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Drugs")
@RestController
@RequestMapping("/api/v1/drugs")
public class DrugController {
    private final DrugRepository repository;

    public DrugController(DrugRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MEDECIN','COORDINATRICE','ADMIN')")
    public Page<DrugEntity> list(@RequestParam(defaultValue = "") String query, Pageable pageable) {
        return repository.searchFull(query, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MEDECIN','COORDINATRICE','ADMIN')")
    public DrugEntity get(@PathVariable Long id) {
        return repository.findById(id).orElseThrow();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATRICE')")
    public DrugEntity create(@RequestBody DrugEntity drug) {
        drug.setId(null);
        return repository.save(drug);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATRICE')")
    public DrugEntity update(@PathVariable Long id, @RequestBody DrugEntity payload) {
        DrugEntity drug = repository.findById(id).orElseThrow();
        drug.setDrugName(payload.getDrugName());
        drug.setGenericName(payload.getGenericName());
        drug.setDosage(payload.getDosage());
        drug.setIndications(payload.getIndications());
        drug.setImageLookupUrl(payload.getImageLookupUrl());
        drug.setSetId(payload.getSetId());
        drug.setSicknesses(payload.getSicknesses());
        drug.setActive(payload.isActive());
        return repository.save(drug);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','COORDINATRICE')")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
