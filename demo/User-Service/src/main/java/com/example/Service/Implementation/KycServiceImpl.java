package com.example.Service.Implementation;

import com.example.Exception.ResourceNotFoundException;
import com.example.Jwt.CustomerAuthentication.CustomAuthentication;
import com.example.Jwt.UserDetail.UserPrinciple;
import com.example.Model.Dto.External.KycAiCheckEvent;
import com.example.Model.Dto.External.KycAiResultEvent;
import com.example.Model.Dto.Internal.Status.KycStatus;
import com.example.Model.Dto.Request.UpdateUserKyc;
import com.example.Model.Dto.Response.Response;
import com.example.Model.Dto.Response.UserKycDtoUser;
import com.example.Model.Entity.User;
import com.example.Model.Entity.UserKycDocument;
import com.example.Model.Mapper.UserKycMapper;
import com.example.Repository.UserKycRepository;
import com.example.Repository.UserRepository;
import com.example.Service.KycService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KycServiceImpl implements KycService {
    private final UserKycRepository userKycRepository;
    private final UserRepository userRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private UserKycMapper userKycMapper = new UserKycMapper();

    @Value("${spring.application.success}")
    private String responseCodeSuccess;

    @Value("${spring.application.not_found}")
    private String responseCodeNotFound;

    private String getMyEmail() {
        Authentication customAuthentication = (CustomAuthentication) SecurityContextHolder.getContext().getAuthentication();
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
    @Transactional
    public UserKycDtoUser submitKyc(UpdateUserKyc request) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }
        if (request.getSubmittedIdentificationNumber() == null || request.getSubmittedIdentificationNumber().isBlank()) {
            throw new IllegalArgumentException("submittedIdentificationNumber is required");
        }
        if (request.getSubmittedFullName() == null || request.getSubmittedFullName().isBlank()) {
            throw new IllegalArgumentException("submittedFullName is required");
        }
        if (request.getDocumentType() == null) {
            throw new IllegalArgumentException("documentType is required");
        }
        if (request.getFrontCardUrl() == null || request.getFrontCardUrl().isBlank()) {
            throw new IllegalArgumentException("frontCardUrl is required");
        }
        if (request.getBackCardUrl() == null || request.getBackCardUrl().isBlank()) {
            throw new IllegalArgumentException("backCardUrl is required");
        }
        if (request.getSelfieUrl() == null || request.getSelfieUrl().isBlank()) {
            throw new IllegalArgumentException("selfieUrl is required");
        }
        User user = userRepository.findByEmail(getMyEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (userKycRepository.existsByUser_UserIdAndStatus(user.getUserId(), KycStatus.PENDING)) {
            throw new IllegalStateException("Your KYC request is already pending review");
        }
        UserKycDocument userKyc = UserKycDocument.builder()
                .user(user)
                .submittedIdentificationNumber(request.getSubmittedIdentificationNumber().trim())
                .submittedFullName(request.getSubmittedFullName().trim())
                .documentType(request.getDocumentType())
                .frontCardUrl(request.getFrontCardUrl().trim())
                .backCardUrl(request.getBackCardUrl().trim())
                .selfieUrl(request.getSelfieUrl().trim())
                .status(KycStatus.PENDING)
                .submittedAt(LocalDateTime.now())
                .reviewedAt(null)
                .reviewedBy(null)
                .adminNote(null)
                .rejectionReason(null)
                .isPotentiallyFake(false)
                .build();
        UserKycDocument saved = userKycRepository.save(userKyc);

        KycAiCheckEvent event = KycAiCheckEvent.builder()
                .kycId(saved.getId())
                .userId(user.getUserId())
                .frontCardUrl(saved.getFrontCardUrl())
                .backCardUrl(saved.getBackCardUrl())
                .selfieUrl(saved.getSelfieUrl())
                .submittedIdentificationNumber(saved.getSubmittedIdentificationNumber())
                .submittedFullName(saved.getSubmittedFullName())
                .build();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafkaTemplate.send("kyc-ai-check", request.getKycId(), event);
            }
        });
        return userKycMapper.toUserDto(saved);
    }

    @Override
    public List<UserKycDtoUser> getMyKycHistory() {
        String email = getMyEmail();
        return userKycRepository.findByUser_EmailOrderBySubmittedAtDesc(email)
                .stream()
                .map(userKycMapper::toUserDto)
                .toList();
    }

    @Override
    @Transactional
    public void processAiResult(KycAiResultEvent resultEvent) {
        if (resultEvent == null || resultEvent.getKycId() == null) {
            throw new IllegalArgumentException("Invalid AI result event");
        }
        UserKycDocument userKyc = userKycRepository.findById(resultEvent.getKycId())
                .orElseThrow(() -> new ResourceNotFoundException("KYC request not found"));
        // Chỉ xử lý khi hồ sơ còn pending (tránh ghi đè hồ sơ đã admin duyệt)
        if (userKyc.getStatus() != KycStatus.PENDING) {
            return;
        }
        userKyc.setOcrIdNumber(resultEvent.getOcrIdNumber());
        userKyc.setOcrFullName(resultEvent.getOcrFullName());
        userKyc.setFaceMatchScore(resultEvent.getFaceMatchScore());
        boolean idMismatch = !safeEqualsNormalized(
                userKyc.getSubmittedIdentificationNumber(),
                resultEvent.getOcrIdNumber()
        );
        boolean nameMismatch = !safeEqualsNormalized(
                userKyc.getSubmittedFullName(),
                resultEvent.getOcrFullName()
        );
        boolean lowFaceScore = resultEvent.getFaceMatchScore() == null
                || resultEvent.getFaceMatchScore() < 0.75;
        boolean aiFlaggedFake = Boolean.TRUE.equals(resultEvent.getPotentiallyFake());
        userKyc.setPotentiallyFake(idMismatch || nameMismatch || lowFaceScore || aiFlaggedFake);
        userKycRepository.save(userKyc);
    }

    @Override
    public UserKycDtoUser getLatestKyc() {
        String email = getMyEmail();
        UserKycDocument latest = userKycRepository
                .findTopByUser_EmailOrderBySubmittedAtDesc(email)
                .orElseThrow(() -> new ResourceNotFoundException("No KYC record found"));
        return userKycMapper.toUserDto(latest);
    }

    @Override
    @Transactional
    public Response updateKycStatus(Long kycId, KycStatus status, String reason) {
        if (kycId == null) {
            throw new IllegalArgumentException("kycId is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }

        UserKycDocument userKyc = userKycRepository.findById(kycId)
                .orElseThrow(() -> new ResourceNotFoundException("KYC request not found"));

        KycStatus currentStatus = userKyc.getStatus();

        // Guard transition đơn giản
        if (currentStatus == KycStatus.VERIFIED && status != KycStatus.EXPIRED) {
            throw new IllegalStateException("Verified KYC cannot be changed to this status");
        }

        if (status == KycStatus.REJECTED) {
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("reason is required when rejecting KYC");
            }
            userKyc.setRejectionReason(reason.trim());
        } else {
            userKyc.setRejectionReason(null);
        }

        userKyc.setStatus(status);

        // Chỉ set reviewed info khi là quyết định cuối
        if (status == KycStatus.VERIFIED || status == KycStatus.REJECTED) {
            userKyc.setReviewedAt(LocalDateTime.now());
            userKyc.setReviewedBy(getMyEmail()); // hoặc "ADMIN" nếu bạn chưa có auth context
        } else {
            userKyc.setReviewedAt(null);
            userKyc.setReviewedBy(null);
        }

        userKycRepository.save(userKyc);

        // Optional notify
        if (status == KycStatus.VERIFIED) {
            kafkaTemplate.send("kyc-user", userKyc.getUser().getEmail(),
                    "Your KYC verification has been approved successfully.");
        } else if (status == KycStatus.REJECTED) {
            kafkaTemplate.send("kyc-user", userKyc.getUser().getEmail(),
                    "Your KYC verification was rejected. Reason: " + userKyc.getRejectionReason());
        }

        return Response.builder()
                .responseCode(responseCodeSuccess)
                .responseMessage("KYC status updated to " + status)
                .build();
    }

    private boolean safeEqualsNormalized(String a, String b) {
        if (a == null || b == null) return false;
        return normalize(a).equals(normalize(b));
    }
    private String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ").toLowerCase();
    }
}
