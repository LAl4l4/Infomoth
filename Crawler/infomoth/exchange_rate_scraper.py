from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor, as_completed
import logging
from datetime import datetime, timezone
from typing import Dict, List, Optional, Tuple

import requests
from requests.exceptions import RequestException, Timeout

LOGGER = logging.getLogger(__name__)

# Major currencies to track (must include USD/CNY/AUD)
MAJOR_CURRENCIES = ["USD", "CNY", "AUD", "EUR", "GBP", "JPY", "CAD", "CHF", "HKD", "SGD", "KRW"]

# Base currencies for which we fetch full rate tables
BASE_CURRENCIES = ["USD", "CNY", "AUD", "EUR", "GBP", "JPY", "CAD", "CHF", "HKD", "SGD", "KRW"]

CURRENCY_NAMES: Dict[str, str] = {
    "USD": "US Dollar",
    "CNY": "Chinese Yuan",
    "AUD": "Australian Dollar",
    "EUR": "Euro",
    "GBP": "British Pound",
    "JPY": "Japanese Yen",
    "CAD": "Canadian Dollar",
    "CHF": "Swiss Franc",
    "HKD": "Hong Kong Dollar",
    "SGD": "Singapore Dollar",
    "KRW": "South Korean Won",
}

_TIMEOUT = 15
_MAX_CONCURRENT_REQUESTS = 3
_API_BASE = "https://api.frankfurter.dev/v1"
_HEADERS = {
    "User-Agent": "Mozilla/5.0 (compatible; InfoMoth/1.0)",
    "Accept": "application/json",
}


class ExchangeRateScraper:
    """Scrapes current exchange rates for major world currencies."""

    def scrape(self) -> List[Dict[str, str]]:
        results: List[Dict[str, str]] = []
        seen: set[str] = set()

        with ThreadPoolExecutor(max_workers=_MAX_CONCURRENT_REQUESTS) as executor:
            futures = [executor.submit(self._fetch_base_rates, base) for base in BASE_CURRENCIES]
            for future in as_completed(futures):
                base, data = future.result()
                if data is None:
                    continue

                fetched_date = data.get("date", datetime.now(timezone.utc).strftime("%Y-%m-%d"))
                rates: Dict[str, float] = data.get("rates", {})

                for quote, rate in rates.items():
                    pair_key = f"{base}/{quote}"
                    if pair_key in seen:
                        continue
                    seen.add(pair_key)
                    results.append(
                        {
                            "base_currency": base,
                            "base_currency_name": CURRENCY_NAMES.get(base, base),
                            "quote_currency": quote,
                            "quote_currency_name": CURRENCY_NAMES.get(quote, quote),
                            "rate": str(rate),
                            "date": fetched_date,
                            "source": "Frankfurter (ECB)",
                        }
                    )

        LOGGER.info("Fetched %s exchange rate pairs", len(results))
        return results

    def _fetch_base_rates(self, base: str) -> Tuple[str, Optional[Dict[str, object]]]:
        targets = [c for c in MAJOR_CURRENCIES if c != base]
        url = f"{_API_BASE}/latest"
        try:
            response = requests.get(
                url,
                params={"base": base, "symbols": ",".join(targets)},
                headers=_HEADERS,
                timeout=_TIMEOUT,
            )
            response.raise_for_status()
            data = response.json()
        except Timeout:
            LOGGER.warning("Timeout fetching exchange rates for base %s", base)
            return base, None
        except RequestException as exc:
            LOGGER.warning("Failed to fetch exchange rates for base %s: %s", base, exc)
            return base, None

        if not isinstance(data, dict):
            LOGGER.warning("Invalid JSON payload for base %s", base)
            return base, None

        return base, data
    
    
if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")
    scraper = ExchangeRateScraper()
    exchange_rates = scraper.scrape()
    for rate in exchange_rates:
        print(rate)
