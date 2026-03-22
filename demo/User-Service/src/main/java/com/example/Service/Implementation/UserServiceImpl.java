package com.example.Service.Implementation;

import com.example.Exception.EmptyFields;
import com.example.Exception.ResourceConflictException;
import com.example.Exception.ResourceNotFoundException;
import com.example.Jwt.CustomerAuthentication.CustomAuthentication;
import com.example.Jwt.JwtProvider;
import com.example.Jwt.UserDetail.UserPrinciple;
import com.example.Model.Dto.Internal.*;
import com.example.Model.Dto.Internal.Status.Status;
import com.example.Model.Dto.Request.ChangePasswordRequest;
import com.example.Model.Dto.Request.LoginRequest;
import com.example.Model.Dto.Request.ResetPasswordRequest;
import com.example.Model.Dto.Request.VerifyDeviceRequest;
import com.example.Model.Dto.Response.CreateResponse;
import com.example.Model.Dto.Response.JwtResponse;
import com.example.Model.Dto.Response.Response;
import com.example.Model.Dto.Response.UserDto;
import com.example.Model.Entity.User;
import com.example.Model.Entity.UserProfile;
import com.example.Model.Entity.VerificationToken;
import com.example.Model.Mapper.UserMapper;
import com.example.Repository.UserRepository;
import com.example.Repository.VerificationTokenRepository;
import com.example.Service.DeviceService;
import com.example.Service.Extend.PasswordGenerator;
import com.example.Service.UserService;
import com.example.Utils.FieldChecked;
import jakarta.ws.rs.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static com.example.Constant.AppConstant.MAX_LOGIN_ATTEMPT;
import static com.example.Constant.AppConstant.NUMBER_OF_PAGE;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final StringRedisTemplate redis;
    private final VerificationTokenRepository verificationTokenRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JwtProvider jwtProvider;
    private final DeviceService deviceService;
    private final SessionService sessionService;

    private static final int MAX_IN_REDIS = 10;

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
    public Response createUser(CreateUser userDto) {
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new ResourceConflictException("Email used on the servers");
        }

        String encodedPassword = passwordEncoder.encode(userDto.getPassword());

        User newUser = User.builder()
                .email(userDto.getEmail())
                .password(encodedPassword)
                .loginAttempts(0)
                .lockUntil(null)
                .contactNo(userDto.getContactNumber())
                .roles(Set.of("USER"))
                .creationOn(LocalDate.now())
                .status(Status.PENDING)
                .enable(false)
                .verifyEmail(false)
                .build();

        userRepository.save(newUser);

        return Response.builder()
                .responseCode(responseCodeSuccess)
                .responseMessage("Register successfully. Please check your email to verify email.")
                .build();
    }

    @Override
    public JwtResponse login(LoginRequest request) {
        String email = request.getEmail();

        String attemptKey = "login_attempt:" + email;
        String attemptsStr = redis.opsForValue().get(attemptKey);

        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;

        if (attempts >= MAX_LOGIN_ATTEMPT) {
            throw new RuntimeException("Too many attempts. Try later.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Email not found on the servers"));

        // check lock DB
        if (user.getLockUntil() != null &&
        user.getLockUntil().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Account locked");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            redis.opsForValue().increment(attemptKey);
            redis.expire(attemptKey, Duration.ofMinutes(15));

            user.setLoginAttempts(user.getLoginAttempts() + 1);

            if (user.getLoginAttempts() >= 5) {
                user.setLockUntil(LocalDateTime.now().plusMinutes(15));
                user.setLoginAttempts(0);
            }

            userRepository.save(user);

            throw new RuntimeException("Invalid password");
        }

        redis.delete(attemptKey);
        user.setLoginAttempts(0);
        user.setLockUntil(null);
        userRepository.save(user);

        if (!deviceService.isNewDevice(user.getUserId(), request.getDeviceId())) {
            String sessionId = sessionService.createSession(user, request);

            return JwtResponse.builder()
                    .accessToken(jwtProvider.generateAccessToken(user.getEmail(), user.getRoles()))
                    .refreshToken(jwtProvider.generateRefreshToken(user.getEmail()))
                    .sessionId(sessionId)
                    .build();
        }
        deviceService.handleNewDevice(user.getUserId(), request);

        return null;
    }

    @Override
    public Response logout(String sessionId) {
        User user = userRepository.findByEmail(getMyEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Email not found on the servers"));

        sessionService.deleteSession(user.getUserId(), sessionId);

        return Response.builder()
                .responseCode(responseCodeSuccess)
                .responseMessage("Logout successfully")
                .build();
    }

    @Override
    @Transactional
    public JwtResponse verifyDeviceAndLogin(VerifyDeviceRequest request) {
        deviceService.verifyDeviceOtp(request);

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found on the servers"));

        LoginRequest loginInfo = new LoginRequest();
        loginInfo.setDeviceId(request.getDeviceId());
        loginInfo.setDeviceName(request.getDeviceName());
        loginInfo.setIpAddress(request.getIpAddress());

        String sessionId = sessionService.createSession(user, loginInfo);

        return JwtResponse.builder()
                .accessToken(jwtProvider.generateAccessToken(user.getEmail(), user.getRoles()))
                .refreshToken(jwtProvider.generateRefreshToken(user.getEmail()))
                .sessionId(sessionId)
                .build();
    }

    @Override
    public JwtResponse refreshToken(String sessionId, String refreshToken) {
        if (!sessionService.isSessionValid(sessionId)) {
            throw new ResourceConflictException("Session expired. Please login again");
        }

        if (!jwtProvider.validateToken(refreshToken)) {
            throw new ResourceConflictException("Refresh Token không hợp lệ hoặc đã hết hạn.");
        }

        String email = jwtProvider.getEmailFromJwt(refreshToken);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng."));

        sessionService.extendSession(sessionId);

        String newAccessToken = jwtProvider.generateAccessToken(user.getEmail(), user.getRoles());

        return JwtResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .sessionId(sessionId)
                .build();
    }

    @Override
    public Response forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Email not found on the servers"));

        String resetOtp = String.format("%06d", new Random().nextInt(999999));

        String key = "reset_password_otp" + email;
        redis.opsForValue().set(key, resetOtp, Duration.ofMinutes(MAX_IN_REDIS));

        kafkaTemplate.send("reset-password", email, resetOtp);

        return Response.builder()
                .responseCode(responseCodeSuccess)
                .responseMessage("Your otp reset password sended your email. Please checking email soon.")
                .build();
    }

    @Override
    @Transactional
    public Response resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail();
        String key = "reset_password_otp:" + email;

        String validOtp = redis.opsForValue().get(key);
        if (validOtp == null || !validOtp.equals(request.getOtp())) {
            throw new ResourceConflictException("Your otp not correct or otp expired.");
        }

        // 2. Cập nhật mật khẩu mới
        User user = userRepository.findByEmail(email).get();
        String newPassword = PasswordGenerator.generateRandomPassword(8);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        redis.delete(key);
        sessionService.deleteAllSessions(user.getUserId());

        kafkaTemplate.send("reset-password", email, newPassword);

        return Response.builder()
                .responseMessage("Reset password successfully. Check email to new password and change password now")
                .responseCode(responseCodeSuccess)
                .build();
    }

    @Override
    @Transactional
    public Response changePassword(ChangePasswordRequest request, String sessionId) {
        String currentEmail = getMyEmail();
        User user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Email not found on the server"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadRequestException("Your current password not correct");
        }

        // 3. Kiểm tra mật khẩu mới không được trùng mật khẩu cũ
        if (request.getNewPassword().equals(request.getOldPassword())) {
            throw new BadRequestException("Your new password same your old password. Please checking.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        sessionService.deleteAllSessions(user.getUserId());

        sessionService.deleteOtherSessions(user.getUserId(), sessionId);

        return Response.builder()
                .responseMessage("Change password successfully. Please login again with difficult device.")
                .responseCode(responseCodeSuccess)
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
