package com.example.Controller;

import com.example.Model.Dto.Internal.UserCreate;
import com.example.Model.Dto.Internal.StatusUserService.KycStatus;
import com.example.Model.Dto.Internal.UpdateStatus;
import com.example.Model.Dto.Internal.UpdateUserProfile;
import com.example.Model.Dto.Request.ChangePasswordRequest;
import com.example.Model.Dto.Response.AccountEligibilityResponse;
import com.example.Model.Dto.Response.JwtResponse;
import com.example.Model.Dto.Response.Response;
import com.example.Model.Dto.Response.DeviceDto;
import com.example.Model.Dto.Response.UserDto;
import com.example.Model.Dto.Response.UserKycDtoAdmin;
import com.example.Model.Dto.Response.UserKycDtoUser;
import com.example.Model.Dto.Request.LoginRequest;
import com.example.Model.Dto.Request.ResetPasswordRequest;
import com.example.Model.Dto.Request.UpdateUserKyc;
import com.example.Model.Dto.Request.VerifyDeviceRequest;
import com.example.Service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<Response> createUser(@RequestBody UserCreate userDto) {
        log.info("Creating user with: {}", userDto.toString());
        return ResponseEntity.ok(userService.createUser(userDto));
    }

    @PatchMapping("{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response> updateUserUpdate(@PathVariable Long id, @RequestBody UpdateStatus update) {
        log.info("updating the user with: {}", update.toString());
        return new ResponseEntity<>(userService.updateUserStatus(id, update), HttpStatus.OK);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> readUserById(@PathVariable Long userId) {
        log.info("reading user by ID");

        return ResponseEntity.ok(userService.readUserById(userId));
    }

    @GetMapping("/internal/{userId}/account-eligibility")
    public ResponseEntity<AccountEligibilityResponse> getAccountEligibility(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getAccountEligibility(userId));
    }

    @GetMapping("/internal/{userId}/email")
    public ResponseEntity<Map<String, String>> getEmailForInternalServices(@PathVariable Long userId) {
        String email = userService.getEmailByUserIdInternal(userId);
        if (email == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("email", email));
    }

    @GetMapping("/internal/by-email")
    public ResponseEntity<Map<String, Long>> getUserIdByEmailInternal(@RequestParam("email") String email) {
        Long userId = userService.getUserIdByEmailInternal(email);
        if (userId == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("userId", userId));
    }

    @PutMapping("/change-contact")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<UserDto> changeContact(@RequestBody String contactNumber) {
        return ResponseEntity.ok(userService.changeContactNumber(contactNumber));
    }

    @PutMapping("/update-profile")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<UserDto> updateMyProfile(@RequestBody UpdateUserProfile userProfile) {
        return ResponseEntity.ok(userService.changeUserProfile(userProfile));
    }

//    @GetMapping("/account/{accountId}")
//    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
//    public ResponseEntity<UserDto> readUserByAccountId(@PathVariable String accountId) {
//        return ResponseEntity.ok(userService.readUserByAccountId(accountId));
//    }

    @PostMapping("/send-code")
    public ResponseEntity<Response> sendCode(@RequestParam("email") String email) {
        return ResponseEntity.ok(userService.sendCode(email));
    }

    @PostMapping("/verify-account")
    public ResponseEntity<Response> verifyAccount(@RequestParam("token") String token) {
        Response response = userService.verifyToken(token);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/getUsers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDto>> getUsers(@RequestParam("page") int page) {
        return ResponseEntity.ok(userService.readAllUsers(page));
    }

    @GetMapping("/my-info")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<UserDto> getMyInformation() {
        return ResponseEntity.ok(userService.getMyInfo());
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(userService.login(loginRequest));
    }

    @PostMapping("/verify-device")
    public ResponseEntity<JwtResponse> verifyDeviceAndLogin(@RequestBody VerifyDeviceRequest request) {
        return ResponseEntity.ok(userService.verifyDeviceAndLogin(request));
    }

    @PostMapping("/logout")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Response> logout() {
        return ResponseEntity.ok(userService.logout());
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<JwtResponse> refreshToken(
            @RequestParam("refreshToken") String refreshToken
    ) {
        return ResponseEntity.ok(userService.refreshToken(refreshToken));
    }

    @PutMapping("/change-password")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<Response> changePassword(
            @RequestBody ChangePasswordRequest request
    ) {
        return ResponseEntity.ok(userService.changePassword(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Response> forgotPassword(@RequestParam("email") String email) {
        return ResponseEntity.ok(userService.forgotPassword(email));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Response> resetPassword(@RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(userService.resetPassword(request));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<UserDto> getAccountId(@PathVariable Long accountId) {
        return ResponseEntity.ok(userService.readUserByAccountId(accountId));
    }

    @PutMapping("/change-profile/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response> updateProfileUser(@PathVariable Long id, @RequestBody UpdateUserProfile userProfile) {
            return ResponseEntity.ok(userService.updateUserProfile(id, userProfile));
    }

    @PutMapping("/add-role-admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response> updateRoleToUser(@PathVariable Long id) {
        return ResponseEntity.ok(userService.addAdminRole(id));
    }

    @PutMapping("/{userId}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response> disableUser(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.disableUser(userId));
    }

    @PutMapping("/{userId}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response> enableUser(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.enableUser(userId));
    }

    @DeleteMapping("/{userId}/devices/{deviceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response> revokeDevice(@PathVariable Long userId, @PathVariable String deviceId) {
        return ResponseEntity.ok(userService.revokeDevice(userId, deviceId));
    }

    @DeleteMapping("/{userId}/sessions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response> forceLogout(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.forceLogout(userId));
    }

    @GetMapping("/{userId}/devices")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DeviceDto>> getUserDevices(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserDevices(userId));
    }

    @PostMapping("/kyc")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<UserKycDtoUser> submitKyc(@RequestBody UpdateUserKyc request) {
        return ResponseEntity.ok(userService.submitKyc(request));
    }

    @GetMapping("/kyc/history")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<UserKycDtoUser>> getMyKycHistory() {
        return ResponseEntity.ok(userService.getMyKycHistory());
    }

    @GetMapping("/kyc/latest")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<UserKycDtoUser> getLatestKyc() {
        return ResponseEntity.ok(userService.getLatestKyc());
    }

    @PatchMapping("/kyc/{kycId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response> updateKycStatus(
            @PathVariable Long kycId,
            @RequestParam("status") KycStatus status,
            @RequestParam(value = "reason", required = false) String reason
    ) {
        return ResponseEntity.ok(userService.updateKycStatus(kycId, status, reason));
    }

    @GetMapping("/kyc/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserKycDtoAdmin>> getPendingKyc(Pageable pageable) {
        return ResponseEntity.ok(userService.getPendingKyc(pageable));
    }

    @PutMapping("/kyc/{kycId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response> approveKyc(
            @PathVariable Long kycId,
            @RequestParam(value = "adminNote", required = false) String adminNote
    ) {
        return ResponseEntity.ok(userService.approveKyc(kycId, adminNote));
    }

    @PutMapping("/kyc/{kycId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response> rejectKyc(
            @PathVariable Long kycId,
            @RequestParam("rejectionReason") String rejectionReason,
            @RequestParam(value = "adminNote", required = false) String adminNote
    ) {
        return ResponseEntity.ok(userService.rejectKyc(kycId, rejectionReason, adminNote));
    }

    @GetMapping("/kyc/updating")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserKycDtoAdmin>> getUpdatingKyc(Pageable pageable) {
        return ResponseEntity.ok(userService.getUpdateKyc(pageable));
    }

    @PutMapping("/kyc/{kycId}/profile-change/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response> approveProfileChange(
            @PathVariable Long kycId,
            @RequestParam(value = "adminNote", required = false) String adminNote
    ) {
        return ResponseEntity.ok(userService.approveProfileChange(kycId, adminNote));
    }

    @PutMapping("/kyc/{kycId}/profile-change/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Response> rejectProfileChange(
            @PathVariable Long kycId,
            @RequestParam("reason") String reason
    ) {
        return ResponseEntity.ok(userService.rejectProfileChange(kycId, reason));
    }

}
