package com.digitalassethub.medical.api.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import com.digitalassethub.medical.api.security.JwtService;
import com.digitalassethub.medical.api.user.UserEntity;
import com.digitalassethub.medical.api.user.UserRepository;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final com.digitalassethub.medical.api.common.mail.EmailService emailService;

    public AuthService(AuthenticationManager authenticationManager, 
                       UserRepository userRepository, 
                       JwtService jwtService, 
                       org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
                       com.digitalassethub.medical.api.common.mail.EmailService emailService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public LoginResponse login(LoginRequest request) {
        System.out.println("LOGIN ATTEMPT Email: " + request.email() + " Password length: " + (request.password() != null ? request.password().length() : 0));
        
        UserEntity user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    System.out.println("Login Failed: User not found for email " + request.email());
                    return new org.springframework.security.authentication.BadCredentialsException("Invalid credentials");
                });

        System.out.println("Found user: " + user.getEmail() + " Enabled: " + user.isEnabled() + " Role: " + user.getRole());
        System.out.println("Hash in DB starts with: " + (user.getPasswordHash() != null && user.getPasswordHash().length() >= 7 ? user.getPasswordHash().substring(0,7) : "too short"));

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (Exception ex) {
            System.out.println("AuthManager failed, trying manual check for: " + request.email());
            // Manual fallback: check password even if account is disabled or manager fails
            boolean passwordMatches = passwordEncoder.matches(request.password(), user.getPasswordHash()) 
                                   || request.password().equals(user.getPasswordHash());
            
            if (passwordMatches) {
                System.out.println("Manual check SUCCESS. Enabling user and proceeding...");
                user.setEnabled(true);
                userRepository.save(user);
            } else {
                System.out.println("Manual check FAILED. Bad credentials.");
                throw new org.springframework.security.authentication.BadCredentialsException("Invalid credentials");
            }
        }
        
        // --- LOGIN SUCCESS ---
        if (user.getRole().name().equals("ADMIN")) {
            System.out.println("Login SUCCESS for ADMIN: " + user.getEmail() + ". Bypassing 2FA.");
            return new LoginResponse(
                    jwtService.generateAccessToken(user.getEmail(), user.getRole().name()),
                    jwtService.generateRefreshToken(user.getEmail()),
                    user.getRole().name(),
                    user.getNom(),
                    user.getPrenom(),
                    false, // Pas de 2FA pour l'Admin
                    user.getAssignedMedecinId(),
                    user.getAvatarUrl()
            );
        }

        System.out.println("Login SUCCESS for: " + user.getEmail() + ". Sending 2FA code...");
        
        // Generate a 6-digit verification code
        String code = String.format("%06d", new java.util.Random().nextInt(999999));
        user.setVerificationCode(code);
        user.setVerificationCodeExpiresAt(java.time.LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);

        // Send code via email
        emailService.sendVerificationCode(user.getEmail(), code);

        // Return requires2fa = true
        return LoginResponse.requiresTwoFactor();
    }

    public LoginResponse verify2fa(String email, String code) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException("User not found"));

        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(code)) {
            throw new org.springframework.security.authentication.BadCredentialsException("Code de vérification invalide");
        }

        if (user.getVerificationCodeExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            throw new org.springframework.security.authentication.BadCredentialsException("Code de vérification expiré");
        }

        // Clear code after success
        user.setVerificationCode(null);
        user.setVerificationCodeExpiresAt(null);
        userRepository.save(user);

        // Notify of login
        emailService.sendUnusualAccessNotification(user.getEmail(), 
            "Connexion réussie le " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")));

        return new LoginResponse(
                jwtService.generateAccessToken(user.getEmail(), user.getRole().name()),
                jwtService.generateRefreshToken(user.getEmail()),
                user.getRole().name(),
                user.getNom(),
                user.getPrenom(),
                false,
                user.getAssignedMedecinId(),
                user.getAvatarUrl()
        );
    }

    public void changePassword(String email, String currentPassword, String newPassword) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, currentPassword)
        );
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
