-- Utilisateurs par défaut (Mot de passe: admin123)
-- Hash BCrypt pour 'admin123': $2a$10$8.UnVuG9HHgffUDAlk8q6uy5akLPNndzqBBl/+hYCm5W6mBrId.05

INSERT INTO `user` (email, password_hash, nom, prenom, role, telephone) VALUES
('admin@medzoon.com', '$2a$10$8.UnVuG9HHgffUDAlk8q6uy5akLPNndzqBBl/+hYCm5W6mBrId.05', 'Dhahri', 'Moetaz', 'ADMIN', '+21620123456'),
('docteur@medzoon.com', '$2a$10$8.UnVuG9HHgffUDAlk8q6uy5akLPNndzqBBl/+hYCm5W6mBrId.05', 'Trabelsi', 'Amine', 'MEDECIN', '+21621456789'),
('coord@medzoon.com', '$2a$10$8.UnVuG9HHgffUDAlk8q6uy5akLPNndzqBBl/+hYCm5W6mBrId.05', 'Gharbi', 'Sonia', 'COORDINATRICE', '+21622789123');

-- Employés Tunisiens
INSERT INTO employee (dossier_number, nom, prenom, date_naissance, telephone, gouvernorat, departement, poste_travail, matricule_paie, statut) VALUES
('EMP-2024-001', 'Ben Salem', 'Ahmed', '1985-05-12', '+21698123456', 'Tunis', 'Production', 'Opérateur Machine', 'MT-854', 'ACTIF'),
('EMP-2024-002', 'Mansour', 'Leila', '1992-08-25', '+21655654321', 'Ariana', 'Administration', 'Secrétaire', 'MT-921', 'ACTIF'),
('EMP-2024-003', 'Khediri', 'Mohamed', '1978-11-03', '+21620456123', 'Sousse', 'Logistique', 'Chauffeur', 'MT-783', 'ACTIF'),
('EMP-2024-004', 'Jendoubi', 'Sami', '1990-01-15', '+21644321987', 'Ben Arous', 'Maintenance', 'Technicien', 'MT-901', 'ACTIF'),
('EMP-2024-005', 'Ayari', 'Fatma', '1995-03-30', '+21697111222', 'Bizerte', 'Qualité', 'Ingénieur', 'MT-952', 'ACTIF'),
('EMP-2024-006', 'Mabrouk', 'Zied', '1988-07-12', '+21698123457', 'Sfax', 'Maintenance', 'Technicien Senior', 'MT-902', 'ACTIF'),
('EMP-2024-007', 'Hamdi', 'Ines', '1994-12-05', '+21655654322', 'Monastir', 'RH', 'Chargée Recrutement', 'MT-922', 'ACTIF');
