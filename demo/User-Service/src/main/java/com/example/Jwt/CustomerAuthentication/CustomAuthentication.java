package com.example.Jwt.CustomerAuthentication;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;

public class CustomAuthentication extends JwtAuthenticationToken {
    private final String email;
    public CustomAuthentication(Jwt jwt,
                                Collection<? extends GrantedAuthority> authorities,
                                String email) {
        super(jwt, authorities, email);
        this.email = email;
    }

    public String getEmail() {
        return this.email;
    }


}
