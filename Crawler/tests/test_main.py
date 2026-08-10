from __future__ import annotations

import json
from pathlib import Path
from unittest.mock import patch, MagicMock

import pytest

from main import save_exchange_rates, save_json, save_us_stock_indices


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
