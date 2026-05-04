package com.digitalassethub.medical.api.employee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee")
public class EmployeeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dossier_number", nullable = false, unique = true)
    private String dossierNumber;

    private String nom;
    private String prenom;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(name = "lieu_naissance")
    private String lieuNaissance;

    private String telephone;
    private String gouvernorat;

    @Column(name = "situation_famille")
    private String situationFamiliale;

    @Column(name = "nb_enfants")
    private Integer nombreEnfants;

    private String adresse;

    @Column(name = "code_postal")
    private String codePostal;

    private String departement;

    @Column(name = "poste_travail")
    private String posteTravail;

    @Column(name = "date_embauche")
    private LocalDate dateEmbauche;

    @Column(name = "matricule_caisse")
    private String matriculeCaisse;

    private String email;

    @Column(name = "statut")
    private String statut = "ACTIF";

    @Column(name = "antecedents_chirurgicaux", length = 2000)
    private String antecedentsChirurgicaux;

    @Column(name = "antecedents_medicaux", length = 2000)
    private String antecedentsMedicaux;

    @Column(name = "antecedents_gynecologiques", length = 2000)
    private String antecedentsGynecologiques;

    @Column(name = "antecedents_hereditaires", length = 2000)
    private String antecedentsHereditaires;

    private Boolean tabac = false;
    private Boolean alcool = false;
    private Boolean automedication = false;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "medecin_id")
    private Long medecinId;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDossierNumber() { return dossierNumber; }
    public void setDossierNumber(String dossierNumber) { this.dossierNumber = dossierNumber; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }
    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }
    public String getLieuNaissance() { return lieuNaissance; }
    public void setLieuNaissance(String lieuNaissance) { this.lieuNaissance = lieuNaissance; }
    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
    public String getGouvernorat() { return gouvernorat; }
    public void setGouvernorat(String gouvernorat) { this.gouvernorat = gouvernorat; }
    public String getSituationFamiliale() { return situationFamiliale; }
    public void setSituationFamiliale(String situationFamiliale) { this.situationFamiliale = situationFamiliale; }
    public Integer getNombreEnfants() { return nombreEnfants; }
    public void setNombreEnfants(Integer nombreEnfants) { this.nombreEnfants = nombreEnfants; }
    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
    public String getCodePostal() { return codePostal; }
    public void setCodePostal(String codePostal) { this.codePostal = codePostal; }
    public String getDepartement() { return departement; }
    public void setDepartement(String departement) { this.departement = departement; }
    public String getPosteTravail() { return posteTravail; }
    public void setPosteTravail(String posteTravail) { this.posteTravail = posteTravail; }
    public LocalDate getDateEmbauche() { return dateEmbauche; }
    public void setDateEmbauche(LocalDate dateEmbauche) { this.dateEmbauche = dateEmbauche; }
    public String getMatriculeCaisse() { return matriculeCaisse; }
    public void setMatriculeCaisse(String matriculeCaisse) { this.matriculeCaisse = matriculeCaisse; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
    public String getAntecedentsChirurgicaux() { return antecedentsChirurgicaux; }
    public void setAntecedentsChirurgicaux(String antecedentsChirurgicaux) { this.antecedentsChirurgicaux = antecedentsChirurgicaux; }
    public String getAntecedentsMedicaux() { return antecedentsMedicaux; }
    public void setAntecedentsMedicaux(String antecedentsMedicaux) { this.antecedentsMedicaux = antecedentsMedicaux; }
    public String getAntecedentsGynecologiques() { return antecedentsGynecologiques; }
    public void setAntecedentsGynecologiques(String antecedentsGynecologiques) { this.antecedentsGynecologiques = antecedentsGynecologiques; }
    public String getAntecedentsHereditaires() { return antecedentsHereditaires; }
    public void setAntecedentsHereditaires(String antecedentsHereditaires) { this.antecedentsHereditaires = antecedentsHereditaires; }
    public Boolean getTabac() { return tabac; }
    public void setTabac(Boolean tabac) { this.tabac = tabac; }
    public Boolean getAlcool() { return alcool; }
    public void setAlcool(Boolean alcool) { this.alcool = alcool; }
    public Boolean getAutomedication() { return automedication; }
    public void setAutomedication(Boolean automedication) { this.automedication = automedication; }
    public LocalDateTime getArchivedAt() { return archivedAt; }
    public void setArchivedAt(LocalDateTime archivedAt) { this.archivedAt = archivedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Long getMedecinId() { return medecinId; }
    public void setMedecinId(Long medecinId) { this.medecinId = medecinId; }
}
