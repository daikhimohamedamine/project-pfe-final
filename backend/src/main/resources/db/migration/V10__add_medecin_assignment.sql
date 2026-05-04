ALTER TABLE `user` ADD COLUMN assigned_medecin_id BIGINT DEFAULT NULL;
ALTER TABLE `user` ADD CONSTRAINT fk_user_assigned_medecin FOREIGN KEY (assigned_medecin_id) REFERENCES `user`(id);

ALTER TABLE employee ADD COLUMN medecin_id BIGINT DEFAULT NULL;
ALTER TABLE employee ADD CONSTRAINT fk_employee_medecin FOREIGN KEY (medecin_id) REFERENCES `user`(id);

ALTER TABLE audit_log ADD COLUMN user_role VARCHAR(50);
ALTER TABLE audit_log ADD COLUMN medecin_context_id BIGINT;
