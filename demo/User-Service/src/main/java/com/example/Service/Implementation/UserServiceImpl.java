package com.example.Service.Implementation;

import com.example.Exception.EmptyFields;
import com.example.Exception.ResourceConflictException;
import com.example.Exception.ResourceNotFoundException;
import com.example.External.AccountService;
import com.example.Jwt.CustomerAuthentication.CustomAuthentication;
import com.example.Jwt.JwtProvider;
import com.example.Jwt.UserDetail.UserPrinciple;
import com.example.Model.Dto.Internal.*;
import com.example.Model.Dto.Response.CreateResponse;
import com.example.Model.Dto.Response.JwtResponse;
import com.example.Model.Dto.Response.Response;
import com.example.Model.Entity.User;
import com.example.Model.Entity.UserProfile;
import com.example.Model.Entity.VerificationToken;
import com.example.Model.External.Account;
import com.example.Model.Mapper.UserMapper;
import com.example.Repository.UserRepository;
import com.example.Repository.VerificationTokenRepository;
import com.example.Service.KeyCloakService;
import com.example.Service.UserService;
import com.example.Utils.FieldChecked;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.example.Constant.AppConstant.NUMBER_OF_PAGE;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final VerificationTokenRepository verificationTokenRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private UserMapper userMapper = new UserMapper();

    @Value("${spring.application.success}")
    private String responseCodeSuccess;

    @Value("${spring.application.not_found}")
    private String responseCodeNotFound;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String getMyEmail() {
        Authentication customAuthentication = (CustomAuthentication)SecurityContextHolder.getContext().getAuthentication();
        if (customAuthentication == null) return null;
        Object principal = customAuthentication.getPrincipal();

        if (principal instanceof CustomAuthentication customAuth) {
            return customAuth.getEmail();
        } else if (principal instanceof UserPrinciple userPrinciple) {
            return userPrinciple.email();
        }
        return null;
    }

    @Override
    public CreateResponse createUser(CreateUser userDto) {
       boolean checkEmail = userRepository.existsByEmail(userDto.getEmail());

       if (checkEmail) {
           throw new ResourceConflictException("Email used on the servers");
       }

       UserProfile userProfile = UserProfile.builder()
               .firstName(userDto.getFirstName())
               .lastName(userDto.getLastName())
               .build();

       User user = User.builder()
               .email(userDto.getEmail())
               .contactNo(userDto.getContactNumber())
               .password(passwordEncoder.encode(userDto.getPassword()))
               .status(Status.PENDING)
               .roles(Collections.singleton("USER"))
               .identificationNumber(userDto.getIdentificationNumber())
               .verifyEmail(false)
               .enable(false)
               .userProfile(userProfile)
               .build();

       userRepository.save(user);

       return CreateResponse.builder()
               .responseMessage("Created successfully. You must verify email")
               .responseCode(responseCodeSuccess)
               .email(userDto.getEmail())
               .build();
    }

    @Override
    public Response sendCode(String email)  {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Email not use with user on the servers"));

        if (user.getStatus() == Status.APPROVED) {
            throw new ResourceConflictException("Your account verified yet.");
        }

        generateAndSendVerification(user);

        return Response.builder()
                .responseMessage("Verification Token sended your email. Please check your email.")
                .responseCode(responseCodeSuccess)
                .build();
    }

    private void generateAndSendVerification(User user) {
        verificationTokenRepository.deleteByUser(user);

        String token = createToken();
        VerificationToken verificationToken = VerificationToken
                .builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(15))
                .build();
        verificationTokenRepository.save(verificationToken);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafkaTemplate.send("registration-topic", user.getEmail(), token);
            }
        });
    }

    private String createToken() {
        String token;
        do {
            token = String.valueOf(new SecureRandom().nextInt(900000) + 100000);
        } while (verificationTokenRepository.existsByToken(token));
        return token;
    }

    @Override
    public Response verifyToken(String token) {
        VerificationToken verification = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found on the servers"));

        if (verification.getExpiryDate().isBefore((LocalDateTime.now()))) {
            throw new ResourceConflictException("Token is expired");
        }

        User user = verification.getUser();

        Response response = this.updateUserStatus(user.getUserId(), UpdateStatus.builder()
                        .status(Status.APPROVED)
                .build());

        verificationTokenRepository.deleteByUser(user);

        response.setResponseMessage("Verify Account successfully. Please login again with your email and password");
        return response;

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
            user.setVerifyEmail(true);
            user.setEnable(true);
        }

        user.setStatus(userUpdate.getStatus());

        userRepository.save(user);


        return Response.builder()
                .responseMessage("User updated successfully")
                .responseCode(responseCodeSuccess)
                .build();
    }

    public Response addAdminRole(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.getRoles().add("ADMIN");

        return Response.builder()
                .responseMessage("Add role to user successfully")
                .responseCode(responseCodeSuccess)
                .build();
    }

    @Override
    public Response updateUserProfile(Long id, UpdateUserProfile userUpdate) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found on the server"));

        user.setContactNo(userUpdate.getContactNo());
        BeanUtils.copyProperties(userUpdate, user.getUserProfile());
        userRepository.save(user);

        return Response.builder()
                .responseCode(responseCodeSuccess)
                .responseMessage("User update successfully")
                .build();
    }

    @Override
    public UserDto getMyInfo() {
        String email = getMyEmail();
        return userRepository.findByEmail(email)
                .map(user -> userMapper.convertToDto(user))
                .orElseThrow(() -> new ResourceNotFoundException("User not found on the server"));
    }

    @Override
    public UserDto changeContactNumber( String contactNumber) {
        String email = getMyEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Email not found on the servers"));

        user.setContactNo(contactNumber);

        userRepository.save(user);

        return userMapper.convertToDto(user);
    }

    @Override
    public UserDto changeUserProfile( UpdateUserProfile profile) {
        String email = getMyEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Email not found on the servers"));

        UserProfile userProfile = user.getUserProfile();

        BeanUtils.copyProperties(profile, userProfile);
        userRepository.save(user);

        return userMapper.convertToDto(user);
    }

    @Override
    public UserDto readUserById(Long userId) {
        return userRepository.findById(userId)
                .map(user -> userMapper.convertToDto(user))
                .orElseThrow(() -> new ResourceNotFoundException("User not found on the server"));
    }

//    @Override
//    public UserDto readUserByAccountId(String accountId) {
//        ResponseEntity<Account> response = accountService.readByAccountNumber(accountId);
//
//        if (Objects.isNull(response.getBody())) {
//            throw new ResourceNotFoundException("Account not found on the server");
//
//        }
//        Long userId = response.getBody().getUserId();
//
//        return userRepository.findById(userId)
//                .map(user -> userMapper.convertToDto(user))
//                .orElseThrow(() -> new ResourceNotFoundException("User not found on the server"));
//    }

    @Override
    public JwtResponse login(UserLogin login) {
        User user = userRepository.findByEmail(login.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Email not found on the servers"));

        if (passwordEncoder.matches(login.getPassword(), user.getPassword())) {
            throw new ResourceConflictException("Password not match");
        }

        String accessToken = jwtProvider.generateAccessToken(login.getEmail(), user.getRoles());
        String refreshToken = jwtProvider.generateRefreshToken(login.getEmail());

        return JwtResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .email(login.getEmail())
                .build();
    }

    @Override
    public List<UserDto> readAllUsers(int page) {
        Pageable pageable = PageRequest.of(
                page,
                NUMBER_OF_PAGE,
                Sort.by("email").ascending()
        );

        Page<User> userPage = userRepository.findAll(pageable);

        return userPage.getContent()
                .stream()
                .map(userMapper::convertToDto)
                .toList();
    }


}
