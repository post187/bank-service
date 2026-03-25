# AI-Service

`AI-Service` duoc trien khai bang Python va bat Kafka binh thuong. Ban nay da dung AI/CV that o muc co the chay du an ca nhan:

- consume topic `kyc-ai-check`
- OCR CCCD tieng Viet bang `PaddleOCR`
- face verification bang `DeepFace`
- fallback sang `OpenCV` histogram neu `DeepFace` chua san sang
- publish ket qua sang `kyc-ai-result`

## Chay local

1. Tao virtualenv va cai dependency:

```bash
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
```

2. Cau hinh bien moi truong:

```bash
copy .env.example .env
```

3. Chay service:

```bash
python -m app.main
```

## Bien moi truong

- `KAFKA_BOOTSTRAP_SERVERS`: mac dinh `localhost:9092`
- `KAFKA_INPUT_TOPIC`: mac dinh `kyc-ai-check`
- `KAFKA_OUTPUT_TOPIC`: mac dinh `kyc-ai-result`
- `KAFKA_GROUP_ID`: mac dinh `ai-service-group`
- `AI_MODE`: `ocr` hoac `mock`, mac dinh `ocr`
- `OCR_LANG`: ngon ngu OCR, mac dinh `vi`
- `HTTP_TIMEOUT_SECONDS`: timeout khi tai anh
- `FACE_MATCH_THRESHOLD`: nguong canh bao face match
- `FACE_MATCH_DEFAULT_SCORE`: diem fallback neu khong detect duoc mat
- `DEEPFACE_MODEL`: model face matching, mac dinh `Facenet512`
- `DEEPFACE_DETECTOR_BACKEND`: backend detect mat cho `DeepFace`, mac dinh `opencv`

## Luong xu ly hien tai

- Nhan `KycAiCheckEvent` tu `User-Service`
- Xac thuc payload co `kycId`
- Tai anh CCCD va selfie tu URL
- OCR mat truoc CCCD qua nhieu variant tien xu ly anh de lay so CCCD va ho ten
- So khop mat giua anh the va selfie bang `DeepFace`
- Tao `KycAiResultEvent`
- Gui ket qua ve topic `kyc-ai-result`

## Luu y thuc te

- Da co rule uu tien format `CCCD` Viet Nam 12 so va nhan dien nhan `Ho va ten`.
- Neu `DeepFace` khong khoi dong duoc tren may hien tai, service se tu fallback ve so khop histogram de khong lam dung luong Kafka.
- Neu may cua ban gap loi cai `paddlepaddle` tren Windows, hay cai dung ban CPU tu trang Paddle phu hop Python version dang dung.
- Mot so may Windows co the can cai them phu thuoc cho `deepface` va backend cua no.

## Buoc tiep theo

- Them OCR cho mat sau CCCD
- Them confidence score chi tiet vao event tra ve
- Them check ngay sinh, gioi tinh, que quan neu ban mo rong event
