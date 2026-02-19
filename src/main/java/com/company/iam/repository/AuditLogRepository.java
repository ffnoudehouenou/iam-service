// repository/AuditLogRepository.java
package com.company.iam.repository;

import com.company.iam.model.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUsernameOrderByTimestampDesc(String username);

    List<AuditLog> findByActionAndTimestampBetween(
        String action, 
        LocalDateTime start, 
        LocalDateTime end
    );

    @Query("SELECT a FROM AuditLog a WHERE a.result = 'FAILURE' AND a.action = 'LOGIN' " +
           "AND a.username = :username AND a.timestamp > :since")
    List<AuditLog> findFailedLoginAttempts(String username, LocalDateTime since);

    long countByUsernameAndActionAndResultAndTimestampAfter(
        String username, String action, String result, LocalDateTime after
    );
}