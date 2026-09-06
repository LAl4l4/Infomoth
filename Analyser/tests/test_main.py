from __future__ import annotations

import json
from unittest.mock import MagicMock, call, patch

import pytest

import main as analyser_main


def test_load_config_reads_json_from_config_path(monkeypatch, tmp_path):
    config_path = tmp_path / "app-config.json"
    expected_config = {"shared": {"directory": "Shared"}}
    config_path.write_text(json.dumps(expected_config), encoding="utf-8")
    monkeypatch.setattr(analyser_main, "CONFIG_PATH", config_path)

    assert analyser_main.load_config() == expected_config


def test_resolve_shared_directory_supports_relative_and_absolute_paths(tmp_path):
    assert analyser_main.resolve_shared_directory({"shared": {"directory": "../Shared"}}) == (
        analyser_main.CONFIG_PATH.parent.parent / "Shared"
    )
    assert analyser_main.resolve_shared_directory({"shared": {"directory": str(tmp_path)}}) == tmp_path


@patch("main.FinanceAnalyser")
def test_main_reuses_one_model_for_both_news_files(mock_analyser, tmp_path):
    with patch("main.resolve_shared_directory", return_value=tmp_path):
        analyser_main.main()
    mock_analyser.assert_called_once_with()
    assert mock_analyser.return_value.analyse.call_args_list == [
        call(tmp_path / "politics_news.json"), call(tmp_path / "tech_news.json")]


@patch("main.FinanceAnalyser")
def test_main_attempts_other_source_before_propagating_failure(mock_analyser, tmp_path):
    mock_analyser.return_value.analyse.side_effect = [RuntimeError("broken source"), None]
    with patch("main.resolve_shared_directory", return_value=tmp_path), pytest.raises(RuntimeError):
        analyser_main.main()
    assert mock_analyser.return_value.analyse.call_count == 2
