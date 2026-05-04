CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nom VARCHAR(100),
    prenom VARCHAR(100),
    role ENUM('ADMIN','COORDINATRICE','MEDECIN') NOT NULL,
    telephone VARCHAR(20),
    enabled BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS employee (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dossier_number VARCHAR(50) NOT NULL UNIQUE,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    date_naissance DATE NOT NULL,
    lieu_naissance VARCHAR(150),
    telephone VARCHAR(20),
    gouvernorat VARCHAR(50),
    situation_famille VARCHAR(50),
    nb_enfants INT DEFAULT 0,
    adresse VARCHAR(255),
    code_postal VARCHAR(10),
    departement VARCHAR(100),
    poste_travail VARCHAR(150),
    date_embauche DATE,
    matricule_paie VARCHAR(50),
    matricule_caisse VARCHAR(50),
    email VARCHAR(255),
    statut ENUM('ACTIF','ARCHIVE') DEFAULT 'ACTIF',
    created_by BIGINT,
    updated_by BIGINT,
    archived_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_employee_created_by FOREIGN KEY (created_by) REFERENCES `user` (id),
    CONSTRAINT fk_employee_updated_by FOREIGN KEY (updated_by) REFERENCES `user` (id)
);

CREATE TABLE IF NOT EXISTS consultation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    medecin_id BIGINT NOT NULL,
    type ENUM('EMBAUCHE','PERIODIQUE','SOIN','SPONTANEE','REPRISE') NOT NULL,
    date_consultation DATE NOT NULL,
    poids DECIMAL(5,2),
    taille DECIMAL(5,2),
    details JSON,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_consult_employee FOREIGN KEY (employee_id) REFERENCES employee(id),
    CONSTRAINT fk_consult_medecin FOREIGN KEY (medecin_id) REFERENCES `user`(id)
);

CREATE TABLE IF NOT EXISTS reminder (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    type ENUM('PERIODIQUE','MANUEL') NOT NULL,
    date_echeance DATE NOT NULL,
    envoye BOOLEAN DEFAULT FALSE,
    date_envoi DATETIME,
    created_by BIGINT,
    CONSTRAINT fk_reminder_employee FOREIGN KEY (employee_id) REFERENCES employee(id),
    CONSTRAINT fk_reminder_user FOREIGN KEY (created_by) REFERENCES `user`(id)
);

CREATE TABLE IF NOT EXISTS drug (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    drug_name TEXT,
    generic_name TEXT,
    dosage TEXT,
    indications TEXT,
    image_lookup_url VARCHAR(500),
    set_id VARCHAR(100),
    sicknesses TEXT,
    active BOOLEAN DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(100),
    entity_type VARCHAR(50),
    entity_id BIGINT,
    details VARCHAR(500),
    ip_address VARCHAR(45),
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS notification_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    destinataire_email VARCHAR(255),
    sujet VARCHAR(255),
    contenu TEXT,
    date_envoi DATETIME DEFAULT CURRENT_TIMESTAMP,
    statut ENUM('SUCCESS','FAILED') NOT NULL
);
