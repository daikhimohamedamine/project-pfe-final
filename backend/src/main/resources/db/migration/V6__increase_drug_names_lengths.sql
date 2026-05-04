-- Migration to increase the size of drug_name and generic_name columns
ALTER TABLE drug MODIFY COLUMN drug_name TEXT;
ALTER TABLE drug MODIFY COLUMN generic_name TEXT;
