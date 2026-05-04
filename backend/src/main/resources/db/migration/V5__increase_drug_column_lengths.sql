-- Migration to increase the size of the dosage column in the drug table
ALTER TABLE drug MODIFY COLUMN dosage TEXT;
