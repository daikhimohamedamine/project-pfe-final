package com.digitalassethub.medical.api;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.digitalassethub.medical.api.user.UserRepository;

@Component
public class PasswordFixer implements CommandLineRunner {
    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public PasswordFixer(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) throws Exception {
        repo.findByEmail("admin@medzoon.com").ifPresent(u -> {
            if (!encoder.matches("admin123", u.getPasswordHash())) {
                u.setPasswordHash(encoder.encode("admin123"));
                repo.save(u);
                System.out.println("SUCCESSFULLY RESET ADMIN PASSWORD IN DATABASE.");
            }
        });
        repo.findByEmail("docteur@medzoon.com").ifPresent(u -> {
            if (!encoder.matches("admin123", u.getPasswordHash())) {
                u.setPasswordHash(encoder.encode("admin123"));
                repo.save(u);
            }
        });
        repo.findByEmail("coord@medzoon.com").ifPresent(u -> {
            if (!encoder.matches("admin123", u.getPasswordHash())) {
                u.setPasswordHash(encoder.encode("admin123"));
                repo.save(u);
            }
        });
    }
}
