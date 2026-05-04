-- Assignation des employés au Docteur Touba (ID 4)
UPDATE employee SET medecin_id = 4 WHERE medecin_id IS NULL;

-- Ajout de quelques consultations pour remplir l'espace
INSERT INTO consultation (employee_id, medecin_id, type, date_consultation, poids, taille, details) VALUES
(1, 4, 'EMBAUCHE', '2024-01-10', 75.5, 180, '{"diagnostic": "Apte", "observations": "Bonne santé générale"}'),
(2, 4, 'PERIODIQUE', '2024-02-15', 62.0, 165, '{"diagnostic": "Apte", "observations": "RAS"}'),
(3, 4, 'SOIN', '2024-03-20', 88.0, 178, '{"diagnostic": "Grippe", "observations": "Repos 3 jours"}');
