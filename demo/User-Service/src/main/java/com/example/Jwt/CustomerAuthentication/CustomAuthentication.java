package com.example.Jwt.CustomerAuthentication;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;

public class CustomAuthentication extends JwtAuthenticationToken {
    private final String email;
    private final String sessionId;
    public CustomAuthentication(Jwt jwt,
                                Collection<? extends GrantedAuthority> authorities,
                                String email, String sessionId) {
        super(jwt, authorities, email);
        this.email = email;
        this.sessionId = sessionId;
    }

    public String getEmail() {
        return this.email;
    }

    public String getSessionId() {
        return this.sessionId;
    }


}
