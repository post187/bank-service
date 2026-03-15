package com.example.Service;

import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;

public interface KeyCloakService {
    Integer createUser(UserRepresentation userRepresentation);

    List<UserRepresentation> readUserByEmail(String email);

    List<UserRepresentation> readUsers(List<String> authIds);

    UserRepresentation readUser(String authId);

    void updateUser(UserRepresentation userRepresentation);

    void assignRealmRole(String id, String roleName);

    void addRealmRoleToUser(String id, String roleName);
}
