from __future__ import annotations

import json
import logging
from datetime import datetime, timezone
from pathlib import Path
from typing import Any
from concurrent.futures import ThreadPoolExecutor

from infomoth import (
    PoliticsNewsScraper,
    TechNewsScraper,
    ExchangeRateScraper,
    AISkillsScraper,
    USStockIndexScraper,
)
from infomoth.runtime_config import load_shared_directory


def save_json(path: Path, payload: list[dict[str, Any]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8") as file:
        json.dump(payload, file, ensure_ascii=False, indent=2)


def save_us_stock_indices(path: Path, payload: list[dict[str, Any]]) -> None:
    if not payload and path.exists():
        try:
            existing_payload = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            existing_payload = None

        today = datetime.now(timezone.utc).date().isoformat()
        if (
            isinstance(existing_payload, list)
            and existing_payload
            and all(
                isinstance(item, dict) and item.get("date") == today
                for item in existing_payload
            )
        ):
            logging.info("No US stock indices fetched; keeping same-day data in %s", path.name)
            return

    save_json(path, payload)


def run() -> None:
    logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")
    shared_directory = load_shared_directory()
    outputs = {
        "tech": shared_directory / "tech_news.json",
        "politics": shared_directory / "politics_news.json",
        "exchange": shared_directory / "exchangeRates.json",
        "ai_skills": shared_directory / "ai_skills_today.json",
        "indices": shared_directory / "us_stock_indices.json",
    }

    tech_scraper = TechNewsScraper()
    politics_scraper = PoliticsNewsScraper()
    exchange_rate_scraper = ExchangeRateScraper()
    ai_skills_scraper = AISkillsScraper()
    us_stock_index_scraper = USStockIndexScraper()

    with ThreadPoolExecutor(max_workers=5) as executor:
        tech_future = executor.submit(tech_scraper.scrape)
        politics_future = executor.submit(politics_scraper.scrape)
        exchange_future = executor.submit(exchange_rate_scraper.scrape)
        ai_future = executor.submit(ai_skills_scraper.scrape)
        index_future = executor.submit(us_stock_index_scraper.scrape)

        tech_results = tech_future.result()
        politics_results = politics_future.result()
        exchange_rate_results = exchange_future.result()
        ai_skills_results = ai_future.result()
        us_stock_indices_results = index_future.result()

    save_json(outputs["tech"], tech_results)
    save_json(outputs["politics"], politics_results)
    save_json(outputs["exchange"], exchange_rate_results)
    save_json(outputs["ai_skills"], ai_skills_results)
    save_us_stock_indices(outputs["indices"], us_stock_indices_results)

    logging.info("Saved %s technology stories to %s", len(tech_results), outputs["tech"].name)
    logging.info("Saved %s global politics stories to %s", len(politics_results), outputs["politics"].name)
    logging.info("Saved %s exchange rate pairs to %s", len(exchange_rate_results), outputs["exchange"].name)
    logging.info("Saved %s AI skills to %s", len(ai_skills_results), outputs["ai_skills"].name)
    logging.info("Saved %s US stock indices to %s", len(us_stock_indices_results), outputs["indices"].name)


if __name__ == "__main__":
    run()
