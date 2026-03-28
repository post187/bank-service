package com.example.Constant;

/**
 * Topic Kafka phát từ User / Account / Transaction — cần khớp với producer.
 */
public final class KafkaTopics {

    public static final String REGISTRATION = "registration-topic";
    public static final String RESET_PASSWORD = "reset-password";
    public static final String KYC_USER = "kyc-user";
    public static final String ABLE_USER = "able-user";
    public static final String VERIFY_NEW_DEVICE = "verify-new-device";

    public static final String ACCOUNT_CREATED = "account-created";
    public static final String ACCOUNT_STATUS_CHANGED = "account-status-changed";
    public static final String ACCOUNT_CLOSED = "account-closed";
    public static final String LEDGER_JOURNAL_POSTED = "ledger-journal-posted";
    public static final String HOLD_CREATED = "hold-created";
    public static final String HOLD_RELEASED = "hold-released";
    public static final String ACCOUNT_SNAPSHOT_CREATED = "account-snapshot-created";

    public static final String TRANSACTION_COMPLETED = "transaction-completed";

    private KafkaTopics() {
    }
}
