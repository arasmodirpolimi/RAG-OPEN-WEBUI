package com.ragplatform.api.web.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * /v1/**  : "Authorization: Bearer <openai key>" (what Open WebUI sends for an OpenAI connection)
 * /api/** : "X-API-Key: <admin key>" (document management)
 * Other paths (actuator health) are open on the internal network.
 */
public final class ApiKeyFilter extends OncePerRequestFilter {

    private final byte[] openAiKey;
    private final byte[] adminKey;

    public ApiKeyFilter(String openAiKey, String adminKey) {
        if (openAiKey == null || openAiKey.isBlank() || adminKey == null || adminKey.isBlank()) {
            throw new IllegalArgumentException("rag.security api keys must be configured");
        }
        this.openAiKey = openAiKey.getBytes(StandardCharsets.UTF_8);
        this.adminKey = adminKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean allowed = true;
        if (path.startsWith("/v1/")) {
            String auth = request.getHeader("Authorization");
            allowed = auth != null && auth.startsWith("Bearer ") && matches(auth.substring(7), openAiKey);
        } else if (path.startsWith("/api/")) {
            allowed = matches(request.getHeader("X-API-Key"), adminKey);
        }
        if (!allowed) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":{\"message\":\"Invalid API key\",\"type\":\"authentication_error\"}}");
            return;
        }
        chain.doFilter(request, response);
    }

    private static boolean matches(String provided, byte[] expected) {
        return provided != null && MessageDigest.isEqual(provided.getBytes(StandardCharsets.UTF_8), expected);
    }
}
