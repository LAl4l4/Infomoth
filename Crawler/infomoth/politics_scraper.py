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
        "url": "https://news.google.com/rss/search?q=site:apnews.com+world",
        "limit": 20,
    },
    {
        "name": "NYTimes World",
        "type": "rss",
        "url": "https://rss.nytimes.com/services/xml/rss/nyt/World.xml",
        "limit": 20,
    },
    {
        "name": "BBC Politics",
        "type": "rss",
        "url": "https://feeds.bbci.co.uk/news/politics/rss.xml",
        "limit": 20,
    },
]


class PoliticsNewsScraper(BaseNewsScraper):
    def __init__(self) -> None:
        super().__init__(category_name="global_politics", sources=POLITICS_SOURCES)
