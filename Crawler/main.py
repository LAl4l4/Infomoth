from __future__ import annotations

import json
import logging
from pathlib import Path

from infomoth import PoliticsNewsScraper, TechNewsScraper, ExchangeRateScraper


ROOT_DIR = Path(__file__).resolve().parent
TECH_OUTPUT = ROOT_DIR / "tech_news.json"
POLITICS_OUTPUT = ROOT_DIR / "politics_news.json"
EXCHANGE_RATE_OUTPUT = ROOT_DIR / "exchangeRates.json"


def save_json(path: Path, payload: list[dict[str, str]]) -> None:
    with path.open("w", encoding="utf-8") as file:
        json.dump(payload, file, ensure_ascii=False, indent=2)


def run() -> None:
    logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")

    tech_scraper = TechNewsScraper()
    politics_scraper = PoliticsNewsScraper()
    exchange_rate_scraper = ExchangeRateScraper()

    tech_results = tech_scraper.scrape()
    politics_results = politics_scraper.scrape()
    exchange_rate_results = exchange_rate_scraper.scrape()

    save_json(TECH_OUTPUT, tech_results)
    save_json(POLITICS_OUTPUT, politics_results)
    save_json(EXCHANGE_RATE_OUTPUT, exchange_rate_results)

    logging.info("Saved %s technology stories to %s", len(tech_results), TECH_OUTPUT.name)
    logging.info("Saved %s global politics stories to %s", len(politics_results), POLITICS_OUTPUT.name)
    logging.info("Saved %s exchange rate pairs to %s", len(exchange_rate_results), EXCHANGE_RATE_OUTPUT.name)


if __name__ == "__main__":
    run()
