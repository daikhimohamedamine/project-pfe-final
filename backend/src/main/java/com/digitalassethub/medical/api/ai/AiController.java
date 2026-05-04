package com.digitalassethub.medical.api.ai;

import com.digitalassethub.medical.api.security.SecurityUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI Assistant")
@RestController
@RequestMapping("/api/v1/ai")
public class AiController {
    private final AiRecommendationService recommendationService;
    private final AiChatService chatService;

    public AiController(AiRecommendationService recommendationService, AiChatService chatService) {
        this.recommendationService = recommendationService;
        this.chatService = chatService;
    }

    @PostMapping("/recommend")
    @PreAuthorize("hasRole('MEDECIN')")
    public Object recommend(@Valid @RequestBody AiRecommendRequest request) {
        return recommendationService.recommend(request);
    }

    @PostMapping("/chat")
    @PreAuthorize("hasAnyRole('COORDINATRICE','MEDECIN','ADMIN')")
    public Object chat(@AuthenticationPrincipal Object principal, @RequestBody ChatRequest request) {
        if (!(principal instanceof SecurityUser securityUser)) {
            java.util.Map<String, Object> err = new java.util.HashMap<>();
            err.put("response", "Erreur de session : utilisateur non reconnu. Veuillez vous reconnecter.");
            err.put("error", true);
            return err;
        }
        return chatService.chat(securityUser.user(), request);
    }
}
