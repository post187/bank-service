package com.example.Model.Dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmailSender {
    private MultipartFile [] files;
    private String to;
    private String cc;
    private String subject;
    private String body;
}
