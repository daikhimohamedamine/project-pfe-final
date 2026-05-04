package com.digitalassethub.medical.api.document;

import com.digitalassethub.medical.api.security.SecurityUser;
import com.digitalassethub.medical.api.user.UserEntity;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Tag(name = "Doctor Library")
@RestController
@RequestMapping("/api/v1/doctor-library")
public class DoctorLibraryController {

    private final DoctorLibraryRepository repository;
    private final com.digitalassethub.medical.api.user.UserRepository userRepository;
    private final Path root = Paths.get("uploads/library");

    public DoctorLibraryController(DoctorLibraryRepository repository, com.digitalassethub.medical.api.user.UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize folder for library!");
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_MEDECIN')")
    public List<DoctorLibraryEntity> list(@RequestParam(required = false) String categorie) {
        SecurityUser securityUser = (SecurityUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long doctorId = securityUser.user().getId();
        if (categorie != null && !categorie.isBlank()) {
            return repository.findByMedecinIdAndCategorieOrderByCreatedAtDesc(doctorId, categorie);
        }
        return repository.findByMedecinIdOrderByCreatedAtDesc(doctorId);
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('ROLE_MEDECIN')")
    public DoctorLibraryEntity upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("categorie") String categorie,
            @RequestParam(value = "description", required = false) String description) throws IOException {

        SecurityUser securityUser = (SecurityUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long doctorId = securityUser.user().getId();

        String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Files.copy(file.getInputStream(), this.root.resolve(filename));

        DoctorLibraryEntity entity = new DoctorLibraryEntity();
        entity.setTitre(file.getOriginalFilename());
        entity.setDescription(description);
        entity.setCategorie(categorie);
        entity.setFileName(filename);
        entity.setContentType(file.getContentType());
        entity.setFileSize(file.getSize());
        entity.setMedecinId(doctorId);

        return repository.save(entity);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws IOException {
        DoctorLibraryEntity entity = repository.findById(id).orElseThrow();
        Path path = root.resolve(entity.getFileName());
        Resource resource = new UrlResource(path.toUri());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(entity.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + entity.getTitre() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_MEDECIN')")
    public void delete(@PathVariable Long id) throws IOException {
        DoctorLibraryEntity entity = repository.findById(id).orElseThrow();
        Files.deleteIfExists(root.resolve(entity.getFileName()));
        repository.delete(entity);
    }

    @PostMapping("/avatar")
    @PreAuthorize("hasAuthority('ROLE_MEDECIN')")
    public java.util.Map<String, String> uploadAvatar(@RequestParam("file") MultipartFile file) throws IOException {
        SecurityUser securityUser = (SecurityUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserEntity user = userRepository.findById(securityUser.user().getId()).orElseThrow();

        String filename = "avatar_" + UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Files.copy(file.getInputStream(), this.root.resolve(filename));
        
        String url = "/api/v1/doctor-library/view/" + filename;
        user.setAvatarUrl(url);
        userRepository.save(user);

        return java.util.Map.of("url", url);
    }

    @GetMapping("/view/{filename}")
    public ResponseEntity<Resource> view(@PathVariable String filename) throws IOException {
        Path path = root.resolve(filename);
        Resource resource = new UrlResource(path.toUri());
        String contentType = Files.probeContentType(path);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : "image/jpeg"))
                .body(resource);
    }
}
