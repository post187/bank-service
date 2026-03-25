package com.example.Service.Implementation;

import com.example.Exception.EmptyFields;
import com.example.Exception.ResourceNotFoundException;
import com.example.Model.Dto.Internal.Status.KycStatus;
import com.example.Model.Dto.Internal.Status.Status;
import com.example.Model.Dto.Internal.UpdateStatus;
import com.example.Model.Dto.Internal.UpdateUserProfile;
import com.example.Model.Dto.Response.DeviceDto;
import com.example.Model.Dto.Response.Response;
import com.example.Model.Dto.Response.UserDto;
import com.example.Model.Dto.Response.UserKycDtoAdmin;
import com.example.Model.Entity.User;
import com.example.Model.Entity.UserKycDocument;
import com.example.Model.Mapper.UserKycMapper;
import com.example.Model.Mapper.UserMapper;
import com.example.Repository.UserKycRepository;
import com.example.Repository.UserRepository;
import com.example.Service.UserAdminService;
import com.example.Utils.FieldChecked;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

import static com.example.Constant.AppConstant.NUMBER_OF_PAGE;
import static java.util.Arrays.stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements UserAdminService {
    private final UserRepository userRepository;
    private final KafkaTemplate<String, Object> kafka;
    private final SessionService sessionService;
    private final StringRedisTemplate redis;
    private final UserKycRepository userKycRepository;
    private final UserKycMapper userKycMapper;
    private UserMapper userMapper = new UserMapper();

    @Value("${spring.application.success}")
    private String responseCodeSuccess;

    @Value("${spring.application.not_found}")
    private String responseCodeNotFound;

    /**
     * Update user status
     * @param id
     * @param userUpdate
     * @return Response
     * @throws EmptyFields if user is not updated completely
     */
    @Override
    @Transactional
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

    /**
     * Add admin role to user
     * @param userId
     * @return Response
     * @throws ResourceNotFoundException if user not found
     */
    @Override
    public Response addAdminRole(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.getRoles().add("ADMIN");

        return Response.builder()
                .responseMessage("Add role to user successfully")
                .responseCode(responseCodeSuccess)
                .build();
    }

    /**
     * Revoke device from user
     * @param userId
     * @param deviceId
     * @return Response
     */
    @Override
    public Response revokeDevice(Long userId, String deviceId) {
        sessionService.deleteOtherSessions(userId, deviceId);

        return Response.builder()
                .responseMessage("Revoke device successfully")
                .responseCode(responseCodeSuccess)
                .build();
    }

    /**
     * Force logout user from all devices
     * @param userId
     * @return Response
     */
    @Override
    public Response forceLogout(Long userId) {
        sessionService.deleteAllSessions(userId);
        return Response.builder()
                .responseMessage("Force logout successfully")
                .responseCode(responseCodeSuccess)
                .build();
    }

    /**
     * Get user devices
     * @param userId
     * @return List<DeviceDto>
     */
    @Override
    public List<DeviceDto> getUserDevices(Long userId) {
        return sessionService.getUserDevicesBrief(userId)
                .stream()
                .map(device -> DeviceDto.builder()
                        .deviceId(device.get("deviceId"))
                        .deviceName(device.get("deviceName"))
                        .build())
                .toList();
    }

    @Override
    public Page<UserKycDtoAdmin> getPendingKyc(Pageable pageable) {
        Pageable effectivePageable = pageable != null ? pageable : Pageable.unpaged();
        Page<UserKycDocument> userKycPage =
                userKycRepository.findByStatus(KycStatus.PENDING, effectivePageable);
        return userKycPage.map(userKycMapper::toDto);
    }

    @Override
    public Response approveKyc(Long kycId, String adminNote) {
        UserKycDocument userKyc = userKycRepository.findById(kycId)
                .orElseThrow(() -> new ResourceNotFoundException("KYC request not found"));

        userKyc.setStatus(KycStatus.VERIFIED);
        userKyc.setAdminNote(adminNote);
        userKyc.setReviewedAt(LocalDateTime.now());
        userKyc.setReviewedBy("ADMIN");
        userKyc.setRejectionReason(null);

        userKycRepository.save(userKyc);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafka.send(
                        "kyc-user",
                        userKyc.getUser().getEmail(),
                        "Dear customer, your KYC verification has been approved successfully. You can now access all verified account features."
                );
            }
        });
        return Response.builder()
                .responseCode(responseCodeSuccess)
                .responseMessage("Approve KYC successfully")
                .build();
    }

    @Override
    @Transactional
    public Response rejectKyc(Long kycId, String rejectionReason, String adminNote) {
        UserKycDocument userKyc = userKycRepository.findById(kycId)
                .orElseThrow(() -> new ResourceNotFoundException("KYC request not found"));

        userKyc.setRejectionReason(rejectionReason);
        userKyc.setAdminNote(adminNote);
        userKyc.setReviewedAt(LocalDateTime.now());
        userKyc.setReviewedBy("ADMIN");
        userKyc.setStatus(KycStatus.REJECTED);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                String payload = String.format(
                        "Dear customer, your KYC verification request" +
                                " has been rejected. Reason: %s. " +
                                "Please update your information and " +
                                "resubmit your documents.",
                        rejectionReason
                );
                kafka.send("kyc-user", userKyc.getUser().getEmail(), payload);
            }
        });

        userKycRepository.save(userKyc);

        return Response.builder()
                .responseCode(responseCodeSuccess)
                .responseMessage("Reject KYC successfully")
                .build();
    }

    @Override
    public Page<UserKycDtoAdmin> getUpdateKyc(Pageable pageable) {
        Pageable effectivePageable = pageable != null ? pageable : Pageable.unpaged();
        Page<UserKycDocument> userKycPage =
                userKycRepository.findByStatus(KycStatus.UPDATING, effectivePageable);
        return userKycPage.map(userKycMapper::toDto);
    }

    @Override
    @Transactional
    public Response approveProfileChange(Long kycId, String adminNote) {
        UserKycDocument userKyc = userKycRepository.findById(kycId)
                .orElseThrow(() -> new ResourceNotFoundException("KYC request not found"));

        userKyc.setStatus(KycStatus.VERIFIED);
        userKyc.setAdminNote(adminNote);
        userKyc.setReviewedAt(LocalDateTime.now());
        userKyc.setReviewedBy("ADMIN");
        userKyc.setRejectionReason(null);

        userKycRepository.save(userKyc);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafka.send(
                        "kyc-user",
                        userKyc.getUser().getEmail(),
                        "Dear customer, your KYC verification has been approved successfully. You can now access all verified account features."
                );
            }
        });
        return Response.builder()
                .responseCode(responseCodeSuccess)
                .responseMessage("Approve KYC successfully")
                .build();
    }

    @Override
    @Transactional
    public Response rejectProfileChange(Long kycId, String reason) {
        UserKycDocument userKyc = userKycRepository.findById(kycId)
                .orElseThrow(() -> new ResourceNotFoundException("KYC request not found"));

        userKyc.setRejectionReason(reason);
        userKyc.setReviewedAt(LocalDateTime.now());
        userKyc.setReviewedBy("ADMIN");
        userKyc.setStatus(KycStatus.REJECTED);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                String payload = String.format(
                        "Dear customer, your KYC verification request" +
                                " has been rejected. Reason: %s. " +
                                "Please update your information and " +
                                "resubmit your documents.",
                        reason
                );
                kafka.send("kyc-user", userKyc.getUser().getEmail(), payload);
            }
        });

        userKycRepository.save(userKyc);

        return Response.builder()
                .responseCode(responseCodeSuccess)
                .responseMessage("Reject KYC successfully")
                .build();
    }

    @Override
    @Transactional
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
    public UserDto readUserById(Long userId) {
        return userRepository.findById(userId)
                .map(user -> userMapper.convertToDto(user))
                .orElseThrow(() -> new ResourceNotFoundException("User not found on the server"));
    }

    @Override
    @Transactional
    public Response disableUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found on the servers"));

        user.setEnable(false);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafka.send("able-user", user.getEmail(), "Your account bank service is disable because you engaging in fraudulent behavior.");
            }
        });
        return Response.builder()
                .responseCode(responseCodeSuccess)
                .responseMessage("Account + " + userId + " disable.")
                .build();
    }

    @Override
    @Transactional
    public Response enableUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found on the servers"));

        user.setEnable(true);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafka.send("able-user", user.getEmail(), "Your account bank enabled.");
            }
        });
        return Response.builder()
                .responseCode(responseCodeSuccess)
                .responseMessage("Account + " + userId + " enabled.")
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