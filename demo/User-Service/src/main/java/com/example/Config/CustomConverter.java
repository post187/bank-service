package com.example.Config;

import com.example.Jwt.CustomerAuthentication.CustomAuthentication;
import com.example.Service.Implementation.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import javax.mail.Session;
import java.lang.annotation.Annotation;
import java.util.Collection;


public class CustomConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final JwtGrantedAuthoritiesConverter authoritiesConverter;

    @Autowired
    private SessionService sessionService;

    public CustomConverter() {
        authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("role");
        authoritiesConverter.setAuthorityPrefix("ROLE_");
    }
    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = authoritiesConverter.convert(jwt);

        String email = jwt.getSubject();
        String sessionId = jwt.getClaim("sessionId");

        if (!sessionService.isSessionValid(sessionId)) {
            throw new RuntimeException("Session expired");
        }

        return new CustomAuthentication(jwt, authorities, email, sessionId);
    }
}
