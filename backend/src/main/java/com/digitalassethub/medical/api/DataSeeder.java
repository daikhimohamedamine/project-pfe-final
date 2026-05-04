package com.digitalassethub.medical.api;

import com.digitalassethub.medical.api.user.Role;
import com.digitalassethub.medical.api.user.UserEntity;
import com.digitalassethub.medical.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.count() == 0) {
            UserEntity admin = new UserEntity();
            admin.setEmail("admin@medzoon.health");
            admin.setPasswordHash(passwordEncoder.encode("password"));
            admin.setNom("Admin");
            admin.setPrenom("Super");
            admin.setRole(Role.ADMIN);
            admin.setEnabled(true);
            userRepository.save(admin);
            
            System.out.println("=========================================");
            System.out.println("Seeded Default User: admin@medzoon.health");
            System.out.println("Password: password");
            System.out.println("=========================================");
        }
    }
}
