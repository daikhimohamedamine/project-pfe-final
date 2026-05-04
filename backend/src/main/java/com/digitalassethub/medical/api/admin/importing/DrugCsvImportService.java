package com.digitalassethub.medical.api.admin.importing;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.digitalassethub.medical.api.drug.DrugEntity;
import com.digitalassethub.medical.api.drug.DrugRepository;

@Service
public class DrugCsvImportService {
    private final DrugRepository repository;

    public DrugCsvImportService(DrugRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ImportResult importCsv(InputStream in, boolean clearBefore) {
        if (clearBefore) {
            repository.deleteAllInBatch();
        }

        int created = 0;
        int skipped = 0;
        List<String> errors = new java.util.ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            CSVParser parser = CSVFormat.DEFAULT
                    .builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreSurroundingSpaces(true)
                    .build()
                    .parse(reader);

            for (CSVRecord record : parser) {
                try {
                    var map = record.toMap();
                    var fixed = new LinkedHashMap<>(map);

                    String drugName = fixed.getOrDefault("drug_name", "");
                    String genericName = fixed.getOrDefault("generic_name", "");
                    String dosage = fixed.getOrDefault("dosage", "");
                    String indications = fixed.getOrDefault("indications", "");
                    String imageLookupUrl = fixed.getOrDefault("image_lookup_url", "");
                    String setId = fixed.getOrDefault("set_id", "");
                    String sicknessRaw = fixed.getOrDefault("sicknesses", "");

                    if ((drugName == null || drugName.isBlank()) && (setId == null || setId.isBlank())) {
                        skipped++;
                        continue;
                    }

                    DrugEntity e = new DrugEntity();
                    e.setDrugName(nullIfBlank(drugName));
                    e.setGenericName(nullIfBlank(genericName));
                    e.setDosage(nullIfBlank(dosage));
                    e.setIndications(nullIfBlank(indications));
                    e.setImageLookupUrl(nullIfBlank(imageLookupUrl));
                    e.setSetId(nullIfBlank(setId));
                    e.setActive(true);
                    e.setSicknesses(nullIfBlank(sicknessRaw));
                    repository.save(e);
                    created++;
                } catch (Exception inner) {
                    errors.add("line " + record.getRecordNumber() + ": " + inner.getMessage());
                }
            }
        } catch (Exception ex) {
            errors.add(ex.getMessage());
        }

        return new ImportResult(created, skipped, errors);
    }

    private static String nullIfBlank(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }


    public record ImportResult(int created, int skipped, List<String> errors) {}
}
