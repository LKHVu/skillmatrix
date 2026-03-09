package com.das.skillmatrix.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.das.skillmatrix.entity.BusinessChangeLog;
import com.das.skillmatrix.entity.User;
import com.das.skillmatrix.repository.BusinessChangeLogRepository;
import com.das.skillmatrix.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BusinessChangeLogService {

    private static final Logger log = LoggerFactory.getLogger(BusinessChangeLogService.class);

    private final BusinessChangeLogRepository repository;
    private final UserRepository userRepository;

    public record FieldChange(String field, String oldValue, String newValue) {
    }

    // Log single field change
    public void log(String action, String entityType, Long entityId,
            String field, String oldValue, String newValue) {
        log(action, entityType, entityId, List.of(new FieldChange(field, oldValue, newValue)));
    }

    // Log multiple field changes
    public void log(String action, String entityType, Long entityId,
            List<FieldChange> changes) {
        try {
            BusinessChangeLog changeLog = new BusinessChangeLog();
            changeLog.setAction(action);
            changeLog.setEntityType(entityType);
            changeLog.setEntityId(entityId);
            changeLog.setChanges(toJson(changes));
            populateUserInfo(changeLog);
            repository.save(changeLog);

            log.debug("Business change logged: action={}, entityType={}, entityId={}",
                    action, entityType, entityId);
        } catch (Exception e) {
            log.error("Failed to log business change: action={}, entityType={}, error={}",
                    action, entityType, e.getMessage(), e);
        }
    }

    private void populateUserInfo(BusinessChangeLog changeLog) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String email = auth.getName();
            changeLog.setUserEmail(email);
            User user = userRepository.findUserByEmail(email);
            if (user != null) {
                changeLog.setUserId(user.getUserId());
            }
        }
    }

    private String toJson(List<FieldChange> changes) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < changes.size(); i++) {
            FieldChange fc = changes.get(i);
            if (i > 0)
                sb.append(",");
            sb.append("{")
                    .append("\"field\":\"").append(escape(fc.field())).append("\",")
                    .append("\"oldValue\":\"").append(escape(fc.oldValue())).append("\",")
                    .append("\"newValue\":\"").append(escape(fc.newValue())).append("\"")
                    .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String escape(String value) {
        if (value == null)
            return "null";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
