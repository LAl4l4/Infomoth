from __future__ import annotations

import json
import logging
from pathlib import Path
from typing import Any
from concurrent.futures import ThreadPoolExecutor

from infomoth import (
    PoliticsNewsScraper,
    TechNewsScraper,
    ExchangeRateScraper,
    AISkillsScraper,
)


ROOT_DIR = Path(__file__).resolve().parent
SHARED_DIR = ROOT_DIR.parent / "Shared"
TECH_OUTPUT = SHARED_DIR / "tech_news.json"
POLITICS_OUTPUT = SHARED_DIR / "politics_news.json"
EXCHANGE_RATE_OUTPUT = SHARED_DIR / "exchangeRates.json"
AI_SKILLS_OUTPUT = SHARED_DIR / "ai_skills_today.json"


def save_json(path: Path, payload: list[dict[str, Any]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as file:
        json.dump(payload, file, ensure_ascii=False, indent=2)


def run() -> None:
    logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")

    tech_scraper = TechNewsScraper()
    politics_scraper = PoliticsNewsScraper()
    exchange_rate_scraper = ExchangeRateScraper()
    ai_skills_scraper = AISkillsScraper()

    with ThreadPoolExecutor(max_workers=4) as executor:
        tech_future = executor.submit(tech_scraper.scrape)
        politics_future = executor.submit(politics_scraper.scrape)
        exchange_future = executor.submit(exchange_rate_scraper.scrape)
        ai_future = executor.submit(ai_skills_scraper.scrape)

        tech_results = tech_future.result()
        politics_results = politics_future.result()
        exchange_rate_results = exchange_future.result()
        ai_skills_results = ai_future.result()

    save_json(TECH_OUTPUT, tech_results)
    save_json(POLITICS_OUTPUT, politics_results)
    save_json(EXCHANGE_RATE_OUTPUT, exchange_rate_results)
    save_json(AI_SKILLS_OUTPUT, ai_skills_results)

    logging.info("Saved %s technology stories to %s", len(tech_results), TECH_OUTPUT.name)
    logging.info("Saved %s global politics stories to %s", len(politics_results), POLITICS_OUTPUT.name)
    logging.info("Saved %s exchange rate pairs to %s", len(exchange_rate_results), EXCHANGE_RATE_OUTPUT.name)
    logging.info("Saved %s AI skills to %s", len(ai_skills_results), AI_SKILLS_OUTPUT.name)


if __name__ == "__main__":
    run()
