# Transaction-Service

Service xử lý **giao dịch tiền** (nạp, rút, chuyển nội bộ): lưu trạng thái orchestration + **idempotency**, kiểm tra **hạn mức rút/chuyển trong ngày**, gọi **Account-Service** để ghi **sổ cái kép** (journal), đồng thời phát sự kiện Kafka khi giao dịch **POSTED**.

## Vai trò trong hệ thống

| Luồng | Mô tả |
|--------|--------|
| **Deposit** | Ghi nhận tiền vào tài khoản khách (CREDIT tài khoản đích, đối ứng DEBIT tài khoản clearing). |
| **Withdraw** | Trừ tiền từ tài khoản khách (DEBIT tài khoản nguồn, CREDIT clearing). |
| **Internal transfer** | Chuyển giữa hai tài khoản (DEBIT nguồn, CREDIT đích). Người gọi phải sở hữu tài khoản **nguồn**; tài khoản đích có thể thuộc user khác. |

- **User-Service**: xác định user hiện tại qua `GET /api/users/my-info` (JWT của client được **forward** qua Feign).
- **Account-Service**: đăng bút toán qua `POST /api/accounts/journals` (cùng JWT), đọc tài khoản qua `GET /api/accounts/{id}`.
- **PostgreSQL**: bảng `monetary_transactions` (trạng thái saga cục bộ, khóa idempotency).
- **Kafka**: topic `transaction-completed` sau khi commit DB thành công (ví dụ cho Notification-Service).

## Yêu cầu chạy

1. **PostgreSql** (mặc định DB `txdb`, user/password theo `application.yaml` hoặc biến môi trường).
2. **Kafka** (`KAFKA_BOOTSTRAP`, mặc định `localhost:9092`).
3. **User-Service** (8082) và **Account-Service** (8085) đã chạy, JWT ký bằng cùng **`jwt.secret`** với User-Service.
4. Trong Account-Service phải tồn tại tài khoản **clearing** (nội bộ) với ID trùng `transaction.clearing-account-id` (mặc định `1`). Tài khoản này dùng để cân đối nghiệp vụ kép cho nạp/rút.

## Cấu hình

| Biến / property | Mặc định | Ý nghĩa |
|-----------------|----------|---------|
| `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | localhost, 5432, txdb, postgres, postgres | Kết nối PostgreSQL |
| `KAFKA_BOOTSTRAP` | localhost:9092 | Kafka brokers |
| `ACCOUNT_SERVICE_URL` | http://localhost:8085 | Base URL Account-Service |
| `USER_SERVICE_URL` | http://localhost:8082 | Base URL User-Service |
| `CLEARING_ACCOUNT_ID` | 1 | `accountId` clearing trong Account-Service |
| `DAILY_TX_LIMIT` | 100000.00 | Tổng hạn mức **rút + chuyển** POSTED trong ngày (theo user, cùng múi giờ máy chủ) |
| `jwt.secret` | (trong yaml) | Phải trùng User-Service để validate Bearer token |

Eureka đang **tắt** (`eureka.client.enabled: false`); có thể bật lại nếu dùng service discovery.

## Chạy

```bash
cd demo/Transaction-Service
mvn spring-boot:run
```

Hoặc từ thư mục `demo`:

```bash
mvn -pl Transaction-Service spring-boot:run
```

Ứng dụng lắng nghe cổng **8086**.

## API

Tất cả endpoint yêu cầu header **`Authorization: Bearer <access_token>`** (role `USER` hoặc `ADMIN`) và header **`Idempotency-Key`** (bắt buộc, tối đa 128 ký tự, nên dùng UUID). Cùng một key sẽ trả về cùng kết quả giao dịch đã lưu (idempotent).

### Nạp tiền — `POST /api/transactions/deposits`

```json
{
  "toAccountId": 2,
  "amount": 100.00,
  "currency": "VND"
}
```

`currency` có thể bỏ qua; khi đó lấy theo tiền tệ tài khoản đích. Chỉ cho phép nạp vào tài khoản **thuộc user đang đăng nhập**.

### Rút tiền — `POST /api/transactions/withdrawals`

```json
{
  "fromAccountId": 2,
  "amount": 50.00,
  "currency": "VND"
}
```

Tính vào **hạn mức outbound trong ngày**.

### Chuyển khoản nội bộ — `POST /api/transactions/transfers`

```json
{
  "fromAccountId": 2,
  "toAccountId": 3,
  "amount": 25.00,
  "currency": "VND"
}
```

Hai tài khoản phải cùng loại tiền; user phải sở hữu tài khoản nguồn. Tính vào hạn mức outbound.

### Ví dụ `curl`

```bash
curl -s -X POST http://localhost:8086/api/transactions/deposits \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -H "Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000" \
  -H "Content-Type: application/json" \
  -d "{\"toAccountId\":2,\"amount\":100,\"currency\":\"VND\"}"
```

### Phản hồi (`TransactionResponse`)

Các trường chính: `id`, `idempotencyKey`, `kind` (`DEPOSIT` | `WITHDRAW` | `INTERNAL_TRANSFER`), `status` (`PENDING` | `POSTED` | `FAILED`), `amount`, `currency`, `fromAccountId`, `toAccountId`, `initiatorUserId`, `ledgerJournalId`, `failureReason`, `createdAt`, `completedAt`.

## Kiến trúc & lý thuyết (tóm tắt)

- **ACID (PostgreSQL)**: Ghi nhận giao dịch cục bộ với isolation `READ_COMMITTED`; journal thực tế commit trên **Account-Service** (cơ sở dữ liệu riêng) — đây là **giao dịch phân tán** theo kiểu **orchestration**: bước 1 lưu bản ghi + bước 2 gọi ledger; nếu ledger lỗi, bản ghi chuyển `FAILED` (bù trừ tự động phức tạp hơn cần **Saga** bổ sung).
- **Idempotency**: Khóa duy nhất `idempotency_key`; trùng request lặp lại không tạo bút toán thứ hai (Account-Service cũng chống trùng theo `referenceType` + `referenceId`).
- **Hạn mức ngày**: Chỉ cộng dồn giao dịch **WITHDRAW** và **INTERNAL_TRANSFER** đã `POSTED` trong ngày (theo `completedAt`).
- **Account-Service**: Bút toán đa dòng khóa tài khoản theo thứ tự ID (**pessimistic lock**) để giảm deadlock/lost update khi cập nhật số dư.

## Kafka

Sau khi trạng thái `POSTED` và transaction DB commit, producer gửi message lên topic **`transaction-completed`** (key = `transactionId`, value JSON `TransactionCompletedEvent`).

## Lưu ý vận hành

- Đảm bảo **clearing account** tồn tại, **ACTIVE**, cùng tiền tệ với giao dịch, và có đủ logic nghiệp vụ (ví dụ nạp cần đủ “nguồn” phía clearing — trong môi trường demo thường cấp số dư ban đầu cho tài khoản clearing).
- Token hết hạn hoặc sai secret sẽ trả **401** từ resource server.
- Gọi Account/User khi service đó sập sẽ dẫn tới `FAILED` hoặc lỗi HTTP tùy tầng Feign/xử lý exception.
