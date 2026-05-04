package com.digitalassethub.medical.api.ai;

import com.digitalassethub.medical.api.drug.DrugEntity;
import com.digitalassethub.medical.api.drug.DrugRepository;
import com.digitalassethub.medical.api.employee.EmployeeRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class AiRecommendationService {
    private final EmployeeRepository employeeRepository;
    private final DrugRepository drugRepository;

    private final com.digitalassethub.medical.api.ai.medassist.OpenRouterClient openRouterClient;

    public AiRecommendationService(EmployeeRepository employeeRepository, DrugRepository drugRepository,
                                com.digitalassethub.medical.api.ai.medassist.OpenRouterClient openRouterClient) {
        this.employeeRepository = employeeRepository;
        this.drugRepository = drugRepository;
        this.openRouterClient = openRouterClient;
    }

    public Map<String, Object> recommend(AiRecommendRequest request) {
        var employee = employeeRepository.findById(request.employeeId()).orElseThrow();
        List<DrugEntity> drugs = drugRepository.findAll();

        String systemPrompt = "Tu es un assistant médical tunisien expert. " +
                "Propose des médicaments UNIQUEMENT à partir de la liste fournie ci-dessous. " +
                "Retourne TOUJOURS ta réponse au format JSON strict (liste d'objets) avec les champs 'medicament', 'justification' et 'avertissement'. " +
                "Ne retourne RIEN d'autre que le JSON, sans blocs de code markdown.";

        String userPrompt = "Symptômes : " + request.symptoms() + "\n"
                + "Patient : " + employee.getNom() + " " + employee.getPrenom() + "\n"
                + "Médicaments autorisés : " + drugs.stream()
                .map(d -> d.getDrugName() + " (" + d.getGenericName() + "), dosage=" + d.getDosage() + ", indications=" + (d.getIndications() == null ? "" : d.getIndications()))
                .limit(20)
                .toList();

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));

        try {
            var result = openRouterClient.complete(messages);
            String content = result.content();
            // Nettoyage si l'IA a mis du markdown
            if (content.contains("```json")) {
                content = content.substring(content.indexOf("```json") + 7);
                if (content.contains("```")) {
                    content = content.substring(0, content.indexOf("```"));
                }
            } else if (content.contains("```")) {
                content = content.substring(content.indexOf("```") + 3);
                if (content.contains("```")) {
                    content = content.substring(0, content.indexOf("```"));
                }
            }
            
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(content.trim(), Map.class);
        } catch (Exception e) {
            return Map.of("error", "Erreur IA: " + e.getMessage());
        }
    }
}
