-- Ajout des utilisateurs supplémentaires pour la démonstration
-- Utilisation de IGNORE pour ne pas bloquer si l'utilisateur existe déjà manuellement dans phpMyAdmin
INSERT IGNORE INTO `user` (email, password_hash, nom, prenom, role, telephone) VALUES
('touba@medzoon.com', '$2a$10$8.UnVuG9HHgffUDAlk8q6uy5akLPNndzqBBl/+hYCm5W6mBrId.05', 'Touba', 'Docteur', 'MEDECIN', '+21699000000'),
('hidea@medzoon.com', '$2a$10$8.UnVuG9HHgffUDAlk8q6uy5akLPNndzqBBl/+hYCm5W6mBrId.05', 'Hidea', 'User', 'MEDECIN', '+21699111111'),
('fizou@medzoon.com', '$2a$10$8.UnVuG9HHgffUDAlk8q6uy5akLPNndzqBBl/+hYCm5W6mBrId.05', 'Fizou', 'User', 'COORDINATRICE', '+21699222222'),
('wiem@medzoon.com', '$2a$10$8.UnVuG9HHgffUDAlk8q6uy5akLPNndzqBBl/+hYCm5W6mBrId.05', 'Wiem', 'User', 'MEDECIN', '+21699333333');

-- Mise à jour des assignations si nécessaire
UPDATE employee SET medecin_id = (SELECT id FROM `user` WHERE email = 'touba@medzoon.com') 
WHERE medecin_id IS NULL AND EXISTS (SELECT 1 FROM `user` WHERE email = 'touba@medzoon.com');
