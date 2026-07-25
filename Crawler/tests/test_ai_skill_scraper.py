from __future__ import annotations

from unittest.mock import MagicMock, patch

import pytest

from infomoth.ai_skill_scraper import AISkillsScraper, SKILL_KEYWORDS


@pytest.fixture
def scraper():
    return AISkillsScraper()


class TestContainsAnyKeyword:
    def test_matches_exact_keyword(self, scraper):
        assert scraper._contains_any_keyword("intro to prompt engineering today", ("prompt engineering",))

    def test_no_match_for_substring(self, scraper):
        assert not scraper._contains_any_keyword("promptengineering is not valid", ("prompt engineering",))

    def test_matches_at_start(self, scraper):
        assert scraper._contains_any_keyword("python is great", ("python",))

    def test_matches_at_end(self, scraper):
        assert scraper._contains_any_keyword("learn python", ("python",))

    def test_no_match_unrelated(self, scraper):
        assert not scraper._contains_any_keyword("java programming", ("python",))

    def test_matches_any_of_multiple(self, scraper):
        assert scraper._contains_any_keyword("using lora for adaptation", ("fine-tuning", "lora", "qlora"))


class TestFetchTopStories:
    @patch("infomoth.ai_skill_scraper.requests.get")
    @patch("infomoth.ai_skill_scraper.feedparser.parse")
    def test_returns_stories(self, mock_parse, mock_get, scraper):
        mock_get.return_value = MagicMock(content=b"<rss/>")
        entry = MagicMock()
        entry.title = "Test Story"
        entry.summary = "About python"
        mock_parse.return_value = MagicMock(entries=[entry])
        stories = scraper._fetch_top_stories()
        assert len(stories) == 1
        assert stories[0]["title"] == "Test Story"

    @patch("infomoth.ai_skill_scraper.requests.get")
    def test_returns_empty_on_timeout(self, mock_get, scraper):
        from requests.exceptions import Timeout
        mock_get.side_effect = Timeout()
        assert scraper._fetch_top_stories() == []

    @patch("infomoth.ai_skill_scraper.requests.get")
    def test_returns_empty_on_request_error(self, mock_get, scraper):
        from requests.exceptions import RequestException
        mock_get.side_effect = RequestException("fail")
        assert scraper._fetch_top_stories() == []


class TestScrape:
    @patch.object(AISkillsScraper, "_fetch_top_stories")
    def test_ranks_by_mentions(self, mock_stories, scraper):
        mock_stories.return_value = [
            {"title": "python tips", "text": ""},
            {"title": "python tricks", "text": ""},
            {"title": "rag pipeline", "text": ""},
        ]
        results = scraper.scrape()
        assert results[0]["skill"] == "Python"
        assert results[0]["mentions"] == 2
        assert results[0]["rank"] == 1

    @patch.object(AISkillsScraper, "_fetch_top_stories")
    def test_limits_to_top_8(self, mock_stories, scraper):
        stories = []
        for skill_keywords in SKILL_KEYWORDS.values():
            stories.append({"title": skill_keywords[0], "text": ""})
        stories *= 2
        mock_stories.return_value = stories
        results = scraper.scrape()
        assert len(results) <= 8

    @patch.object(AISkillsScraper, "_fetch_top_stories")
    def test_empty_stories_returns_empty(self, mock_stories, scraper):
        mock_stories.return_value = []
        assert scraper.scrape() == []

    @patch.object(AISkillsScraper, "_fetch_top_stories")
    def test_includes_sample_title(self, mock_stories, scraper):
        mock_stories.return_value = [{"title": "Deep learning breakthrough", "text": ""}]
        results = scraper.scrape()
        dl = next(r for r in results if r["skill"] == "Deep Learning")
        assert dl["sample_post_title"] == "Deep learning breakthrough"

    @patch.object(AISkillsScraper, "_fetch_top_stories")
    def test_output_schema(self, mock_stories, scraper):
        mock_stories.return_value = [{"title": "machine learning ops", "text": ""}]
        results = scraper.scrape()
        entry = results[0]
        assert set(entry.keys()) == {"rank", "skill", "mentions", "date", "source", "sample_post_title"}
        assert entry["source"] == "Hacker News RSS"
