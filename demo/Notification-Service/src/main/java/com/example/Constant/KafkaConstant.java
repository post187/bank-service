package com.example.Constant;

import org.springframework.beans.factory.annotation.Value;

public class KafkaConstant {
    public static final String USER_CREATED = "registration-topic";
    public static final String SEND_EMAIL_VERIFY = "send-email-verify-topic";
    public static final String GROUP_ID = "notification-topic";
}
