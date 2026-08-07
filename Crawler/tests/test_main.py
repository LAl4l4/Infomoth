from __future__ import annotations

import json
from datetime import datetime, timezone
from pathlib import Path
from unittest.mock import patch, MagicMock

import pytest

from main import save_json, save_us_stock_indices


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
    def test_keeps_same_day_data_when_new_payload_is_empty(self, tmp_path):
        target = tmp_path / "us_stock_indices.json"
        existing = [{"symbol": "^GSPC", "date": datetime.now(timezone.utc).date().isoformat()}]
        target.write_text(json.dumps(existing), encoding="utf-8")

        save_us_stock_indices(target, [])

        assert json.loads(target.read_text(encoding="utf-8")) == existing

    def test_overwrites_previous_day_data_when_new_payload_is_empty(self, tmp_path):
        target = tmp_path / "us_stock_indices.json"
        target.write_text(json.dumps([{"symbol": "^GSPC", "date": "2020-01-01"}]), encoding="utf-8")

        save_us_stock_indices(target, [])

        assert json.loads(target.read_text(encoding="utf-8")) == []


class TestRun:
    @patch("main.save_json")
    @patch("main.USStockIndexScraper")
    @patch("main.AISkillsScraper")
    @patch("main.ExchangeRateScraper")
    @patch("main.PoliticsNewsScraper")
    @patch("main.TechNewsScraper")
    def test_run_saves_all_outputs(self, mock_tech_cls, mock_pol_cls, mock_ex_cls, mock_ai_cls, mock_stock_cls, mock_save):
        for cls_mock in (mock_tech_cls, mock_pol_cls, mock_ex_cls, mock_ai_cls, mock_stock_cls):
            instance = cls_mock.return_value
            instance.scrape.return_value = [{"data": "test"}]

        from main import run
        run()

        assert mock_save.call_count == 5
