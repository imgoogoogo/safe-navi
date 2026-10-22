package com.safenavi.backend.auth.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.safenavi.backend.common.exception.ErrorCode;
import com.safenavi.backend.common.response.ApiResponse;

import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/** Writes 401/403 from the security filter chain in the same ApiResponse shape as controllers. */
@Component
public class SecurityErrorResponder {

    private final JsonMapper jsonMapper;

    public SecurityErrorResponder(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public AuthenticationEntryPoint entryPoint() {
        return (request, response, ex) -> {
            Object tokenError = request.getAttribute(JwtAuthenticationFilter.TOKEN_ERROR_ATTRIBUTE);
            write(response, tokenError instanceof ErrorCode code ? code : ErrorCode.UNAUTHORIZED);
        };
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> write(response, ErrorCode.FORBIDDEN);
    }

    private void write(HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(response.getOutputStream(), ApiResponse.fail(code.name(), code.getDefaultMessage()));
    }
}
