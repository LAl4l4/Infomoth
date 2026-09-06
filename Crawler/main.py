from __future__ import annotations

import json
import logging
import sys
import os
import tempfile
from pathlib import Path
from typing import Any
from concurrent.futures import ThreadPoolExecutor, as_completed

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
    temporary = None
    try:
        with tempfile.NamedTemporaryFile(mode="w", encoding="utf-8", dir=path.parent, delete=False) as file:
            temporary = Path(file.name)
            json.dump(payload, file, ensure_ascii=False, indent=2)
        temporary.chmod(path.stat().st_mode & 0o777 if path.exists() else 0o644)
        os.replace(temporary, path)
    finally:
        if temporary is not None:
            temporary.unlink(missing_ok=True)


def save_us_stock_indices(path: Path, payload: list[dict[str, Any]]) -> None:
    if not payload and path.exists():
        try:
            existing_payload = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            existing_payload = None

        if isinstance(existing_payload, list) and existing_payload:
            logging.info("No US stock indices fetched; keeping latest trading-day data in %s", path.name)
            return

    save_json(path, payload)


def save_exchange_rates(path: Path, payload: list[dict[str, Any]]) -> None:
    if path.exists():
        try:
            existing_payload = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            existing_payload = None

        if isinstance(existing_payload, list) and existing_payload:
            # Update only captured directions; retain missing pairs with their original dates.
            def key(item):
                return (item.get("base_currency", item.get("base")),
                        item.get("quote_currency", item.get("quote")))

            merged = {key(item): item for item in existing_payload}
            for item in payload:
                previous = merged.get(key(item))
                if previous is None or item.get("date", "") >= previous.get("date", ""):
                    merged[key(item)] = item
            payload = list(merged.values())

    save_json(path, payload)


def run_stocks() -> None:
    logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")
    output = load_shared_directory() / "us_stock_indices.json"
    results = USStockIndexScraper().scrape()
    save_us_stock_indices(output, results)
    logging.info("Saved %s US stock indices to %s", len(results), output.name)


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

    jobs = {
        "tech": (tech_scraper, save_json),
        "politics": (politics_scraper, save_json),
        "exchange": (exchange_rate_scraper, save_exchange_rates),
        "ai_skills": (ai_skills_scraper, save_json),
        "indices": (us_stock_index_scraper, save_us_stock_indices),
    }
    failures = []
    with ThreadPoolExecutor(max_workers=5) as executor:
        futures = {executor.submit(scraper.scrape): name for name, (scraper, _) in jobs.items()}
        for future in as_completed(futures):
            name = futures[future]
            try:
                payload = future.result()
                if not payload:
                    raise ValueError("No records fetched")
                jobs[name][1](outputs[name], payload)
                logging.info("Updated %s: %s records", name, len(payload))
            except Exception:
                failures.append(name)
                logging.exception("Cannot update %s; retaining previous snapshot", name)
    if failures:
        logging.warning("Partial crawler cycle; failed sources: %s", ", ".join(sorted(failures)))
    if len(failures) == len(jobs):
        raise RuntimeError("All crawler sources failed")


if __name__ == "__main__":
    if sys.argv[1:] == ["--stocks-only"]:
        run_stocks()
    else:
        run()
