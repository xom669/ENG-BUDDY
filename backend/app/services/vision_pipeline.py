from __future__ import annotations

import io
from dataclasses import dataclass
from typing import Literal

import cv2
import numpy as np
from pypdf import PdfReader, PdfWriter

DuplicateDecisionType = Literal[
    "UNIQUE",
    "CONFIRMED_DUPLICATE_SEND_LOWER_TO_TRASH",
    "POSSIBLE_DUPLICATE_KEEP_BOTH_FLAGGED",
]


@dataclass(frozen=True)
class VisionMetricsResult:
    laplacian_sharpness: float
    is_blurry: bool
    phash: str
    dhash: str


@dataclass(frozen=True)
class PairDedupResult:
    hamming_phash: int
    hamming_dhash: int
    ssim_score: float
    decision: DuplicateDecisionType
    explanation: str


class OpenCvNotePipeline:
    BLUR_THRESHOLD: float = 85.0
    HASH_HAMMING_THRESHOLD: int = 10
    SSIM_DUPLICATE_THRESHOLD: float = 0.85

    @classmethod
    def compute_metrics(cls, image_bytes: bytes) -> VisionMetricsResult:
        arr = np.frombuffer(image_bytes, dtype=np.uint8)
        gray = cv2.imdecode(arr, cv2.IMREAD_GRAYSCALE)
        if gray is None:
            raise ValueError("Invalid image buffer")

        lap_var = float(cv2.Laplacian(gray, cv2.CV_64F).var())
        phash = cls.compute_phash(gray)
        dhash = cls.compute_dhash(gray)

        return VisionMetricsResult(
            laplacian_sharpness=round(lap_var, 2),
            is_blurry=lap_var < cls.BLUR_THRESHOLD,
            phash=phash,
            dhash=dhash,
        )

    @staticmethod
    def compute_dhash(gray: np.ndarray) -> str:
        resized = cv2.resize(gray, (9, 8), interpolation=cv2.INTER_AREA)
        diff = resized[:, :-1] > resized[:, 1:]
        bits = "".join("1" if b else "0" for b in diff.flatten())
        return f"{int(bits, 2):016X}"

    @staticmethod
    def compute_phash(gray: np.ndarray) -> str:
        resized = np.float32(cv2.resize(gray, (32, 32), interpolation=cv2.INTER_AREA))
        dct = cv2.dct(resized)
        low_freq = dct[:8, :8].flatten()
        ac_mean = float(np.mean(low_freq[1:]))
        bits = "".join("1" if val >= ac_mean else "0" for val in low_freq)
        return f"{int(bits, 2):016X}"

    @staticmethod
    def compute_ssim(gray_a: np.ndarray, gray_b: np.ndarray) -> float:
        a = np.float64(cv2.resize(gray_a, (128, 128), interpolation=cv2.INTER_AREA))
        b = np.float64(cv2.resize(gray_b, (128, 128), interpolation=cv2.INTER_AREA))

        c1 = (0.01 * 255) ** 2
        c2 = (0.03 * 255) ** 2

        mu_a = float(a.mean())
        mu_b = float(b.mean())
        var_a = float(a.var())
        var_b = float(b.var())
        cov_ab = float(np.mean((a - mu_a) * (b - mu_b)))

        numerator = (2 * mu_a * mu_b + c1) * (2 * cov_ab + c2)
        denominator = (mu_a**2 + mu_b**2 + c1) * (var_a + var_b + c2)
        if denominator == 0:
            return 1.0
        return round(float(np.clip(numerator / denominator, 0.0, 1.0)), 3)

    @classmethod
    def evaluate_duplicate_pair(
        cls,
        phash_a: str,
        dhash_a: str,
        phash_b: str,
        dhash_b: str,
        ssim_score: float,
    ) -> PairDedupResult:
        dist_p = bin(int(phash_a, 16) ^ int(phash_b, 16)).count("1")
        dist_d = bin(int(dhash_a, 16) ^ int(dhash_b, 16)).count("1")

        hash_matches = dist_p <= cls.HASH_HAMMING_THRESHOLD or dist_d <= cls.HASH_HAMMING_THRESHOLD
        ssim_matches = ssim_score >= cls.SSIM_DUPLICATE_THRESHOLD

        if hash_matches and ssim_matches:
            return PairDedupResult(
                hamming_phash=dist_p,
                hamming_dhash=dist_d,
                ssim_score=ssim_score,
                decision="CONFIRMED_DUPLICATE_SEND_LOWER_TO_TRASH",
                explanation="pHash and SSIM both confirm duplicate. Lower sharpness sent to 30-day Trash.",
            )
        if hash_matches != ssim_matches:
            return PairDedupResult(
                hamming_phash=dist_p,
                hamming_dhash=dist_d,
                ssim_score=ssim_score,
                decision="POSSIBLE_DUPLICATE_KEEP_BOTH_FLAGGED",
                explanation="pHash and SSIM disagree. Keeping both pages and flagging 'Possible Duplicate'.",
            )
        return PairDedupResult(
            hamming_phash=dist_p,
            hamming_dhash=dist_d,
            ssim_score=ssim_score,
            decision="UNIQUE",
            explanation="Distinct page.",
        )

    @staticmethod
    def stitch_pdfs(pdf_page_buffers: list[bytes]) -> bytes:
        writer = PdfWriter()
        for buf in pdf_page_buffers:
            reader = PdfReader(io.BytesIO(buf))
            for page in reader.pages:
                writer.add_page(page)
        output = io.BytesIO()
        writer.write(output)
        return output.getvalue()
