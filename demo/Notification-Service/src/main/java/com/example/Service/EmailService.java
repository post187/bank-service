package com.example.Service;

import com.example.Model.Dto.EmailDetail;
import org.springframework.web.multipart.MultipartFile;

public interface EmailService {
    String sendSimpleMail(EmailDetail emailDetail);
    String sendMailWithAttachment(EmailDetail detail);
    String sendMail(MultipartFile [] files, String to, String [] cc, String subject, String body);

}
