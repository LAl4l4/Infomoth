from __future__ import annotations

import json
from unittest.mock import call, patch

import pytest

from analyser.finance import FinanceAnalyser


class TestFinanceAnalyser:
    @patch("analyser.finance.pipeline")
    def test_initialises_finbert_pipeline(self, mock_pipeline):
        analyser = FinanceAnalyser()

        assert analyser.pipe is mock_pipeline.return_value
        mock_pipeline.assert_called_once_with(
            "sentiment-analysis",
            model="ProsusAI/finbert",
            local_files_only=True,
        )

    @patch("analyser.finance.pipeline")
    def test_analyse_appends_scores(self, mock_pipeline, tmp_path):
        news_path = tmp_path / "news.json"
        news_path.write_text(
            json.dumps([{"title": "good news", "url": "one"}, {"title": "bad news"}]),
            encoding="utf-8",
        )
        mock_pipeline.return_value.side_effect = [
            [
                {"label": "positive", "score": 0.8},
                {"label": "neutral", "score": 0.1},
                {"label": "negative", "score": 0.1},
            ],
            [
                {"label": "positive", "score": 0.2},
                {"label": "neutral", "score": 0.3},
                {"label": "negative", "score": 0.5},
            ],
        ]

        analyser = FinanceAnalyser()
        analyser.analyse(news_path)

        saved_news = json.loads(news_path.read_text(encoding="utf-8"))
        assert saved_news == [
            {
                "title": "good news",
                "url": "one",
                "financeInfluence": {
                    "positive": 0.8,
                    "neutral": 0.1,
                    "negative": 0.1,
                    "sentiment_score": pytest.approx(0.7),
                },
            },
            {
                "title": "bad news",
                "financeInfluence": {
                    "positive": 0.2,
                    "neutral": 0.3,
                    "negative": 0.5,
                    "sentiment_score": pytest.approx(-0.3),
                },
            },
        ]
        assert mock_pipeline.return_value.call_args_list == [
            call("good news", top_k=3),
            call("bad news", top_k=3),
        ]

    @patch("analyser.finance.pipeline")
    def test_analyse_skips_items_without_a_valid_title(self, mock_pipeline, tmp_path):
        news_path = tmp_path / "news.json"
        news_path.write_text(
            json.dumps([{"url": "missing"}, {"title": " "}, {"title": 123}]),
            encoding="utf-8",
        )

        analyser = FinanceAnalyser()
        analyser.analyse(news_path)

        assert json.loads(news_path.read_text(encoding="utf-8")) == []
        mock_pipeline.return_value.assert_not_called()

    def test_has_no_mysql_persistence_api(self):
        assert not hasattr(FinanceAnalyser, "setupMysql")
        assert not hasattr(FinanceAnalyser, "saveAverageToMysql")
        assert not hasattr(FinanceAnalyser, "closeMysql")
