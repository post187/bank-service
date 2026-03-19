package com.example.Service.Impl;

import com.example.Constant.AppConstant;
import com.example.Model.Dto.EmailDetail;
import com.example.Service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.message.SimpleMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender javaMailSender;
    @Value("${spring.mail.username}")
    private String fromEmail;
    @Override
    public String sendSimpleMail(EmailDetail emailDetail) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(emailDetail.getRecipient());
        message.setText(emailDetail.getMsBody());
        message.setSubject(emailDetail.getSubject());

        javaMailSender.send(message);

        return AppConstant.MAIL_SEND_SUCCESS;
    }

    @Override
    public String sendMailWithAttachment(EmailDetail detail) {
        MimeMessage message = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = null;
        try {
            helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromEmail);
            helper.setTo(detail.getRecipient());
            helper.setText(detail.getMsBody());
            helper.setSubject(detail.getSubject());
            FileSystemResource file = new FileSystemResource(new File(detail.getAttachment()));

            helper.addAttachment(Objects.requireNonNull(file.getFilename()), file);

            javaMailSender.send(message);

            return AppConstant.MAIL_SEND_SUCCESS;
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String sendMail(MultipartFile[] files, String to, String[] cc, String subject, String body) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper = null;
        try {
            mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);
            mimeMessageHelper.setFrom(fromEmail);
            mimeMessageHelper.setTo(to);
            mimeMessageHelper.setCc(cc);
            mimeMessageHelper.setSubject(subject);
            mimeMessageHelper.setText(body);

            for (MultipartFile file : files) {
                mimeMessageHelper.addAttachment(
                        Objects.requireNonNull(file.getOriginalFilename()),
                        new ByteArrayResource(file.getBytes())
                );
            }
            return AppConstant.MAIL_SEND_SUCCESS;
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
