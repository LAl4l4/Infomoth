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


@patch("main.ThreadPoolExecutor")
@patch("main.FinanceAnalyser")
def test_main_only_analyses_both_news_files(mock_analyser, mock_executor, tmp_path):
    politics_analyser, tech_analyser = MagicMock(), MagicMock()
    mock_analyser.side_effect = [politics_analyser, tech_analyser]
    executor = mock_executor.return_value.__enter__.return_value
    politics_future, tech_future = MagicMock(), MagicMock()
    executor.submit.side_effect = [politics_future, tech_future]
    with (
        patch("main.load_config", return_value={"shared": {"directory": "Shared"}}),
        patch("main.resolve_shared_directory", return_value=tmp_path),
    ):
        analyser_main.main()

    assert executor.submit.call_args_list == [
        call(politics_analyser.analyse, tmp_path / "politics_news.json"),
        call(tech_analyser.analyse, tmp_path / "tech_news.json"),
    ]
    politics_future.result.assert_called_once_with()
    tech_future.result.assert_called_once_with()
    politics_analyser.setupMysql.assert_not_called()
    tech_analyser.setupMysql.assert_not_called()
    politics_analyser.saveAverageToMysql.assert_not_called()
    tech_analyser.saveAverageToMysql.assert_not_called()


@patch("main.ThreadPoolExecutor")
@patch("main.FinanceAnalyser")
def test_main_propagates_analysis_failures(mock_analyser, mock_executor, tmp_path):
    failed_future = MagicMock()
    failed_future.result.side_effect = RuntimeError("analysis failed")
    mock_executor.return_value.__enter__.return_value.submit.side_effect = [
        failed_future,
        MagicMock(),
    ]

    with (
        patch("main.load_config", return_value={"shared": {"directory": "Shared"}}),
        patch("main.resolve_shared_directory", return_value=tmp_path),
        pytest.raises(RuntimeError, match="analysis failed"),
    ):
        analyser_main.main()
