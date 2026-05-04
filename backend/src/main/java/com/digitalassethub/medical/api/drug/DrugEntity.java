package com.digitalassethub.medical.api.drug;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "drug")
public class DrugEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "drug_name", columnDefinition = "TEXT")
    private String drugName;

    @Column(name = "generic_name", columnDefinition = "TEXT")
    private String genericName;

    @Column(columnDefinition = "TEXT")
    private String dosage;
    private String indications;

    @Column(name = "image_lookup_url")
    private String imageLookupUrl;

    @Column(name = "set_id")
    private String setId;

    @Column(columnDefinition = "TEXT")
    private String sicknesses;

    private boolean active = true;

    // Manual Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDrugName() { return drugName; }
    public void setDrugName(String drugName) { this.drugName = drugName; }
    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public String getIndications() { return indications; }
    public void setIndications(String indications) { this.indications = indications; }
    public String getImageLookupUrl() { return imageLookupUrl; }
    public void setImageLookupUrl(String imageLookupUrl) { this.imageLookupUrl = imageLookupUrl; }
    public String getSetId() { return setId; }
    public void setSetId(String setId) { this.setId = setId; }
    public String getSicknesses() { return sicknesses; }
    public void setSicknesses(String sicknesses) { this.sicknesses = sicknesses; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
