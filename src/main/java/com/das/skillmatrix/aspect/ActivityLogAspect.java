package com.das.skillmatrix.aspect;

import java.lang.reflect.Method;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.das.skillmatrix.annotation.LogActivity;
import com.das.skillmatrix.entity.AuditLog;
import com.das.skillmatrix.entity.User;
import com.das.skillmatrix.repository.AuditLogRepository;
import com.das.skillmatrix.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
public class ActivityLogAspect {

    private static final Logger log = LoggerFactory.getLogger(ActivityLogAspect.class);

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final HttpServletRequest httpServletRequest;

    @AfterReturning(pointcut = "@annotation(logActivity)", returning = "result")
    public void logActivity(JoinPoint joinPoint, LogActivity logActivity, Object result) {
        try {
            AuditLog auditLog = new AuditLog();
            auditLog.setAction(logActivity.action());
            auditLog.setEntityType(logActivity.entityType());

            // 1. Get current user info
            populateUserInfo(auditLog);
            populateUserInfoFromArgs(auditLog, joinPoint);

            // 2. Extract entityId from return value or method parameters
            Long entityId = extractEntityId(result, joinPoint);
            auditLog.setEntityId(entityId);

            // 3. Build metadata JSON from method arguments
            String metadata = buildMetadata(joinPoint, logActivity);
            auditLog.setMetadata(metadata);

            // 4. Get IP address from HTTP request
            auditLog.setIpAddress(getClientIpAddress());

            auditLogRepository.save(auditLog);

            log.debug("Activity logged: action={}, entityType={}, entityId={}, userId={}",
                    logActivity.action(), logActivity.entityType(), entityId, auditLog.getUserId());

        } catch (Exception e) {
            log.error("Failed to log activity: action={}, entityType={}, error={}",
                    logActivity.action(), logActivity.entityType(), e.getMessage(), e);
        }
    }

    // Populate userId and userEmail from SecurityContext.
    // Fallback: extract email from method parameters (e.g. LoginRequest) when not yet authenticated.
    private void populateUserInfo(AuditLog auditLog) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String email = auth.getName();
            auditLog.setUserEmail(email);
            User user = userRepository.findUserByEmail(email);
            if (user != null) {
                auditLog.setUserId(user.getUserId());
            }
        }
    }

    // Fallback: extract email from method parameters when SecurityContext is empty
    private void populateUserInfoFromArgs(AuditLog auditLog, JoinPoint joinPoint) {
        if (auditLog.getUserEmail() != null)
            return; // already populated
        for (Object arg : joinPoint.getArgs()) {
            try {
                java.lang.reflect.Method getEmail = arg.getClass().getMethod("getEmail");
                Object email = getEmail.invoke(arg);
                if (email instanceof String emailStr && !emailStr.isBlank()) {
                    auditLog.setUserEmail(emailStr);
                    User user = userRepository.findUserByEmail(emailStr);
                    if (user != null) {
                        auditLog.setUserId(user.getUserId());
                    }
                    return;
                }
            } catch (Exception ignored) {
            }
        }
    }

    // Extract entityId from the method return value or parameters.
    private Long extractEntityId(Object result, JoinPoint joinPoint) {
        if (result != null) {
            Long id = extractIdFromObject(result);
            if (id != null) {
                return id;
            }
        }

        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof Long longArg) {
                return longArg;
            }
        }

        return null;
    }

    private Long extractIdFromObject(Object obj) {
        if (obj == null)
            return null;

        for (Method method : obj.getClass().getMethods()) {
            String name = method.getName();
            if (name.startsWith("get") && name.endsWith("Id")
                    && method.getParameterCount() == 0
                    && (Long.class.equals(method.getReturnType()) || long.class.equals(method.getReturnType()))) {
                try {
                    Object value = method.invoke(obj);
                    if (value instanceof Long longValue) {
                        return longValue;
                    }
                } catch (Exception ignored) {
                    // Skip this getter
                }
            }
        }
        return null;
    }

    // Build a simple JSON metadata string
    private String buildMetadata(JoinPoint joinPoint, LogActivity logActivity) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (paramNames == null || paramNames.length == 0) {
            return null;
        }

        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (int i = 0; i < paramNames.length; i++) {
            Object arg = args[i];
            if (arg == null)
                continue;

            String value = null;
            if (arg instanceof Long || arg instanceof Integer || arg instanceof Boolean) {
                value = arg.toString();
            } else if (arg instanceof String strArg) {
                value = "\"" + strArg.replace("\"", "\\\"") + "\"";
            } else if (arg instanceof java.util.List<?> list) {
                value = list.toString();
            }

            if (value != null) {
                if (!first)
                    sb.append(", ");
                sb.append("\"").append(paramNames[i]).append("\": ").append(value);
                first = false;
            }
        }
        sb.append("}");

        String result = sb.toString();
        return "{}".equals(result) ? null : result;
    }

    // Get client IP address, handling proxy headers.
    private String getClientIpAddress() {
        try {
            String ip = httpServletRequest.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = httpServletRequest.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = httpServletRequest.getRemoteAddr();
            }
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        } catch (Exception e) {
            return null;
        }
    }
}