# User-Service

`User-Service` la service trung tam cho nghiep vu nguoi dung trong du an banking demo. Module nay phu trach:

- dang ky, dang nhap, refresh token, logout
- xac minh email va quan ly mat khau
- quan ly thong tin ca nhan
- quan ly user cho admin
- tiep nhan va xu ly KYC
- giao tiep voi Kafka de gui email/OTP va nhan ket qua AI KYC

## Cong nghe chinh

- `Java 17`
- `Spring Boot`
- `Spring Web`
- `Spring Data JPA`
- `Spring Security`
- `Spring Kafka`
- `Spring Data Redis`
- `MySQL`
- `Keycloak Admin Client`
- `Eureka Client`

## Cau truc chuc nang

- `Controller`
  - `UserController`: endpoint cho auth, profile, admin va KYC
- `Service`
  - `UserService`: facade tong hop
  - `UserAuth`: nhom auth
  - `UserProfileService`: nhom profile
  - `UserAdminService`: nhom admin
  - `KycService`: nhom KYC
- `Repository`
  - repository cho `User`, `VerificationToken`, `UserKycDocument`, ...
- `Model`
  - entity, request dto, response dto, external event dto

## Port mac dinh

Service chay mac dinh tren:

```text
8082
```

## Cau hinh chinh

Trong `src/main/resources/application.yaml`:

- `MYSQL_HOST`: mac dinh `localhost`
- `MYSQL_PORT`: mac dinh `3308`
- `MYSQL_DB_NAME`: mac dinh `mydb`
- `MYSQL_USER`: mac dinh `root`
- `MYSQL_PASSWORD`
- `spring.kafka.bootstrap-servers`: mac dinh `localhost:9092`
- `spring.redis.host`: mac dinh `localhost`
- `spring.redis.port`: mac dinh `6379`
- `app.config.keycloak.server-url`: mac dinh `http://localhost:8080`
- `jwt.secret`

Luu y: file cau hinh hien tai dang chua mot so gia tri nhay cam local. Neu dua len moi truong khac, nen chuyen sang bien moi truong hoac secret manager.

## Yeu cau de chay local

Ban can chuan bi:

- JDK `17`
- Maven
- MySQL
- Redis
- Kafka
- Keycloak

## Cach chay local

Tu thu muc `demo/User-Service`:

```bash
mvn spring-boot:run
```

Hoac build roi chay:

```bash
mvn clean package
java -jar target/User-Service-0.0.1-SNAPSHOT.jar
```

Neu chay tu root module `demo`:

```bash
mvn -pl User-Service spring-boot:run
```

## Kafka topics dang dung

`User-Service` hien giao tiep voi Kafka qua cac topic sau:

- `registration-topic`: gui token xac minh email
- `reset-password`: gui OTP/reset password
- `kyc-ai-check`: day event sang `AI-Service` sau khi submit KYC
- `kyc-ai-result`: nhan ket qua OCR/face match tu `AI-Service`
- `kyc-user`: gui thong bao ket qua xet duyet KYC
- `able-user`: gui thong bao enable/disable tai khoan

## Luong KYC

1. User goi endpoint submit KYC.
2. Service luu `UserKycDocument` voi trang thai `PENDING`.
3. Sau khi transaction commit, service publish `KycAiCheckEvent` len topic `kyc-ai-check`.
4. `AI-Service` xu ly OCR/face match va tra ve `KycAiResultEvent` qua topic `kyc-ai-result`.
5. `User-Service` consume ket qua, cap nhat cac truong:
   - `ocrIdNumber`
   - `ocrFullName`
   - `faceMatchScore`
   - `isPotentiallyFake`
6. Admin co the phe duyet hoac tu choi KYC.

## API chinh

Base path:

```text
/api/users
```

### Auth

- `POST /register`
- `POST /login`
- `POST /verify-device`
- `POST /logout`
- `POST /refresh-token`
- `PUT /change-password`
- `POST /forgot-password`
- `POST /reset-password`
- `POST /send-code`
- `POST /verify-account`

### Profile

- `GET /my-info`
- `PUT /change-contact`
- `PUT /update-profile`

### Admin user

- `PATCH /{id}`
- `GET /{userId}`
- `GET /getUsers?page={page}`
- `PUT /change-profile/{id}`
- `PUT /add-role-admin/{id}`
- `PUT /{userId}/disable`
- `PUT /{userId}/enable`
- `DELETE /{userId}/devices/{deviceId}`
- `DELETE /{userId}/sessions`
- `GET /{userId}/devices`

### KYC

- `POST /kyc`
- `GET /kyc/history`
- `GET /kyc/latest`
- `PATCH /kyc/{kycId}/status`
- `GET /kyc/pending`
- `PUT /kyc/{kycId}/approve`
- `PUT /kyc/{kycId}/reject`
- `GET /kyc/updating`
- `PUT /kyc/{kycId}/profile-change/approve`
- `PUT /kyc/{kycId}/profile-change/reject`

## Mot so model quan trong

- `User`
- `UserProfile`
- `VerificationToken`
- `UserKycDocument`
- `KycAiCheckEvent`
- `KycAiResultEvent`

## Ghi chu hien trang

- `UserService` hien dong vai tro facade tong hop cac service con.
- KYC da duoc noi voi `AI-Service` qua Kafka.
- Session/login flow dang ket hop giua JWT, Redis va device verification.
- Neu ban muon viet test hoac public API docs, nen bo sung them `Swagger/OpenAPI`.
