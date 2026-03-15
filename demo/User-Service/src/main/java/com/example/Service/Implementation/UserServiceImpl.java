package com.example.Service.Implementation;

import com.example.Exception.EmptyFields;
import com.example.Exception.ResourceConfigException;
import com.example.Exception.ResourceNotFoundException;
import com.example.External.AccountService;
import com.example.Model.Dto.Internal.*;
import com.example.Model.Dto.Response.Response;
import com.example.Model.Entity.User;
import com.example.Model.Entity.UserProfile;
import com.example.Model.External.Account;
import com.example.Model.Mapper.UserMapper;
import com.example.Repository.UserRepository;
import com.example.Service.KeyCloakService;
import com.example.Service.UserService;
import com.example.Utils.FieldChecked;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final AccountService accountService;
    private final KeyCloakService keyCloakService;

    private UserMapper userMapper = new UserMapper();

    @Value("${spring.application.success}")
    private String responseCodeSuccess;

    @Value("${spring.application.not_found}")
    private String responseCodeNotFound;


    @Override
    public Response createUser(CreateUser userDto) {
        List<UserRepresentation> userRepresentations = keyCloakService.readUserByEmail(userDto.getEmail());

        if (userRepresentations.size() > 0) {
            log.error("This email is already registered a user");
            throw new ResourceConfigException("This email is already registered as a user");
        }
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(userDto.getEmail());
        userRepresentation.setFirstName(userDto.getFirstName());
        userRepresentation.setLastName(userDto.getLastName());
        userRepresentation.setEmailVerified(false);
        userRepresentation.setEnabled(false);
        userRepresentation.setEmail(userDto.getEmail());

        CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setValue(userDto.getPassword());
        credentialRepresentation.setTemporary(false);
        userRepresentation.setCredentials(Collections.singletonList(credentialRepresentation));

        Integer userCreationResponse = keyCloakService.createUser(userRepresentation);

        if (userRepresentations.equals(201)) {
            List<UserRepresentation> representations = keyCloakService.readUserByEmail(userDto.getEmail());


            UserProfile userProfile = UserProfile.builder()
                    .firstName(userDto.getFirstName())
                    .lastName(userDto.getLastName())
                    .build();

            User user = User.builder()
                    .email(userDto.getEmail())
                    .contactNo(userDto.getContactNumber())
                    .status(Status.PENDING)
                    .userProfile(userProfile)
                    .build();

            userRepository.save(user);

            return Response.builder()
                    .responseCode(responseCodeSuccess)
                    .responseMessage("User created successfully")
                    .build();
        }
        throw new RuntimeException("User with identification number not found");
    }

    @Override
    public List<UserDto> readAllUsers() {
        List<User> users = userRepository.findAll();

        Map<String, UserRepresentation> userRepresentationMap =
                keyCloakService.readUsers(
                                users.stream()
                                        .map(user -> user.getAuthId())
                                        .collect(Collectors.toList())
                        )
                        .stream()
                        .collect(Collectors.toMap(UserRepresentation::getId, Function.identity()));


        return users.stream()
                .map(user -> {
                    UserDto userDto = userMapper.convertToDto(user);
                    UserRepresentation userRepresentation = userRepresentationMap.get(user.getAuthId());
                    userDto.setUserId(user.getUserId());
                    userDto.setEmail(userRepresentation.getEmail());
                    userDto.setIdentificationNumber(user.getIdentificationNumber());
                    return userDto;
                }).collect(Collectors.toList());
    }

    @Override
    public UserDto readUser(String authId) {
        User user  = userRepository.findUserByAuthId(authId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found in server"));
        UserRepresentation userRepresentation = keyCloakService.readUser(authId);
        UserDto userDto = userMapper.convertToDto(user);
        userDto.setEmail(userRepresentation.getEmail());
        return userDto;
    }

    @Override
    public Response updateUserStatus(Long id, UpdateStatus userUpdate) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found on the server"));

        if (FieldChecked.hasEmptyFields(user)) {
            log.error("User not updated completely");
            throw new EmptyFields(responseCodeNotFound, "please fill up fields to update the user");
        }

        if (userUpdate.getStatus().equals(Status.APPROVED)) {
            UserRepresentation userRepresentation = keyCloakService.readUser(user.getAuthId());

            userRepresentation.setEnabled(true);
            userRepresentation.setEmailVerified(true);
            keyCloakService.updateUser(userRepresentation);
        }

        user.setStatus(userUpdate.getStatus());

        userRepository.save(user);


        return Response.builder()
                .responseMessage("User updated successfully")
                .responseCode(responseCodeSuccess)
                .build();
    }

    public void addAdminRole(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        keyCloakService.addRealmRoleToUser(user.getAuthId(), "ADMIN");
    }

    @Override
    public Response updateUser(Long id, UpdateUserProfile userUpdate) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found on the server"));

        user.setContactNo(userUpdate.getContactNo());
        BeanUtils.copyProperties(userUpdate, user.getUserProfile());
        userRepository.save(user);

        return Response.builder()
                .responseCode(responseCodeSuccess)
                .responseMessage("user update successfully")
                .build();
    }

    @Override
    public UserDto readUserById(Long userId) {
        return userRepository.findById(userId)
                .map(user -> userMapper.convertToDto(user))
                .orElseThrow(() -> new ResourceNotFoundException("User not found on the server"));
    }

    @Override
    public UserDto readUserByAccountId(String accountId) {
        ResponseEntity<Account> response = accountService.readByAccountNumber(accountId);

        if (Objects.isNull(response.getBody())) {
            throw new ResourceNotFoundException("Account not found on the server");

        }
        Long userId = response.getBody().getUserId();

        return userRepository.findById(userId)
                .map(user -> userMapper.convertToDto(user))
                .orElseThrow(() -> new ResourceNotFoundException("User not found on the server"));
    }
}
