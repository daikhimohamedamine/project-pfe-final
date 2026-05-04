ALTER TABLE employee
ADD COLUMN antecedents_chirurgicaux TEXT,
ADD COLUMN antecedents_medicaux TEXT,
ADD COLUMN antecedents_gynecologiques TEXT,
ADD COLUMN antecedents_hereditaires TEXT,
ADD COLUMN tabac BOOLEAN DEFAULT FALSE,
ADD COLUMN alcool BOOLEAN DEFAULT FALSE,
ADD COLUMN automedication BOOLEAN DEFAULT FALSE;
