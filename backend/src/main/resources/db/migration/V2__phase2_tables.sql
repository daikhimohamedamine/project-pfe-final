CREATE TABLE IF NOT EXISTS appointment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    medecin_id BIGINT NULL,
    date_debut DATETIME NOT NULL,
    date_fin DATETIME NOT NULL,
    type_visite VARCHAR(50),
    statut ENUM('PLANIFIE','ANNULE','EFFECTUE') DEFAULT 'PLANIFIE',
    notes TEXT,
    created_by BIGINT,
    CONSTRAINT fk_appointment_employee FOREIGN KEY (employee_id) REFERENCES employee(id),
    CONSTRAINT fk_appointment_medecin FOREIGN KEY (medecin_id) REFERENCES `user`(id),
    CONSTRAINT fk_appointment_created_by FOREIGN KEY (created_by) REFERENCES `user`(id)
);

CREATE TABLE IF NOT EXISTS document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    consultation_id BIGINT NULL,
    nom_fichier VARCHAR(255),
    chemin_stockage VARCHAR(500),
    type_mime VARCHAR(100),
    taille BIGINT,
    uploaded_by BIGINT,
    uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_document_employee FOREIGN KEY (employee_id) REFERENCES employee(id),
    CONSTRAINT fk_document_consultation FOREIGN KEY (consultation_id) REFERENCES consultation(id),
    CONSTRAINT fk_document_uploaded_by FOREIGN KEY (uploaded_by) REFERENCES `user`(id)
);
