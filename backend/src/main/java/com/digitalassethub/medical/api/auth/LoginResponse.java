package com.digitalassethub.medical.api.auth;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String role,
        String nom,
        String prenom,
        boolean requires2fa,
        Long assignedMedecinId,
        String avatarUrl
) {
    public static LoginResponse requiresTwoFactor() {
        return new LoginResponse(null, null, null, null, null, true, null, null);
    }
}
