-- Assignation des coordinatrices aux médecins
-- Sonia (coord@medzoon.com) -> Amine (docteur@medzoon.com)
UPDATE `user` 
SET assigned_medecin_id = (SELECT id FROM (SELECT id FROM `user` WHERE email = 'docteur@medzoon.com') as t)
WHERE email = 'coord@medzoon.com';

-- Fizou (fizou@medzoon.com) -> Touba (touba@medzoon.com)
UPDATE `user` 
SET assigned_medecin_id = (SELECT id FROM (SELECT id FROM `user` WHERE email = 'touba@medzoon.com') as t)
WHERE email = 'fizou@medzoon.com';

-- S'assurer que tous les employés sont assignés à un médecin (par défaut Touba ou Amine)
UPDATE employee SET medecin_id = (SELECT id FROM (SELECT id FROM `user` WHERE email = 'docteur@medzoon.com') as t) 
WHERE medecin_id IS NULL;
