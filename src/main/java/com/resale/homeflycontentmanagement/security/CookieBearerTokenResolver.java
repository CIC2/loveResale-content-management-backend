package com.resale.homeflycontentmanagement.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;

import java.util.Arrays;
import java.util.List;

public class CookieBearerTokenResolver implements BearerTokenResolver {

    private final List<String> cookieNames;

    public CookieBearerTokenResolver(String... cookieNames) {
        this.cookieNames = Arrays.asList(cookieNames);
    }

    @Override
    public String resolve(HttpServletRequest request) {
        // 1. Authorization header first
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        // 2. Then check cookies
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (cookieNames.contains(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}


