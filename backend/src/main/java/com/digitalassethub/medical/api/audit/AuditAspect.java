package com.digitalassethub.medical.api.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.digitalassethub.medical.api.security.SecurityUser;
import com.digitalassethub.medical.api.user.UserEntity;
import com.digitalassethub.medical.api.user.Role;

@Aspect
@Component
public class AuditAspect {
    private final AuditLogRepository repository;
    private final HttpServletRequest request;

    public AuditAspect(AuditLogRepository repository, HttpServletRequest request) {
        this.repository = repository;
        this.request = request;
    }

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void log(JoinPoint joinPoint, Auditable auditable, Object result) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            AuditLogEntity log = new AuditLogEntity();
            log.setAction(auditable.action());
            log.setEntityType(auditable.entityType());
            log.setDetails(joinPoint.getSignature().toShortString());
            log.setIpAddress(request != null ? request.getRemoteAddr() : "unknown");
            
            if (auth != null && auth.getPrincipal() instanceof SecurityUser securityUser) {
                UserEntity user = securityUser.user();
                log.setUserId(user.getId());
                log.setUserRole(user.getRole().name());
                
                // Définir le contexte du médecin
                if (user.getRole() == Role.MEDECIN) {
                    log.setMedecinContextId(user.getId());
                } else if (user.getRole() == Role.COORDINATRICE) {
                    log.setMedecinContextId(user.getAssignedMedecinId());
                }
            }
            
            // Tentative d'extraire l'ID de l'entité
            try {
                if (result != null) {
                    java.lang.reflect.Method getIdMethod = result.getClass().getMethod("getId");
                    Object id = getIdMethod.invoke(result);
                    if (id instanceof Long) {
                        log.setEntityId((Long) id);
                    }
                } else if (joinPoint.getArgs().length > 0 && joinPoint.getArgs()[0] instanceof Long) {
                    log.setEntityId((Long) joinPoint.getArgs()[0]);
                }
            } catch (Exception e) {
                // Pas d'ID disponible
            }

            repository.save(log);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
