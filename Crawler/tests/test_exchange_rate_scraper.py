from __future__ import annotations

from unittest.mock import MagicMock, patch

import pytest

from infomoth.exchange_rate_scraper import ExchangeRateScraper, MAJOR_CURRENCIES, CURRENCY_NAMES


@pytest.fixture
def scraper():
    return ExchangeRateScraper()


class TestFetchBaseRates:
    @patch("infomoth.exchange_rate_scraper.requests.get")
    def test_returns_data_on_success(self, mock_get, scraper):
        mock_resp = MagicMock()
        mock_resp.json.return_value = {"date": "2025-01-01", "rates": {"CNY": 7.2}}
        mock_get.return_value = mock_resp
        base, data = scraper._fetch_base_rates("USD")
        assert base == "USD"
        assert data["rates"]["CNY"] == 7.2

    @patch("infomoth.exchange_rate_scraper.requests.get")
    def test_returns_none_on_timeout(self, mock_get, scraper):
        from requests.exceptions import Timeout
        mock_get.side_effect = Timeout()
        base, data = scraper._fetch_base_rates("USD")
        assert base == "USD"
        assert data is None

    @patch("infomoth.exchange_rate_scraper.requests.get")
    def test_returns_none_on_request_error(self, mock_get, scraper):
        from requests.exceptions import RequestException
        mock_get.side_effect = RequestException("fail")
        base, data = scraper._fetch_base_rates("EUR")
        assert data is None

    @patch("infomoth.exchange_rate_scraper.requests.get")
    def test_returns_none_on_invalid_json(self, mock_get, scraper):
        mock_resp = MagicMock()
        mock_resp.json.return_value = [1, 2, 3]
        mock_get.return_value = mock_resp
        _, data = scraper._fetch_base_rates("USD")
        assert data is None


class TestScrape:
    @patch.object(ExchangeRateScraper, "_fetch_base_rates")
    def test_deduplicates_pairs(self, mock_fetch, scraper):
        mock_fetch.return_value = ("USD", {"date": "2025-01-01", "rates": {"CNY": 7.2, "EUR": 0.9}})
        results = scraper.scrape()
        pairs = [f"{r['base_currency']}/{r['quote_currency']}" for r in results]
        assert len(pairs) == len(set(pairs))

    @patch.object(ExchangeRateScraper, "_fetch_base_rates")
    def test_includes_currency_names(self, mock_fetch, scraper):
        mock_fetch.return_value = ("USD", {"date": "2025-01-01", "rates": {"CNY": 7.2}})
        results = scraper.scrape()
        usd_cny = next(r for r in results if r["quote_currency"] == "CNY")
        assert usd_cny["base_currency_name"] == "US Dollar"
        assert usd_cny["quote_currency_name"] == "Chinese Yuan"

    @patch.object(ExchangeRateScraper, "_fetch_base_rates")
    def test_skips_failed_bases(self, mock_fetch, scraper):
        mock_fetch.return_value = ("USD", None)
        results = scraper.scrape()
        assert results == []

    @patch.object(ExchangeRateScraper, "_fetch_base_rates")
    def test_rate_is_string(self, mock_fetch, scraper):
        mock_fetch.return_value = ("USD", {"date": "2025-01-01", "rates": {"EUR": 0.92}})
        results = scraper.scrape()
        assert all(isinstance(r["rate"], str) for r in results)

    @patch.object(ExchangeRateScraper, "_fetch_base_rates")
    def test_source_field(self, mock_fetch, scraper):
        mock_fetch.return_value = ("USD", {"date": "2025-01-01", "rates": {"EUR": 0.92}})
        results = scraper.scrape()
        assert results[0]["source"] == "Frankfurter (ECB)"
