package com.example.Config;

import lombok.RequiredArgsConstructor;
import org.keycloak.admin.client.resource.RealmResource;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

@Component
public class KeyCloakManager {
    private final KeyCloakProperties keyCloakProperties;

    public KeyCloakManager(KeyCloakProperties keyCloakProperties) {
        this.keyCloakProperties = keyCloakProperties;
    }

    public RealmResource getKeyCloakInstanceWithRealm() {
        return keyCloakProperties.getKeycloakInstance().realm(keyCloakProperties.getRealm());
    }
}
