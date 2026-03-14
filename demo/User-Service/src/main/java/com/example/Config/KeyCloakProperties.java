package com.example.Config;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Builder
@Data
public class KeyCloakProperties {
    @Value("${app.config.keycloak.server-url}")
    private String serverUrl;

    @Value("${app.config.keycloak.realm}")
    private String realm;

    @Value("${app.config.keycloak.client-id}")
    private String client_id;

    @Value("${app.config.keycloak.client-secret}")
    private String client_secret;

    private static Keycloak keycloakInstance = null;

    public Keycloak getKeycloakInstance() {
        if (keycloakInstance == null) {
            keycloakInstance = KeycloakBuilder.builder()
                    .serverUrl(serverUrl)
                    .realm(realm)
                    .clientId(client_id)
                    .clientSecret(client_secret)
                    .grantType("client_credentials")
                    .build();
        }

        return keycloakInstance;
    }

    public String getRealm() {
        return realm;
    }
}
