package com.digitalassethub.medical.api;  
import org.springframework.boot.CommandLineRunner;  
import org.springframework.stereotype.Component;  
import com.digitalassethub.medical.api.user.UserRepository;  
@Component public class CheckPasswords implements CommandLineRunner { private final UserRepository r; public CheckPasswords(UserRepository r) { this.r = r; } @Override public void run(String... args) throws Exception { r.findAll().forEach(u -> { System.out.println("USER: " + u.getEmail() + " HASH: " + u.getPasswordHash()); }); } } 
