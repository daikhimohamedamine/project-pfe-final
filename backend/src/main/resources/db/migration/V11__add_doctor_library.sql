CREATE TABLE IF NOT EXISTS doctor_library_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    medecin_id BIGINT NOT NULL,
    nom_fichier VARCHAR(255) NOT NULL,
    chemin_stockage VARCHAR(500) NOT NULL,
    type_mime VARCHAR(100),
    taille BIGINT,
    categorie VARCHAR(50), -- 'CERTIFICAT', 'EXPERIENCE', 'LIVRE', 'AUTRE'
    uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    description TEXT,
    CONSTRAINT fk_library_medecin FOREIGN KEY (medecin_id) REFERENCES `user`(id)
);
