from typing import Any
from pathlib import Path
import json

from .runtime_config import load_shared_directory


SHARED_DIR = load_shared_directory()
TECH_OUTPUT = SHARED_DIR / "tech_news.json"
POLITICS_OUTPUT = SHARED_DIR / "politics_news.json"
EXCHANGE_RATE_OUTPUT = SHARED_DIR / "exchangeRates.json"
AI_SKILLS_OUTPUT = SHARED_DIR / "ai_skills_today.json"
US_STOCK_INDICES_OUTPUT = SHARED_DIR / "us_stock_indices.json"


def save_json(path: Path, payload: list[dict[str, Any]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as file:
        json.dump(payload, file, ensure_ascii=False, indent=2)
