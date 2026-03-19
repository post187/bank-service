package com.example.Config;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.OAuth2Constants;
import org.keycloak.*;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
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

            String safeServerUrl = serverUrl != null ? serverUrl.trim() : "";
            String safeRealm = realm != null ? realm.trim() : "";
            String safeClientId = client_id != null ? client_id.trim() : "";
            String safeClientSecret = client_secret != null ? client_secret.trim() : "";

            // fix localhost -> tránh lỗi Docker / network
            if (safeServerUrl.contains("localhost")) {
                safeServerUrl = safeServerUrl.replace("localhost", "127.0.0.1");
            }

            System.out.println(">>> serverUrl = [" + safeServerUrl + "]");
            System.out.println(">>> realm = [" + safeRealm + "]");
            System.out.println(">>> clientId = [" + safeClientId + "]");

            keycloakInstance = KeycloakBuilder.builder()
                    .serverUrl(safeServerUrl)   // ✅ FIX QUAN TRỌNG
                    .realm(safeRealm)           // ✅ FIX
                    .clientId(safeClientId)     // ✅ FIX
                    .clientSecret(safeClientSecret) // ✅ FIX
                    .grantType(OAuth2Constants.CLIENT_CREDENTIALS) // ✅ chuẩn
                    .build();
        }

        return keycloakInstance;
    }

    public String getRealm() {
        return realm;
    }
}
