package com.digitalassethub.medical.api.security.encryption;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

@Converter
public class AesAttributeConverter implements AttributeConverter<String, String> {
    private static final String DEFAULT_SECRET = "0123456789abcdef0123456789abcdef";

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isBlank()) {
            return attribute;
        }
        try {
            String secret = System.getenv().getOrDefault("APP_ENCRYPTION_KEY", DEFAULT_SECRET);
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(secret.substring(0, 16).getBytes(StandardCharsets.UTF_8), "AES"));
            return Base64.getEncoder().encodeToString(cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return dbData;
        }
        try {
            String secret = System.getenv().getOrDefault("APP_ENCRYPTION_KEY", DEFAULT_SECRET);
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(secret.substring(0, 16).getBytes(StandardCharsets.UTF_8), "AES"));
            return new String(cipher.doFinal(Base64.getDecoder().decode(dbData)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Si le déchiffrement échoue (ex: données non chiffrées en base), on renvoie le texte brut
            System.err.println("Avertissement: Échec du déchiffrement pour une valeur, utilisation du texte brut.");
            return dbData;
        }
    }
}
