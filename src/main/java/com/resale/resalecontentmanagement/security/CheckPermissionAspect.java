package com.resale.resalecontentmanagement.security;

import com.resale.resalecontentmanagement.exception.PermissionDeniedException;
import com.resale.resalecontentmanagement.model.Permission;
import com.resale.resalecontentmanagement.model.UserPermission;
import com.resale.resalecontentmanagement.repository.PermissionRepository;
import com.resale.resalecontentmanagement.repository.UserPermissionRepository;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Component
@RequiredArgsConstructor
@ControllerAdvice
public class CheckPermissionAspect {

    private final UserPermissionRepository userPermissionRepository;
    private final PermissionRepository permissionRepository;
    private final JwtTokenUtil jwtTokenUtil;

    @Around("@annotation(checkPermission)")
    public Object verifyPermission(ProceedingJoinPoint joinPoint, CheckPermission checkPermission
    ) throws Throwable {

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AuthenticationCredentialsNotFoundException("Session expired. Please login again.");
        }

        String token = jwt.getTokenValue();

        Integer userId;
        try {
            userId = jwtTokenUtil.extractUserId(token);
        } catch (ExpiredJwtException ex) {
            throw new AuthenticationCredentialsNotFoundException("Session expired. Please login again.");
        }

        if (userId == null) {
            return joinPoint.proceed();
        }

        List<UserPermission> userPerms =
                userPermissionRepository.findByUserId(userId);

        if (userPerms.isEmpty()) {
            throw new PermissionDeniedException("User has no permissions assigned");
        }

        Set<Integer> permIds = userPerms.stream()
                .map(UserPermission::getPermissionId)
                .collect(Collectors.toSet());

        List<Permission> permissions =
                permissionRepository.findAllById(permIds);

        Set<String> userActions = permissions.stream().map(p -> p.getResource() + ":" + p.getAction()).collect(Collectors.toSet());

        boolean allowed = switch (checkPermission.match()) {
            case ANY -> Arrays.stream(checkPermission.value()).anyMatch(userActions::contains);
            case ALL -> userActions.containsAll(Arrays.asList(checkPermission.value()));
        };

        if (!allowed) {
            throw new PermissionDeniedException("You do not have the required permission(s) to access this resource.");
        }

        return joinPoint.proceed();
    }
}

