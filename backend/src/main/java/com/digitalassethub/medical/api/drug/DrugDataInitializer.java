package com.digitalassethub.medical.api.drug;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class DrugDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DrugDataInitializer.class);
    private final DrugRepository drugRepository;

    public DrugDataInitializer(DrugRepository drugRepository) {
        this.drugRepository = drugRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (drugRepository.count() > 0) {
            log.info("Drug table already populated. Skipping import.");
            return;
        }

        log.info("Starting drug dataset import from CSV...");
        try (Reader in = new InputStreamReader(new ClassPathResource("final_drug_dataset_with_sicknesses.csv").getInputStream(), StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreHeaderCase(true)
                    .setTrim(true)
                    .build()
                    .parse(in);

            for (CSVRecord record : records) {
                DrugEntity drug = new DrugEntity();
                drug.setDrugName(record.get("drug_name"));
                drug.setGenericName(record.get("generic_name"));
                drug.setDosage(record.get("dosage"));
                drug.setIndications(record.get("indications"));
                drug.setImageLookupUrl(record.get("image_lookup_url"));
                drug.setSetId(record.get("set_id"));
                
                String sicknessStr = record.get("sicknesses");
                drug.setSicknesses((sicknessStr != null && !sicknessStr.isBlank()) ? sicknessStr.trim() : null);
                
                drug.setActive(true);
                drugRepository.save(drug);
            }
            log.info("Drug dataset import completed.");
        } catch (Exception e) {
            log.error("Failed to import drugs: {}", e.getMessage());
        }
    }
}
