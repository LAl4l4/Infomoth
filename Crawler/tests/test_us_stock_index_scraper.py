from __future__ import annotations

from unittest.mock import MagicMock, patch

import pandas as pd
import pytest

from infomoth.us_stock_index_scraper import USStockIndexScraper, _INDEX_SYMBOLS


@pytest.fixture
def scraper():
    return USStockIndexScraper()


def _make_multi_index_data(closes: dict[str, list[float]]) -> pd.DataFrame:
    max_len = max(len(v) for v in closes.values())
    arrays = {}
    for symbol, values in closes.items():
        padded = values + [float("nan")] * (max_len - len(values))
        arrays[(symbol, "Close")] = padded
    columns = pd.MultiIndex.from_tuples(arrays.keys())
    return pd.DataFrame(arrays, columns=columns)


class TestExtractCloseSeries:
    def test_extracts_from_multi_index(self, scraper):
        data = _make_multi_index_data({"^GSPC": [100.0, 101.0, 102.0]})
        series = scraper._extract_close_series(data, "^GSPC")
        assert series is not None
        assert len(series) == 3

    def test_returns_none_for_missing_symbol(self, scraper):
        data = _make_multi_index_data({"^GSPC": [100.0, 101.0]})
        assert scraper._extract_close_series(data, "^DJI") is None

    def test_returns_none_for_insufficient_data(self, scraper):
        data = _make_multi_index_data({"^GSPC": [100.0]})
        assert scraper._extract_close_series(data, "^GSPC") is None

    def test_drops_nan_values(self, scraper):
        data = _make_multi_index_data({"^GSPC": [100.0, float("nan"), 102.0]})
        series = scraper._extract_close_series(data, "^GSPC")
        assert len(series) == 2


class TestScrape:
    @patch("infomoth.us_stock_index_scraper.yf.download")
    def test_computes_change_and_percent(self, mock_download, scraper):
        mock_download.return_value = _make_multi_index_data({
            "^GSPC": [5000.0, 5100.0],
            "^DJI": [38000.0, 38200.0],
            "^IXIC": [16000.0, 16100.0],
            "^RUT": [2000.0, 2010.0],
        })
        results = scraper.scrape()
        sp500 = next(r for r in results if r["symbol"] == "^GSPC")
        assert sp500["price"] == 5100.0
        assert sp500["change"] == 100.0
        assert abs(sp500["changePercent"] - 2.0) < 0.001
        assert sp500["name"] == "S&P 500"

    @patch("infomoth.us_stock_index_scraper.yf.download")
    def test_returns_empty_on_download_failure(self, mock_download, scraper):
        mock_download.side_effect = Exception("network error")
        assert scraper.scrape() == []

    @patch("infomoth.us_stock_index_scraper.yf.download")
    def test_returns_empty_on_empty_data(self, mock_download, scraper):
        mock_download.return_value = pd.DataFrame()
        assert scraper.scrape() == []

    @patch("infomoth.us_stock_index_scraper.yf.download")
    def test_skips_indices_with_insufficient_data(self, mock_download, scraper):
        mock_download.return_value = _make_multi_index_data({
            "^GSPC": [5000.0, 5100.0],
            "^DJI": [38000.0],
            "^IXIC": [16000.0, 16100.0],
            "^RUT": [2000.0, 2010.0],
        })
        results = scraper.scrape()
        symbols = [r["symbol"] for r in results]
        assert "^DJI" not in symbols
        assert "^GSPC" in symbols

    @patch("infomoth.us_stock_index_scraper.yf.download")
    def test_output_schema(self, mock_download, scraper):
        mock_download.return_value = _make_multi_index_data({
            "^GSPC": [5000.0, 5100.0],
            "^DJI": [38000.0, 38200.0],
            "^IXIC": [16000.0, 16100.0],
            "^RUT": [2000.0, 2010.0],
        })
        results = scraper.scrape()
        entry = results[0]
        assert set(entry.keys()) == {"symbol", "name", "price", "change", "changePercent", "date", "source"}
        assert entry["source"] == "Yahoo Finance"
