-- Add missing fields to reminder table
ALTER TABLE reminder ADD COLUMN employee_email VARCHAR(255);
ALTER TABLE reminder ADD COLUMN message TEXT;
