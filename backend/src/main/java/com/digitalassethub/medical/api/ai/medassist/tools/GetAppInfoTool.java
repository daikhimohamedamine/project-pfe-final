package com.digitalassethub.medical.api.ai.medassist.tools;

import com.digitalassethub.medical.api.ai.medassist.Tool;
import com.digitalassethub.medical.api.ai.medassist.ToolResult;
import com.digitalassethub.medical.api.user.Role;
import com.digitalassethub.medical.api.user.UserEntity;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Returns documentation about the COFICAB Medical platform: features by role,
 * navigation paths, terminology, and "how do I…" guidance.
 *
 * The model uses this to answer non-clinical, application-level questions
 * (e.g. "How do I add an employee?", "Where is the audit log?", "What can a
 * coordinatrice do?") without inventing answers.
 */
@Component
public class GetAppInfoTool implements Tool {

    @Override
    public String name() { return "get_app_info"; }

    @Override
    public ToolResult execute(Map<String, Object> input, UserEntity user) {
        String topic = input.get("topic") == null ? "overview" : input.get("topic").toString().toLowerCase().trim();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("topic", topic);
        data.put("platform", "COFICAB Medical / MedZoon — occupational-health management platform (Tunis, Tunisie)");

        switch (topic) {
            case "navigation", "menu" -> data.put("content", navigationByRole());
            case "admin" -> data.put("content", adminFeatures());
            case "coordinatrice", "coordinator", "coord" -> data.put("content", coordFeatures());
            case "doctor", "medecin", "médecin" -> data.put("content", doctorFeatures());
            case "employee", "employees", "patient", "patients" -> data.put("content", employeeModule());
            case "consultation", "consultations" -> data.put("content", consultationsModule());
            case "appointment", "appointments", "rendez-vous", "schedule" -> data.put("content", appointmentsModule());
            case "reminder", "reminders", "rappel", "rappels" -> data.put("content", remindersModule());
            case "audit", "logs" -> data.put("content", auditModule());
            case "drugs", "library", "bibliotheque", "bibliothèque" -> data.put("content", drugLibraryModule());
            case "auth", "login", "security", "2fa" -> data.put("content", authModule());
            case "vaccines", "vaccinations" -> data.put("content", vaccinationsModule());
            case "settings", "parametres", "paramètres" -> data.put("content", settingsModule());
            case "roles", "permissions" -> data.put("content", rolesAndPermissions());
            default -> data.put("content", overview(user.getRole()));
        }

        data.put("available_topics", List.of(
                "overview", "navigation", "admin", "coordinatrice", "doctor",
                "employees", "consultations", "appointments", "reminders",
                "audit", "drugs", "auth", "vaccines", "settings", "roles"
        ));
        return ToolResult.ok(data);
    }

    private Map<String, Object> overview(Role role) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", "MedZoon — Plateforme médicale COFICAB");
        m.put("purpose", "Gestion de la médecine du travail : dossiers employés, consultations, ordonnances, rendez-vous, vaccinations et rappels.");
        m.put("user_roles", List.of(
                "ADMIN — administration globale (utilisateurs, audit, paramètres)",
                "COORDINATRICE — coordination des soins (employés, rendez-vous, rappels)",
                "MEDECIN — actes cliniques (consultations, ordonnances, notes SOAP, vaccinations)"
        ));
        m.put("your_role", role.name());
        m.put("modules", List.of(
                "Employés / Patients (dossiers médicaux)",
                "Consultations (visites, ordonnances, notes SOAP)",
                "Rendez-vous (planning)",
                "Rappels (suivi automatisé)",
                "Vaccinations (calendrier vaccinal)",
                "Bibliothèque médicamenteuse",
                "Audit (journal des actions, ADMIN)",
                "Paramètres (ADMIN)"
        ));
        m.put("ai_assistant", "MedAssist — assistant IA agentique avec accès en lecture/écriture limitée à la base via outils contrôlés par rôle.");
        return m;
    }

    private Map<String, Object> navigationByRole() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("admin", List.of(
                "/dashboard/admin — Vue d'ensemble (KPIs)",
                "/dashboard/admin/users — Gestion des utilisateurs (créer, archiver, réinitialiser MDP)",
                "/dashboard/admin/dossiers — Tous les dossiers médicaux",
                "/dashboard/admin/schedule — Tous les rendez-vous",
                "/dashboard/admin/consults — Toutes les consultations",
                "/dashboard/admin/audit — Journal d'audit",
                "/dashboard/admin/settings — Paramètres système"
        ));
        m.put("coordinatrice", List.of(
                "/dashboard/coordinatrice — Vue d'ensemble",
                "/dashboard/coordinatrice/employees — Gestion des employés",
                "/dashboard/coordinatrice/schedule — Planning des rendez-vous",
                "/dashboard/coordinatrice/reminders — Rappels à envoyer"
        ));
        m.put("medecin", List.of(
                "/dashboard/doctor — Tableau de bord",
                "/dashboard/doctor/patients — Mes patients (employés assignés)",
                "/dashboard/doctor/dossiers — Dossiers médicaux",
                "/dashboard/doctor/consults — Mes consultations",
                "/dashboard/doctor/vaccines — Calendrier vaccinal",
                "/dashboard/doctor/drugs — Bibliothèque médicamenteuse"
        ));
        return m;
    }

    private Map<String, Object> adminFeatures() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("scope", "Lecture/écriture sur toute la plateforme. Ne peut PAS prescrire ni rédiger des SOAP.");
        m.put("user_management", "Créer un utilisateur via Utilisateurs → bouton 'Inviter utilisateur'. Champs : prénom, nom, email, rôle, mot de passe initial.");
        m.put("audit", "Toute action sensible (création, modification, suppression, consultation de dossier) est tracée dans Audit avec horodatage, utilisateur, IP et détails.");
        m.put("settings", "Configuration globale : politique de session, 2FA obligatoire, rétention des audits.");
        m.put("reports", "Bouton 'Exporter rapport' depuis la vue d'ensemble.");
        return m;
    }

    private Map<String, Object> coordFeatures() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("scope", "Coordination administrative pour son médecin assigné (assigned_medecin_id). Ne peut PAS prescrire ni rédiger des SOAP.");
        m.put("employees", "Créer / archiver des dossiers employés. Renseigner antécédents, poste de travail. L'employé est automatiquement assigné au médecin de la coordinatrice.");
        m.put("appointments", "Planifier, modifier ou annuler des rendez-vous. Par défaut, le rendez-vous est affecté au médecin de la coordinatrice.");
        m.put("reminders", "Programmer des rappels (visite annuelle, vaccin, suivi). Marquer comme envoyés ou déclencher l'envoi par email.");
        m.put("workflow", "Flux typique : créer dossier employé → planifier visite → envoyer rappel → médecin réalise consultation.");
        return m;
    }

    private Map<String, Object> doctorFeatures() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("scope", "Actes cliniques sur les patients qui lui sont assignés (medecin_id).");
        m.put("consultations", "Créer une consultation, signer le rapport, joindre des documents.");
        m.put("prescriptions", "Génération d'ordonnance via MedAssist (outil generate_prescription) — vérification d'interactions automatique.");
        m.put("soap", "Rédaction de notes SOAP via MedAssist (generate_soap_note).");
        m.put("vaccinations", "Suivi du calendrier vaccinal de chaque employé.");
        m.put("library", "Recherche de médicaments par nom, DCI, indication ou pathologie.");
        return m;
    }

    private Map<String, Object> employeeModule() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("entity", "EmployeeEntity (= patient dans le contexte clinique)");
        m.put("identifier", "Numéro de dossier (dossier_number) unique, ou ID interne numérique.");
        m.put("fields", List.of(
                "Identité : prénom, nom, date/lieu de naissance, situation familiale, nb enfants",
                "Coordonnées : adresse, code postal, téléphone, email",
                "Travail : poste de travail, département, date d'embauche, matricule caisse",
                "Médical : antécédents (chirurgicaux, médicaux, gynécologiques, héréditaires)",
                "Affectation : medecin_id (médecin référent)"
        ));
        m.put("how_to_create", "COORDINATRICE ou ADMIN → menu Employés → bouton 'Nouveau'. MEDECIN n'a pas le droit de créer.");
        m.put("how_to_archive", "Bouton 'Archiver' sur la fiche. L'employé n'apparaît plus dans les listes actives mais reste consultable.");
        return m;
    }

    private Map<String, Object> consultationsModule() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("entity", "ConsultationEntity");
        m.put("types", List.of(
                "VISITE_GENERALE — visite médicale standard",
                "ORDONNANCE — ordonnance (générée par MedAssist ou manuellement)",
                "SOAP — note SOAP structurée",
                "URGENCE — consultation d'urgence"
        ));
        m.put("encryption", "Le champ 'details' est chiffré au repos.");
        m.put("how_to_create", "MEDECIN → menu Consultations → 'Nouvelle consultation'. Ou via MedAssist : 'Génère une ordonnance pour le patient X'.");
        return m;
    }

    private Map<String, Object> appointmentsModule() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("entity", "AppointmentEntity");
        m.put("statuts", List.of("PLANIFIE", "CONFIRME", "EN_COURS", "TERMINE", "ANNULE", "ABSENT"));
        m.put("types_visite", List.of("Embauche", "Périodique", "Reprise", "Spontanée", "Urgence"));
        m.put("how_to_schedule", "COORDINATRICE → menu Rendez-vous → 'Planifier visite'. Choisir employé, médecin, date/heure, type.");
        return m;
    }

    private Map<String, Object> remindersModule() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("entity", "ReminderEntity");
        m.put("usage", "Suivi proactif : visite annuelle, vaccin de rappel, contrôle après traitement.");
        m.put("delivery", "Email automatique à l'employé si sendEmailNow=true ou à la date d'échéance.");
        m.put("how_to_create", "COORDINATRICE → menu Rappels → 'Nouveau rappel'.");
        return m;
    }

    private Map<String, Object> auditModule() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("scope", "ADMIN uniquement");
        m.put("entity", "AuditLogEntity");
        m.put("captured", List.of("user_id", "action", "entity_type", "entity_id", "details", "ip_address", "user_role", "timestamp"));
        m.put("retention", "Configurable via Paramètres.");
        return m;
    }

    private Map<String, Object> drugLibraryModule() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("entity", "DrugEntity");
        m.put("contents", "Catalogue de médicaments (DCI, posologies, indications, pathologies couvertes, image).");
        m.put("ai_search", "MedAssist utilise l'outil search_medical_library pour interroger ce catalogue.");
        return m;
    }

    private Map<String, Object> authModule() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("flow", "Email + mot de passe. Optionnel : code 2FA à 6 chiffres envoyé par email.");
        m.put("session", "JWT (accessToken + refreshToken). Stocké côté client dans localStorage (clé 'medzoon.auth.token').");
        m.put("password_reset", "Demandé via le formulaire 'Mot de passe oublié'.");
        m.put("demo_accounts", List.of(
                "admin@medzoon.health",
                "coord@medzoon.health",
                "doctor@medzoon.health"
        ));
        return m;
    }

    private Map<String, Object> vaccinationsModule() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("scope", "Suivi du calendrier vaccinal des employés.");
        m.put("how", "MEDECIN → menu Vaccinations → fiche employé → ajouter dose, date, lot, prochaine échéance.");
        m.put("reminders", "Génère automatiquement un rappel si la date de rappel approche.");
        return m;
    }

    private Map<String, Object> settingsModule() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("scope", "ADMIN uniquement");
        m.put("sections", List.of("2FA obligatoire", "Politique de mot de passe", "Rétention audit", "Branding"));
        return m;
    }

    private Map<String, Object> rolesAndPermissions() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ADMIN", List.of(
                "Gestion utilisateurs (CRUD)",
                "Lecture audit complet",
                "Lecture toutes données médicales",
                "INTERDIT : prescription, note SOAP"
        ));
        m.put("COORDINATRICE", List.of(
                "Gestion administrative pour son médecin assigné",
                "CRUD employés (assignés par défaut à son médecin)",
                "CRUD rendez-vous (assignés par défaut à son médecin)",
                "CRUD rappels",
                "Lecture consultations (liées à son médecin)",
                "INTERDIT : prescription, note SOAP, audit"
        ));
        m.put("MEDECIN", List.of(
                "Lecture/écriture sur SES patients (medecin_id)",
                "Création consultations, ordonnances, notes SOAP",
                "Vaccinations",
                "INTERDIT : gestion utilisateurs, audit, paramètres"
        ));
        return m;
    }
}
