package com.digitalassethub.medical.api.report;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Reports")
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {
    private final Map<String, String> reports = new ConcurrentHashMap<>();

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('COORDINATRICE','MEDECIN')")
    public Map<String, String> generate(@RequestBody Map<String, Object> payload) {
        String id = UUID.randomUUID().toString();
        reports.put(id, "Report generated for request: " + payload);
        return Map.of("id", id, "status", "READY");
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('COORDINATRICE','MEDECIN')")
    public ResponseEntity<ByteArrayResource> download(@PathVariable String id) {
        String content = reports.getOrDefault(id, "Report not found");
        ByteArrayResource resource = new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"report-" + id + ".txt\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(resource);
    }
}
