package com.digitalassethub.medical.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MedicalApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedicalApiApplication.class, args);
    }
    @org.springframework.context.annotation.Bean
    public org.springframework.boot.CommandLineRunner logUserCount(com.digitalassethub.medical.api.user.UserRepository repository) {
        return args -> {
            long count = repository.count();
            System.out.println("====================================================");
            System.out.println("DATABASE CHECK: Number of users in DB: " + count);
            if (count > 0) {
                repository.findAll().forEach(u -> System.out.println("User in DB: " + u.getEmail() + " [Role: " + u.getRole() + "]"));
            } else {
                System.out.println("WARNING: DATABASE IS EMPTY! Check Flyway migrations.");
            }
            System.out.println("====================================================");
        };
    }
}
