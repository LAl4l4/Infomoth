from __future__ import annotations

from .base_scraper import BaseNewsScraper


POLITICS_SOURCES = [
    {
        "name": "Reuters",
        "type": "rss",
        "url": "https://news.google.com/rss/search?q=site:reuters.com",
        "limit": 20,
        # Reuters closed its RSS feeds, so use Google News RSS as a workaround
    },
    {
        "name": "BBC News",
        "type": "rss",
        "url": "http://feeds.bbci.co.uk/news/world/rss.xml",
        "limit": 20,
    },
    {
        "name": "AP News",
        "type": "rss",
        "url": "https://feeds.apnews.com/apf-topnews",
        "limit": 20,
    },
    {
        "name": "Reuters World",
        "type": "html",
        "url": "https://www.reuters.com/world/",
        "base_url": "https://www.reuters.com",
        "selectors": [
            "a[data-testid='Heading']",
            "article a",
        ],
        "limit": 15,
    },
    {
        "name": "BBC Politics",
        "type": "selenium",
        "url": "https://www.bbc.com/news/politics",
        "article_selector": "main a[href*='/news/']",
        "limit": 10,
        "wait_seconds": 5,
        "browser": "chrome",
    },
]


class PoliticsNewsScraper(BaseNewsScraper):
    def __init__(self) -> None:
        super().__init__(category_name="global_politics", sources=POLITICS_SOURCES)
