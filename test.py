import unittest
from unittest.mock import patch
import numpy as np
from app import main
class AiServiceTests(unittest.TestCase):
    def test_extracts_cccd_number_from_vietnamese_label(self) -> None:
        lines = [
            "CĂN CƯỚC CÔNG DÂN",
            "Số: 079123456789",
            "Họ và tên: NGUYỄN VĂN A",
        ]
        self.assertEqual(main.extract_id_number(lines), "079123456789")
    def test_extracts_name_from_vietnamese_label(self) -> None:
        lines = [
            "CĂN CƯỚC CÔNG DÂN",
            "Họ và tên",
            "Nguyễn Văn A",
        ]
        self.assertEqual(main.extract_name(lines), "NGUYỄN VĂN A")
    @patch("app.main.compare_faces", return_value=0.91)
    @patch("app.main.detect_main_face", return_value=np.zeros((160, 160), dtype=np.uint8))
    @patch("app.main.load_image_from_source", return_value=np.zeros((300, 500, 3), dtype=np.uint8))
    @patch(
        "app.main.run_ocr",
        return_value=[
            "CĂN CƯỚC CÔNG DÂN",
            "Số: 079123456789",
            "Họ và tên: Nguyen Van A",
        ],
    )
    def test_analyze_event_accepts_matching_cccd_data(
        self,
        _run_ocr,
        _load_image,
        _detect_face,
        _compare_faces,
    ) -> None:
        event = {
            "kycId": 1,
            "userId": 7,
            "frontCardUrl": "front.jpg",
            "selfieUrl": "selfie.jpg",
            "submittedIdentificationNumber": "079123456789",
            "submittedFullName": "Nguyễn Văn A",
        }
        result = main.analyze_kyc_event(event)
        self.assertEqual(result["ocrIdNumber"], "079123456789")
        self.assertEqual(result["ocrFullName"], "Nguyễn Văn A")
        self.assertAlmostEqual(result["faceMatchScore"], 0.91)
        self.assertFalse(result["potentiallyFake"])
    @patch("app.main.compare_faces", return_value=0.42)
    @patch("app.main.detect_main_face", return_value=np.zeros((160, 160), dtype=np.uint8))
    @patch("app.main.load_image_from_source", return_value=np.zeros((300, 500, 3), dtype=np.uint8))
    @patch(
        "app.main.run_ocr",
        return_value=[
            "CĂN CƯỚC CÔNG DÂN",
            "Số: 079123456780",
            "Họ và tên: TRẦN VĂN B",
        ],
    )
    def test_analyze_event_flags_fake_when_fields_mismatch(
        self,
        _run_ocr,
        _load_image,
        _detect_face,
        _compare_faces,
    ) -> None:
        event = {
            "kycId": 2,
            "userId": 8,
            "frontCardUrl": "front.jpg",
            "selfieUrl": "selfie.jpg",
            "submittedIdentificationNumber": "079123456789",
            "submittedFullName": "Nguyễn Văn A",
        }
        result = main.analyze_kyc_event(event)
        self.assertTrue(result["potentiallyFake"])
if __name__ == "__main__":
    unittest.main()