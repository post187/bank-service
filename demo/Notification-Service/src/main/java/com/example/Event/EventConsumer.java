package com.example.Event;

import com.example.Config.Kafka.KafkaProperties;
import com.example.Constant.KafkaConstant;
import com.example.Model.Document.Email;
import com.example.Model.Dto.EmailDetail;
import com.example.Repository.EmailRepository;
import com.example.Service.EmailService;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@Slf4j
@RequiredArgsConstructor
public class EventConsumer {
    Gson gson = new Gson();

    private final EmailService emailService;
    private final EmailRepository emailRepository;



    @KafkaListener(topics = KafkaConstant.USER_CREATED, groupId = KafkaConstant.GROUP_ID)
    public void consumeRegistrationEmail(
            @Header(KafkaHeaders.RECEIVED_KEY) String email,
            @Payload String token,
            Acknowledgment ack
    ) {
        Email emailLog = Email.builder()
                .recipient(email)
                .subject("Verify Account")
                .body("Your token: " + token)
                .status("SENDING")
                .build();
        emailRepository.save(emailLog);

        try {
            EmailDetail detail = new EmailDetail();
            detail.setRecipient(email);
            detail.setSubject(emailLog.getSubject());
            detail.setMsBody(emailLog.getBody());

            // 3. Gọi EmailService thực hiện gửi mail (Nhớ thêm .send() trong EmailServiceImpl)
            emailService.sendSimpleMail(detail);

            // 4. Cập nhật trạng thái thành công
            emailLog.setStatus("SUCCESS");
            emailRepository.save(emailLog);

            // 5. Xác nhận với Kafka là đã xử lý xong (Không gửi lại nữa)
            ack.acknowledge();
            log.info("Gửi email thành công tới: {}", email);

        } catch (Exception e) {
            log.error("Lỗi khi gửi email cho {}: {}", email, e.getMessage());

            emailLog.setStatus("FAILED");
            emailLog.setErrorMessage(e.getMessage());
            emailRepository.save(emailLog);
        }

    }
}
