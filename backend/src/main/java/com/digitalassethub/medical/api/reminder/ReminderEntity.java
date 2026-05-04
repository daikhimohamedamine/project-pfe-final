package com.digitalassethub.medical.api.reminder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reminder")
public class ReminderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "employee_email")
    private String employeeEmail;

    private String type;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "date_echeance")
    @com.fasterxml.jackson.annotation.JsonAlias({"dueDate", "dateEcheance"})
    private LocalDate dateEcheance;

    private boolean envoye;
    
    @Column(name = "date_envoi")
    private LocalDateTime dateEnvoi;
    
    @Column(name = "created_by")
    private Long createdBy;

    @jakarta.persistence.Transient
    private boolean sendEmailNow;

    // Manual Getters and Setters
    public boolean isSendEmailNow() { return sendEmailNow; }
    public void setSendEmailNow(boolean sendEmailNow) { this.sendEmailNow = sendEmailNow; }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
    public String getEmployeeEmail() { return employeeEmail; }
    public void setEmployeeEmail(String employeeEmail) { this.employeeEmail = employeeEmail; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDate getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(LocalDate dateEcheance) { this.dateEcheance = dateEcheance; }
    public boolean isEnvoye() { return envoye; }
    public void setEnvoye(boolean envoye) { this.envoye = envoye; }
    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime dateEnvoi) { this.dateEnvoi = dateEnvoi; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
