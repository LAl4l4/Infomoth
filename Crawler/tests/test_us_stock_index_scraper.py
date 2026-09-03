from __future__ import annotations

from unittest.mock import MagicMock, patch

import pandas as pd
import pytest

from infomoth.us_stock_index_scraper import USStockIndexScraper, _INDEX_SYMBOLS


@pytest.fixture
def scraper():
    return USStockIndexScraper()


def _make_multi_index_data(
    closes: dict[str, list[float]],
    start_date: str = "2026-08-06",
) -> pd.DataFrame:
    max_len = max(len(v) for v in closes.values())
    arrays = {}
    for symbol, values in closes.items():
        padded = values + [float("nan")] * (max_len - len(values))
        arrays[(symbol, "Close")] = padded
    columns = pd.MultiIndex.from_tuples(arrays.keys())
    index = pd.date_range(start_date, periods=max_len, freq="D")
    return pd.DataFrame(arrays, columns=columns, index=index)


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

    def test_collapses_intraday_bars_to_each_trading_days_latest_price(self, scraper):
        index = pd.to_datetime([
            "2026-08-06 15:55:00",
            "2026-08-06 16:00:00",
            "2026-08-07 09:35:00",
            "2026-08-07 09:40:00",
        ])
        closes = pd.Series([4990.0, 5000.0, 5010.0, 5020.0], index=index)

        daily_closes = scraper._collapse_to_daily_closes(closes)

        assert daily_closes.tolist() == [5000.0, 5020.0]


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
        assert sp500["date"] == "2026-08-07"
        assert sp500["name"] == "S&P 500"
        assert mock_download.call_args.kwargs["interval"] == "5m"
        assert mock_download.call_args.kwargs["prepost"] is False

    @patch("infomoth.us_stock_index_scraper.yf.download")
    def test_uses_previous_trading_day_close_for_intraday_change(self, mock_download, scraper):
        index = pd.to_datetime([
            "2026-08-06 15:55:00",
            "2026-08-06 16:00:00",
            "2026-08-07 09:35:00",
            "2026-08-07 09:40:00",
        ])
        values = {
            (symbol, "Close"): [4990.0, 5000.0, 5010.0, 5020.0]
            for symbol in _INDEX_SYMBOLS
        }
        mock_download.return_value = pd.DataFrame(values, index=index)

        results = scraper.scrape()

        sp500 = next(r for r in results if r["symbol"] == "^GSPC")
        assert sp500["price"] == 5020.0
        assert sp500["change"] == 20.0
        assert sp500["changePercent"] == 0.4
        assert sp500["date"] == "2026-08-07"

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
