from __future__ import annotations

from unittest.mock import MagicMock, patch

import pytest

from infomoth.base_scraper import BaseNewsScraper, NewsItem


@pytest.fixture
def scraper():
    return BaseNewsScraper(category_name="test", sources=[], timeout=5)


class TestAppendItem:
    def test_appends_valid_item(self, scraper):
        scraper._append_item("Title", "http://a.com", "Summary", "2025-01-01", "Src")
        assert len(scraper.items) == 1
        assert scraper.items[0] == NewsItem("Title", "http://a.com", "Summary", "2025-01-01", "Src")

    def test_skips_empty_title(self, scraper):
        scraper._append_item("", "http://a.com", "s", "d", "Src")
        assert len(scraper.items) == 0

    def test_skips_empty_link(self, scraper):
        scraper._append_item("Title", "", "s", "d", "Src")
        assert len(scraper.items) == 0

    def test_deduplicates_same_title_link(self, scraper):
        scraper._append_item("Title", "http://a.com", "s1", "d1", "Src")
        scraper._append_item("Title", "http://a.com", "s2", "d2", "Src")
        assert len(scraper.items) == 1

    def test_strips_whitespace(self, scraper):
        scraper._append_item("  Title  ", "  http://a.com  ", " Sum ", " D ", "Src")
        item = scraper.items[0]
        assert item.title == "Title"
        assert item.link == "http://a.com"
        assert item.summary == "Sum"
        assert item.published_date == "D"


class TestBuildHeaders:
    def test_rotates_user_agents(self, scraper):
        h0 = scraper._build_headers(0)
        h1 = scraper._build_headers(1)
        assert h0["User-Agent"] != h1["User-Agent"]

    def test_wraps_around(self, scraper):
        h0 = scraper._build_headers(0)
        h3 = scraper._build_headers(3)
        assert h0["User-Agent"] == h3["User-Agent"]


class TestGet:
    @patch("infomoth.base_scraper.requests.get")
    def test_returns_response_on_success(self, mock_get, scraper):
        mock_resp = MagicMock()
        mock_resp.status_code = 200
        mock_get.return_value = mock_resp
        result = scraper._get("http://example.com")
        assert result is mock_resp

    @patch("infomoth.base_scraper.requests.get")
    def test_retries_on_403(self, mock_get, scraper):
        forbidden = MagicMock()
        forbidden.status_code = 403
        ok = MagicMock()
        ok.status_code = 200
        mock_get.side_effect = [forbidden, ok]
        result = scraper._get("http://example.com")
        assert result is ok
        assert mock_get.call_count == 2

    @patch("infomoth.base_scraper.requests.get")
    def test_returns_none_on_request_exception(self, mock_get, scraper):
        from requests.exceptions import RequestException
        mock_get.side_effect = RequestException("fail")
        result = scraper._get("http://example.com")
        assert result is None


class TestScrapeRssSource:
    @patch("infomoth.base_scraper.feedparser.parse")
    @patch.object(BaseNewsScraper, "_get")
    def test_parses_rss_entries(self, mock_get, mock_parse, scraper):
        mock_get.return_value = MagicMock(content=b"<rss/>")
        entry = MagicMock()
        entry.get = lambda key, default="": {
            "title": "Hello",
            "link": "http://x.com",
            "summary": "World",
            "published": "2025-01-01",
        }.get(key, default)
        mock_parse.return_value = MagicMock(entries=[entry])

        source = {"name": "Test", "url": "http://feed.com", "limit": 5}
        result = scraper._scrape_rss_source(source)
        assert result is True
        assert len(scraper.items) == 1
        assert scraper.items[0].title == "Hello"

    @patch.object(BaseNewsScraper, "_get")
    def test_returns_false_when_get_fails(self, mock_get, scraper):
        mock_get.return_value = None
        source = {"name": "Test", "url": "http://feed.com"}
        assert scraper._scrape_rss_source(source) is False


class TestExtractHtmlItems:
    def test_extracts_links_from_selectors(self, scraper):
        html = '<div class="article"><a href="/post/1">First Post</a></div>'
        source = {
            "name": "Test",
            "base_url": "http://site.com",
            "selectors": [".article a"],
            "limit": 10,
        }
        items = list(scraper._extract_html_items(html, source))
        assert len(items) == 1
        assert items[0]["title"] == "First Post"
        assert items[0]["link"] == "http://site.com/post/1"

    def test_respects_limit(self, scraper):
        html = "".join(
            f'<div class="a"><a href="/p/{i}">Post {i}</a></div>' for i in range(10)
        )
        source = {"name": "T", "base_url": "http://s.com", "selectors": [".a a"], "limit": 3}
        items = list(scraper._extract_html_items(html, source))
        assert len(items) == 3

    def test_skips_nodes_without_href(self, scraper):
        html = '<div class="a"><span>No link</span></div>'
        source = {"name": "T", "selectors": [".a"], "limit": 10}
        items = list(scraper._extract_html_items(html, source))
        assert len(items) == 0


class TestScrape:
    @patch.object(BaseNewsScraper, "_scrape_rss_source")
    def test_dispatches_rss_type(self, mock_rss, scraper):
        mock_rss.return_value = True
        scraper.sources = [{"name": "A", "type": "rss", "url": "http://f.com", "sleep_seconds": 0}]
        scraper.scrape()
        mock_rss.assert_called_once()

    @patch.object(BaseNewsScraper, "_scrape_html_source")
    def test_dispatches_html_type(self, mock_html, scraper):
        scraper.sources = [{"name": "A", "type": "html", "url": "http://s.com", "sleep_seconds": 0}]
        scraper.scrape()
        mock_html.assert_called_once()

    @patch.object(BaseNewsScraper, "_scrape_rss_source")
    def test_falls_back_to_selenium_on_rss_failure(self, mock_rss, scraper):
        mock_rss.return_value = False
        scraper.sources = [{"name": "A", "type": "rss", "url": "http://f.com", "sleep_seconds": 0}]
        with patch.object(BaseNewsScraper, "_scrape_selenium_source") as mock_sel:
            scraper.scrape()
            mock_sel.assert_called_once()
