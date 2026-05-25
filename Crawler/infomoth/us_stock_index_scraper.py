from __future__ import annotations

import logging
from datetime import datetime, timezone
from typing import Dict, List, Optional
from unitTestHelper import save_json, US_STOCK_INDICES_OUTPUT

import yfinance as yf

LOGGER = logging.getLogger(__name__)

_INDEX_SYMBOLS: Dict[str, str] = {
    "^GSPC": "S&P 500",
    "^DJI": "Dow Jones Industrial Average",
    "^IXIC": "NASDAQ Composite",
    "^RUT": "Russell 2000",
}

class USStockIndexScraper:
    """Scrape major US stock index snapshots from Yahoo Finance."""

    def scrape(self) -> List[Dict[str, object]]:
        symbols = list(_INDEX_SYMBOLS.keys())
        try:
            data = yf.download(
                tickers=symbols,
                period="5d",
                interval="1d",
                progress=False,
                auto_adjust=False,
                group_by="ticker",
                threads=False,
            )
        except Exception as exc:
            LOGGER.warning("Failed to fetch US stock indices from Yahoo Finance: %s", exc)
            return []

        if data is None or data.empty:
            LOGGER.warning("Yahoo Finance returned empty US stock index data")
            return []

        today = datetime.now(timezone.utc).strftime("%Y-%m-%d")
        results: List[Dict[str, object]] = []
        for symbol in symbols:
            closes = self._extract_close_series(data, symbol)
            if closes is None or len(closes) < 2:
                LOGGER.warning("Insufficient close data for index %s", symbol)
                continue

            latest_close = float(closes.iloc[-1])
            previous_close = float(closes.iloc[-2])
            change = latest_close - previous_close
            change_percent = (change / previous_close * 100.0) if previous_close else 0.0

            results.append(
                {
                    "symbol": symbol,
                    "name": _INDEX_SYMBOLS[symbol],
                    "price": round(latest_close, 4),
                    "change": round(change, 4),
                    "change_percent": round(change_percent, 4),
                    "date": today,
                    "source": "Yahoo Finance",
                }
            )

        LOGGER.info("Fetched %s US stock indices", len(results))
        return results

    def _extract_close_series(self, data, symbol: str) -> Optional[object]:
        if hasattr(data, "columns") and hasattr(data.columns, "nlevels") and data.columns.nlevels > 1:
            if symbol not in data.columns.get_level_values(0):
                return None
            close_series = data[symbol].get("Close")
        else:
            close_series = data.get("Close")

        if close_series is None:
            return None
        close_series = close_series.dropna()
        return close_series if len(close_series) >= 2 else None
    

        
if __name__ == "__main__":
    logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")
    scraper = USStockIndexScraper()
    indices = scraper.scrape()
    save_json(US_STOCK_INDICES_OUTPUT, indices)
    logging.info("Saved %s US stock indices to %s", len(indices), US_STOCK_INDICES_OUTPUT.name)
