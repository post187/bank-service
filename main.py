import json
import logging
import os
import re
import signal
import sys
import unicodedata
from pathlib import Path
from typing import Any
from urllib.parse import urlparse

import cv2
import numpy as np
import requests
from kafka import KafkaConsumer, KafkaProducer
from paddleocr import PaddleOCR

try:
    from deepface import DeepFace
except Exception:  # pragma: no cover - optional dependency at runtime
    DeepFace = None


logging.basicConfig(
    level=os.getenv("LOG_LEVEL", "INFO"),
    format="%(asctime)s %(levelname)s %(name)s - %(message)s",
)
logger = logging.getLogger("ai-service")


RUNNING = True


def load_env_file(path: str = ".env") -> None:
    if not os.path.exists(path):
        return

    with open(path, "r", encoding="utf-8") as env_file:
        for raw_line in env_file:
            line = raw_line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            os.environ.setdefault(key.strip(), value.strip())


def env(name: str, default: str) -> str:
    value = os.getenv(name, default).strip()
    return value if value else default


load_env_file()


BOOTSTRAP_SERVERS = env("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
INPUT_TOPIC = env("KAFKA_INPUT_TOPIC", "kyc-ai-check")
OUTPUT_TOPIC = env("KAFKA_OUTPUT_TOPIC", "kyc-ai-result")
GROUP_ID = env("KAFKA_GROUP_ID", "ai-service-group")
AI_MODE = env("AI_MODE", "ocr").lower()
FACE_MATCH_DEFAULT_SCORE = float(env("FACE_MATCH_DEFAULT_SCORE", "0.98"))
OCR_LANG = env("OCR_LANG", "vi")
HTTP_TIMEOUT_SECONDS = int(env("HTTP_TIMEOUT_SECONDS", "20"))
FACE_MATCH_THRESHOLD = float(env("FACE_MATCH_THRESHOLD", "0.75"))
DEEPFACE_MODEL = env("DEEPFACE_MODEL", "Facenet512")
DEEPFACE_DETECTOR_BACKEND = env("DEEPFACE_DETECTOR_BACKEND", "opencv")


_OCR_ENGINE: PaddleOCR | None = None
_FACE_CASCADE: cv2.CascadeClassifier | None = None
CCCD_LABEL_PATTERNS = (
    "so",
    "so can cuoc",
    "so cccd",
    "so cmnd",
    "identification no",
    "identity card no",
)
NAME_LABEL_PATTERNS = (
    "ho va ten",
    "ho ten",
    "full name",
)
IGNORED_NAME_TOKENS = {
    "cong hoa xa hoi chu nghia viet nam",
    "doc lap tu do hanh phuc",
    "can cuoc cong dan",
    "can cuoc",
    "citizen identity card",
    "the can cuoc",
}


def normalize_text(value: Any) -> str:
    if value is None:
        return ""
    return " ".join(str(value).strip().split()).lower()


def normalize_key_text(value: Any) -> str:
    plain = normalize_text(value)
    ascii_text = unicodedata.normalize("NFD", plain)
    ascii_text = "".join(char for char in ascii_text if unicodedata.category(char) != "Mn")
    ascii_text = re.sub(r"[^a-z0-9 ]+", " ", ascii_text)
    return " ".join(ascii_text.split())


def normalize_name(value: Any) -> str:
    return normalize_key_text(value)


def compact_digits(value: Any) -> str:
    return re.sub(r"\D", "", str(value or ""))


def cleanup_extracted_name(value: str | None) -> str | None:
    if not value:
        return None

    cleaned = re.sub(r"^[^A-Za-zÀ-ỹ]+", "", value).strip(" :.-")
    cleaned = re.sub(r"\s{2,}", " ", cleaned)
    if len(cleaned) < 4:
        return None
    if any(char.isdigit() for char in cleaned):
        return None
    if normalize_key_text(cleaned) in IGNORED_NAME_TOKENS:
        return None
    return cleaned.upper()


def handle_signal(signum: int, _frame: Any) -> None:
    global RUNNING
    logger.info("Received signal %s, stopping consumer loop", signum)
    RUNNING = False


def build_consumer() -> KafkaConsumer:
    return KafkaConsumer(
        INPUT_TOPIC,
        bootstrap_servers=BOOTSTRAP_SERVERS,
        group_id=GROUP_ID,
        auto_offset_reset="earliest",
        enable_auto_commit=True,
        value_deserializer=lambda data: json.loads(data.decode("utf-8")),
        key_deserializer=lambda data: data.decode("utf-8") if data else None,
    )


def build_producer() -> KafkaProducer:
    return KafkaProducer(
        bootstrap_servers=BOOTSTRAP_SERVERS,
        key_serializer=lambda data: str(data).encode("utf-8") if data is not None else None,
        value_serializer=lambda data: json.dumps(data).encode("utf-8"),
    )


def get_ocr_engine() -> PaddleOCR:
    global _OCR_ENGINE
    if _OCR_ENGINE is None:
        logger.info("Initializing PaddleOCR with lang=%s", OCR_LANG)
        _OCR_ENGINE = PaddleOCR(use_angle_cls=True, lang=OCR_LANG, show_log=False)
    return _OCR_ENGINE


def get_face_cascade() -> cv2.CascadeClassifier:
    global _FACE_CASCADE
    if _FACE_CASCADE is None:
        cascade_path = cv2.data.haarcascades + "haarcascade_frontalface_default.xml"
        _FACE_CASCADE = cv2.CascadeClassifier(cascade_path)
    return _FACE_CASCADE


def load_image_from_source(source: str | None) -> np.ndarray | None:
    if not source:
        return None

    parsed = urlparse(source)
    try:
        if parsed.scheme in {"http", "https"}:
            response = requests.get(source, timeout=HTTP_TIMEOUT_SECONDS)
            response.raise_for_status()
            raw = np.frombuffer(response.content, dtype=np.uint8)
            return cv2.imdecode(raw, cv2.IMREAD_COLOR)

        if parsed.scheme == "file":
            local_path = Path(parsed.path)
            if local_path.exists():
                return cv2.imread(str(local_path), cv2.IMREAD_COLOR)

        local_path = Path(source)
        if local_path.exists():
            return cv2.imread(str(local_path), cv2.IMREAD_COLOR)
    except Exception:
        logger.exception("Failed to load image from source=%s", source)
        return None

    logger.warning("Image source is not accessible: %s", source)
    return None


def preprocess_document(image: np.ndarray) -> np.ndarray:
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    denoised = cv2.bilateralFilter(gray, 9, 75, 75)
    return cv2.cvtColor(denoised, cv2.COLOR_GRAY2BGR)


def build_ocr_variants(image: np.ndarray) -> list[np.ndarray]:
    base = preprocess_document(image)
    gray = cv2.cvtColor(base, cv2.COLOR_BGR2GRAY)
    boosted = cv2.convertScaleAbs(gray, alpha=1.35, beta=8)
    adaptive = cv2.adaptiveThreshold(
        boosted,
        255,
        cv2.ADAPTIVE_THRESH_GAUSSIAN_C,
        cv2.THRESH_BINARY,
        31,
        11,
    )
    enlarged = cv2.resize(base, None, fx=1.5, fy=1.5, interpolation=cv2.INTER_CUBIC)
    return [
        image,
        base,
        cv2.cvtColor(boosted, cv2.COLOR_GRAY2BGR),
        cv2.cvtColor(adaptive, cv2.COLOR_GRAY2BGR),
        enlarged,
    ]


def run_ocr(image: np.ndarray | None) -> list[str]:
    if image is None:
        return []

    texts: list[str] = []
    seen: set[str] = set()

    for variant in build_ocr_variants(image):
        result = get_ocr_engine().ocr(variant, cls=True)
        for page in result or []:
            for line in page or []:
                if len(line) < 2:
                    continue
                text_info = line[1]
                if not text_info or len(text_info) < 1:
                    continue
                text = str(text_info[0]).strip()
                normalized = normalize_key_text(text)
                if text and normalized and normalized not in seen:
                    seen.add(normalized)
                    texts.append(text)

    return texts


def extract_id_number(lines: list[str]) -> str | None:
    best_match: str | None = None

    for index, line in enumerate(lines):
        normalized = normalize_key_text(line)
        if any(label in normalized for label in CCCD_LABEL_PATTERNS):
            compact = compact_digits(line)
            if len(compact) == 12:
                return compact
            if index + 1 < len(lines):
                next_digits = compact_digits(lines[index + 1])
                if len(next_digits) == 12:
                    return next_digits

    for line in lines:
        compact = compact_digits(line)
        if len(compact) == 12:
            return compact
        if len(compact) >= 9 and best_match is None:
            best_match = compact

    return best_match


def extract_name(lines: list[str]) -> str | None:
    normalized_lines = [normalize_key_text(line) for line in lines]

    for index, normalized_line in enumerate(normalized_lines):
        if any(label in normalized_line for label in NAME_LABEL_PATTERNS):
            candidates: list[str] = []
            inline_match = re.split(r":", lines[index], maxsplit=1)
            if len(inline_match) == 2:
                candidates.append(inline_match[1])
            if index + 1 < len(lines):
                candidates.append(lines[index + 1])
            for candidate in candidates:
                cleaned = cleanup_extracted_name(candidate)
                if cleaned:
                    return cleaned

    uppercase_candidates: list[str] = []
    for line in lines:
        cleaned = cleanup_extracted_name(line)
        if cleaned:
            uppercase_candidates.append(cleaned)

    return uppercase_candidates[0] if uppercase_candidates else None


def detect_main_face(image: np.ndarray | None) -> np.ndarray | None:
    if image is None:
        return None

    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
    faces = get_face_cascade().detectMultiScale(gray, scaleFactor=1.1, minNeighbors=5, minSize=(80, 80))
    if len(faces) == 0:
        return None

    x, y, w, h = max(faces, key=lambda face: face[2] * face[3])
    return gray[y : y + h, x : x + w]


def compare_faces(card_face: np.ndarray | None, selfie_face: np.ndarray | None) -> float | None:
    if card_face is None or selfie_face is None:
        return None

    if DeepFace is not None:
        try:
            card_bgr = cv2.cvtColor(card_face, cv2.COLOR_GRAY2BGR)
            selfie_bgr = cv2.cvtColor(selfie_face, cv2.COLOR_GRAY2BGR)
            verification = DeepFace.verify(
                img1_path=card_bgr,
                img2_path=selfie_bgr,
                model_name=DEEPFACE_MODEL,
                detector_backend=DEEPFACE_DETECTOR_BACKEND,
                enforce_detection=False,
                silent=True,
            )
            distance = float(verification.get("distance", 1.0))
            threshold = float(verification.get("threshold", 1.0)) or 1.0
            score = max(0.0, min(1.0, 1.0 - (distance / threshold)))
            return 1.0 if verification.get("verified") else score
        except Exception:
            logger.exception("DeepFace verification failed, falling back to histogram similarity")

    resized_card = cv2.resize(card_face, (160, 160))
    resized_selfie = cv2.resize(selfie_face, (160, 160))
    card_hist = cv2.calcHist([resized_card], [0], None, [256], [0, 256])
    selfie_hist = cv2.calcHist([resized_selfie], [0], None, [256], [0, 256])
    cv2.normalize(card_hist, card_hist)
    cv2.normalize(selfie_hist, selfie_hist)
    similarity = cv2.compareHist(card_hist, selfie_hist, cv2.HISTCMP_CORREL)
    return max(0.0, min(1.0, float((similarity + 1.0) / 2.0)))


def analyze_kyc_event(event: dict[str, Any]) -> dict[str, Any]:
    front_image = load_image_from_source(event.get("frontCardUrl"))
    selfie_image = load_image_from_source(event.get("selfieUrl"))

    ocr_lines = run_ocr(front_image)
    logger.info("OCR extracted %s text lines for kycId=%s", len(ocr_lines), event.get("kycId"))

    ocr_id_number = extract_id_number(ocr_lines) or event.get("submittedIdentificationNumber")
    ocr_full_name = extract_name(ocr_lines) or event.get("submittedFullName")

    card_face = detect_main_face(front_image)
    selfie_face = detect_main_face(selfie_image)
    face_match_score = compare_faces(card_face, selfie_face)

    submitted_id = event.get("submittedIdentificationNumber")
    submitted_name = event.get("submittedFullName")
    if normalize_name(submitted_name) == normalize_name(ocr_full_name):
        ocr_full_name = submitted_name

    result = {
        "kycId": event.get("kycId"),
        "userId": event.get("userId"),
        "ocrIdNumber": ocr_id_number,
        "ocrFullName": ocr_full_name,
        "faceMatchScore": face_match_score if face_match_score is not None else FACE_MATCH_DEFAULT_SCORE,
        "potentiallyFake": False,
    }

    id_mismatch = compact_digits(submitted_id) != compact_digits(ocr_id_number)
    name_mismatch = normalize_name(submitted_name) != normalize_name(ocr_full_name)
    low_face_score = face_match_score is None or face_match_score < FACE_MATCH_THRESHOLD
    ocr_failed = not ocr_lines

    if name_mismatch == False:
        result['ocrFullName'] = event.get('submittedFullName')

    result["potentiallyFake"] = bool(id_mismatch or name_mismatch or low_face_score or ocr_failed)
    return result


def process_mock_event(event: dict[str, Any]) -> dict[str, Any]:
    submitted_id = event.get("submittedIdentificationNumber")
    submitted_name = event.get("submittedFullName")
    face_match_score = FACE_MATCH_DEFAULT_SCORE

    missing_required_images = not event.get("frontCardUrl") or not event.get("selfieUrl")

    ocr_id_number = submitted_id
    ocr_full_name = submitted_name
    potentially_fake = missing_required_images

    if AI_MODE != "mock":
        logger.warning("AI_MODE=%s is not implemented yet, using mock pipeline", AI_MODE)

    result = {
        "kycId": event.get("kycId"),
        "userId": event.get("userId"),
        "ocrIdNumber": ocr_id_number,
        "ocrFullName": ocr_full_name,
        "faceMatchScore": face_match_score,
        "potentiallyFake": potentially_fake,
    }

    if normalize_text(submitted_id) != normalize_text(ocr_id_number):
        result["potentiallyFake"] = True
    if normalize_text(submitted_name) != normalize_text(ocr_full_name):
        result["potentiallyFake"] = True

    return result


def validate_event(event: dict[str, Any]) -> None:
    if not event:
        raise ValueError("Empty event payload")
    if event.get("kycId") is None:
        raise ValueError("Missing kycId in event payload")


def main() -> int:
    signal.signal(signal.SIGINT, handle_signal)
    signal.signal(signal.SIGTERM, handle_signal)

    logger.info(
        "Starting AI service with bootstrap=%s, input_topic=%s, output_topic=%s, group_id=%s, mode=%s",
        BOOTSTRAP_SERVERS,
        INPUT_TOPIC,
        OUTPUT_TOPIC,
        GROUP_ID,
        AI_MODE,
    )

    consumer = build_consumer()
    producer = build_producer()

    try:
        while RUNNING:
            messages = consumer.poll(timeout_ms=1000, max_records=10)
            if not messages:
                continue

            for _partition, records in messages.items():
                for record in records:
                    event = record.value
                    logger.info("Received event for kycId=%s", event.get("kycId"))
                    try:
                        validate_event(event)
                        result = analyze_kyc_event(event) if AI_MODE == "ocr" else process_mock_event(event)
                        producer.send(OUTPUT_TOPIC, key=event.get("kycId"), value=result)
                        producer.flush()
                        logger.info("Published result for kycId=%s", result.get("kycId"))
                    except Exception:
                        logger.exception("Failed to process event: %s", event)
    finally:
        consumer.close()
        producer.close()

    logger.info("AI service stopped")
    return 0


if __name__ == "__main__":
    sys.exit(main())
