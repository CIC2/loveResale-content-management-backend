package com.resale.resalecontentmanagement.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resale.resalecontentmanagement.utils.ReturnObject;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {

        response.setContentType("application/json");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ReturnObject<Void> errorResponse = new ReturnObject<>();
        errorResponse.setStatus(false);
        errorResponse.setData(null);

        // Check if the error is due to expired token
        String errorMessage = authException.getMessage();
        if (errorMessage != null && errorMessage.toLowerCase().contains("expired")) {
            errorResponse.setMessage("Session expired. Please login again.");
        } else if (errorMessage != null && errorMessage.toLowerCase().contains("invalid")) {
            errorResponse.setMessage("Invalid token. Please login again.");
        } else {
            errorResponse.setMessage("Unauthorized access. Please login again.");
        }

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}

