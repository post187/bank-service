package com.example.Service.Implementation;

import com.example.Config.KeyCloakManager;
import com.example.Service.KeyCloakService;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.Realm;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KeyCloakServiceImpl implements KeyCloakService {

    private final KeyCloakManager keyCloakManager;

    @Override
    public Integer createUser(UserRepresentation userRepresentation) {
        return keyCloakManager.getKeyCloakInstanceWithRealm().users().create(userRepresentation).getStatus();
    }

    @Override
    public List<UserRepresentation> readUserByEmail(String email) {
        return keyCloakManager.getKeyCloakInstanceWithRealm().users().search(email);
    }

    @Override
    public List<UserRepresentation> readUsers(List<String> authIds) {
        return authIds.stream()
                .map(authId -> {
                    UserResource userResource = keyCloakManager.getKeyCloakInstanceWithRealm()
                            .users().get(authId);

                    return userResource.toRepresentation();
                }).collect(Collectors.toList());
    }

    @Override
    public UserRepresentation readUser(String authId) {
        return keyCloakManager.getKeyCloakInstanceWithRealm()
                .users().get(authId).toRepresentation();
    }

    @Override
    public void updateUser(UserRepresentation userRepresentation) {
        keyCloakManager.getKeyCloakInstanceWithRealm().users()
                .get(userRepresentation.getId()).update(userRepresentation);
    }

    @Override
    public void assignRealmRole(String id, String roleName) {
        RealmResource realmResource = keyCloakManager.getKeyCloakInstanceWithRealm();

        RoleRepresentation role = realmResource
                .roles()
                .get(roleName)
                .toRepresentation();

        realmResource.users()
                .get(id)
                .roles()
                .realmLevel()
                .add(Collections.singletonList(role));
    }

    @Override
    public void addRealmRoleToUser(String id, String roleName) {
        RealmResource realmResource = keyCloakManager.getKeyCloakInstanceWithRealm();

        RoleRepresentation roleRepresentation = realmResource
                .roles()
                .get(roleName)
                .toRepresentation();

        realmResource.users()
                .get(id)
                .roles()
                .realmLevel()
                .add(Collections.singletonList(roleRepresentation));
    }
}
