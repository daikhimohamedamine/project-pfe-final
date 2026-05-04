package com.digitalassethub.medical.api.document;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Documents")
@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {
    private final DocumentRepository repository;

    @Value("${app.storage.documents:./backend-storage/documents}")
    private String storagePath;

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('COORDINATRICE','MEDECIN','ADMIN')")
    public DocumentEntity upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("employeeId") Long employeeId,
            @RequestParam(value = "consultationId", required = false) Long consultationId
    ) throws IOException {
        Files.createDirectories(Path.of(storagePath));
        String filename = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path target = Path.of(storagePath, filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        DocumentEntity entity = new DocumentEntity();
        entity.setEmployeeId(employeeId);
        entity.setConsultationId(consultationId);
        entity.setNomFichier(file.getOriginalFilename());
        entity.setCheminStockage(target.toAbsolutePath().toString());
        entity.setTypeMime(file.getContentType());
        entity.setTaille(file.getSize());
        return repository.save(entity);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COORDINATRICE','MEDECIN','ADMIN')")
    public List<DocumentEntity> list(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long consultationId
    ) {
        if (consultationId != null) {
            return repository.findByConsultationId(consultationId);
        }
        if (employeeId != null) {
            return repository.findByEmployeeId(employeeId);
        }
        return List.of();
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('COORDINATRICE','MEDECIN','ADMIN')")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        DocumentEntity document = repository.findById(id).orElseThrow();
        Resource resource = new FileSystemResource(document.getCheminStockage());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getTypeMime() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : document.getTypeMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + document.getNomFichier() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('COORDINATRICE','MEDECIN','ADMIN')")
    public void delete(@PathVariable Long id) throws IOException {
        DocumentEntity document = repository.findById(id).orElseThrow();
        Files.deleteIfExists(Path.of(document.getCheminStockage()));
        repository.deleteById(id);
    }
}
