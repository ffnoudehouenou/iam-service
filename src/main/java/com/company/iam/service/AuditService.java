// service/AuditService.java
package com.company.iam.service;

import com.company.iam.model.entity.AuditLog;
import com.company.iam.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * ACCOUNTING - Enregistrer une action (async pour ne pas bloquer)
     */
    @Async
    public void logAction(String action, String username, 
                          String ipAddress, String result, String details) {
        try {
            AuditLog auditLog = AuditLog.builder()
                .action(action)
                .username(username)
                .ipAddress(ipAddress)
                .result(result)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log saved: action={}, user={}, result={}", 
                action, username, result);
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }
    }

    /**
     * Récupérer les logs d'un utilisateur
     */
    public List<AuditLog> getUserAuditLogs(String username) {
        return auditLogRepository.findByUsernameOrderByTimestampDesc(username);
    }

    /**
     * Détecter les tentatives de connexion échouées
     */
    public boolean isAccountLocked(String username) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(15);
        long failedAttempts = auditLogRepository.countByUsernameAndActionAndResultAndTimestampAfter(
            username, "LOGIN", "FAILURE", since
        );
        return failedAttempts >= 5; // Blocage après 5 tentatives en 15 minutes
    }

    /**
     * Récupérer les logs par période
     */
    public List<AuditLog> getAuditLogsByPeriod(String action, 
                                                LocalDateTime start, 
                                                LocalDateTime end) {
        return auditLogRepository.findByActionAndTimestampBetween(action, start, end);
    }
}