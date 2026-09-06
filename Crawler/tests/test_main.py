from __future__ import annotations

import json
from pathlib import Path
from unittest.mock import patch, MagicMock

import pytest

from main import run_stocks, save_exchange_rates, save_json, save_us_stock_indices


class TestSaveJson:
    def test_creates_file_and_directories(self, tmp_path):
        target = tmp_path / "sub" / "out.json"
        payload = [{"key": "value"}]
        save_json(target, payload)
        assert target.exists()
        assert json.loads(target.read_text(encoding="utf-8")) == payload

    def test_overwrites_existing_file(self, tmp_path):
        target = tmp_path / "out.json"
        save_json(target, [{"a": 1}])
        save_json(target, [{"b": 2}])
        assert json.loads(target.read_text(encoding="utf-8")) == [{"b": 2}]

    def test_handles_empty_list(self, tmp_path):
        target = tmp_path / "empty.json"
        save_json(target, [])
        assert json.loads(target.read_text(encoding="utf-8")) == []

    def test_preserves_unicode(self, tmp_path):
        target = tmp_path / "uni.json"
        save_json(target, [{"title": "中文标题"}])
        content = target.read_text(encoding="utf-8")
        assert "中文标题" in content


class TestSaveUsStockIndices:
    def test_keeps_latest_trading_day_data_when_new_payload_is_empty(self, tmp_path):
        target = tmp_path / "us_stock_indices.json"
        existing = [{"symbol": "^GSPC", "date": "2026-08-07"}]
        target.write_text(json.dumps(existing), encoding="utf-8")

        save_us_stock_indices(target, [])

        assert json.loads(target.read_text(encoding="utf-8")) == existing

    def test_writes_empty_payload_when_existing_data_is_invalid(self, tmp_path):
        target = tmp_path / "us_stock_indices.json"
        target.write_text("not json", encoding="utf-8")

        save_us_stock_indices(target, [])

        assert json.loads(target.read_text(encoding="utf-8")) == []


class TestSaveExchangeRates:
    def test_keeps_more_complete_existing_payload(self, tmp_path):
        target = tmp_path / "exchangeRates.json"
        existing = [{"base_currency": "USD"}, {"base_currency": "CNY"}]
        target.write_text(json.dumps(existing), encoding="utf-8")

        save_exchange_rates(target, [{"base_currency": "USD"}])

        assert json.loads(target.read_text(encoding="utf-8")) == existing

    def test_replaces_existing_payload_when_current_payload_is_more_complete(self, tmp_path):
        target = tmp_path / "exchangeRates.json"
        target.write_text(json.dumps([{"base_currency": "USD"}]), encoding="utf-8")
        current = [{"base_currency": "USD"}, {"base_currency": "CNY"}]

        save_exchange_rates(target, current)

        assert json.loads(target.read_text(encoding="utf-8")) == current


class TestRun:
    @patch("main.load_shared_directory")
    @patch("main.save_json")
    @patch("main.USStockIndexScraper")
    @patch("main.AISkillsScraper")
    @patch("main.ExchangeRateScraper")
    @patch("main.PoliticsNewsScraper")
    @patch("main.TechNewsScraper")
    def test_run_saves_all_outputs(
        self,
        mock_tech_cls,
        mock_pol_cls,
        mock_ex_cls,
        mock_ai_cls,
        mock_stock_cls,
        mock_save,
        mock_shared_directory,
        tmp_path,
    ):
        mock_shared_directory.return_value = tmp_path
        for cls_mock in (mock_tech_cls, mock_pol_cls, mock_ex_cls, mock_ai_cls, mock_stock_cls):
            instance = cls_mock.return_value
            instance.scrape.return_value = [{"data": "test"}]

        from main import run
        run()

        assert mock_save.call_count == 5

    @patch("main.USStockIndexScraper")
    @patch("main.load_shared_directory")
    def test_run_stocks_only_updates_just_the_stock_output(
        self,
        mock_shared_directory,
        mock_stock_cls,
        tmp_path,
    ):
        mock_shared_directory.return_value = tmp_path
        payload = [{"symbol": "^GSPC", "date": "2026-08-07"}]
        mock_stock_cls.return_value.scrape.return_value = payload

        run_stocks()

        output = tmp_path / "us_stock_indices.json"
        assert json.loads(output.read_text(encoding="utf-8")) == payload


def test_partial_crawl_saves_successes_and_keeps_failed_source(tmp_path):
    from main import run
    old = [{"title": "previous"}]
    (tmp_path / "tech_news.json").write_text(json.dumps(old))
    with patch("main.load_shared_directory", return_value=tmp_path), \
         patch("main.TechNewsScraper.scrape", side_effect=RuntimeError("offline")), \
         patch("main.PoliticsNewsScraper.scrape", return_value=[{"title": "new"}]), \
         patch("main.ExchangeRateScraper.scrape", return_value=[]), \
         patch("main.AISkillsScraper.scrape", return_value=[]), \
         patch("main.USStockIndexScraper.scrape", return_value=[]):
        run()
    assert json.loads((tmp_path / "tech_news.json").read_text()) == old
    assert json.loads((tmp_path / "politics_news.json").read_text()) == [{"title": "new"}]


def test_partial_rates_update_available_pairs_without_redating_missing_pairs(tmp_path):
    target = tmp_path / "exchangeRates.json"
    old = [
        {"base_currency": "USD", "quote_currency": "CNY", "rate": "7", "date": "2026-09-01"},
        {"base_currency": "CNY", "quote_currency": "USD", "rate": ".14", "date": "2026-09-01"}]
    save_json(target, old)
    fresh = {**old[0], "rate": "7.1", "date": "2026-09-02"}
    save_exchange_rates(target, [fresh])
    assert json.loads(target.read_text()) == [fresh, old[1]]
    save_exchange_rates(target, [old[0]])
    assert json.loads(target.read_text()) == [fresh, old[1]]


def test_failed_json_serialization_does_not_truncate_previous_snapshot(tmp_path):
    target = tmp_path / "data.json"
    save_json(target, [{"value": 1}])
    with pytest.raises(TypeError):
        save_json(target, [{"value": object()}])
    assert json.loads(target.read_text()) == [{"value": 1}]
    assert list(tmp_path.iterdir()) == [target]
