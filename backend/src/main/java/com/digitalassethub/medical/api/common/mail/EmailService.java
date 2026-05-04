package com.digitalassethub.medical.api.common.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username:no-reply@medzoon.com}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendWelcomeEmail(String to, String name, String tempPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Bienvenue sur MediCab - Vos identifiants");
        message.setText("Bonjour " + name + ",\n\n" +
                "Votre compte a été créé avec succès sur la plateforme MediCab.\n\n" +
                "Vos identifiants temporaires :\n" +
                "Email : " + to + "\n" +
                "Mot de passe : " + tempPassword + "\n\n" +
                "Veuillez changer votre mot de passe lors de votre première connexion.\n\n" +
                "L'équipe MediCab.");
        try {
            mailSender.send(message);
            log.info("Welcome email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", to, e.getMessage());
        }
    }

    public void sendAppointmentReminder(String to, String patientName, String date, String time) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Rappel de rendez-vous - MediCab");
        message.setText("Bonjour " + patientName + ",\n\n" +
                "Ceci est un rappel pour votre rendez-vous médical prévu le " + date + " à " + time + ".\n\n" +
                "En cas d'empêchement, merci de nous prévenir au plus vite.\n\n" +
                "L'équipe MediCab.");
        try {
            mailSender.send(message);
            log.info("Appointment reminder sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send reminder to {}: {}", to, e.getMessage());
        }
    }

    public void sendCustomReminder(String to, String customMessage) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Rappel de rendez-vous - MediCab");
        
        // On rajoute la signature MediCab à la fin si elle n'y est pas
        String body = customMessage;
        if (!body.contains("MediCab")) {
            body += "\n\nL'équipe MediCab";
        }
        
        message.setText(body);
        try {
            mailSender.send(message);
            log.info("Custom reminder sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send custom reminder to {}: {}", to, e.getMessage());
        }
    }

    public void sendVerificationCode(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Code de vérification MediCab");
        message.setText("Bonjour,\n\n" +
                "Votre code de vérification est : " + code + "\n\n" +
                "Ce code expirera dans 10 minutes.\n\n" +
                "Si vous n'êtes pas à l'origine de cette demande, veuillez ignorer cet email.\n\n" +
                "L'équipe MediCab.");
        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send verification code to {}", to);
        }
    }

    public void sendUnusualAccessNotification(String to, String details) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Alerte de sécurité - Connexion MediCab");
        message.setText("Bonjour,\n\n" +
                "Une connexion réussie à votre compte MediCab a été détectée.\n\n" +
                "Détails : " + details + "\n\n" +
                "Si c'était vous, vous pouvez ignorer cet email. Sinon, nous vous recommandons de changer votre mot de passe immédiatement.\n\n" +
                "L'équipe MediCab.");
        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send security alert to {}", to);
        }
    }
}
