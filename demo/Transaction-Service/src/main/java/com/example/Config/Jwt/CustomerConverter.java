package com.example.Config.Jwt;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;

public class CustomerConverter implements Converter<Jwt, AbstractAuthenticationToken>{
    private final JwtGrantedAuthoritiesConverter authoritiesConverter;

    public CustomerConverter() {
        authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("role");
        authoritiesConverter.setAuthorityPrefix("ROLE_");
    }

    public CustomerConverter(JwtGrantedAuthoritiesConverter authoritiesConverter) {
        this.authoritiesConverter = authoritiesConverter;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = authoritiesConverter.convert(jwt);

        String email = jwt.getSubject();
        String sessionId = jwt.getClaim("sessionId");


        return new CustomAuthentication(jwt, authorities, email);
    }
}
