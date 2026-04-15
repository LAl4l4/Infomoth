from __future__ import annotations

from .base_scraper import BaseNewsScraper


TECH_SOURCES = [
    {
        "name": "TechCrunch",
        "type": "rss",
        "url": "https://techcrunch.com/feed/",
        "limit": 20,
    },
    {
        "name": "The Verge",
        "type": "rss",
        "url": "https://www.theverge.com/rss/index.xml",
        "limit": 20,
    },
    {
        "name": "WIRED",
        "type": "rss",
        "url": "https://www.wired.com/feed/rss",
        "limit": 20,
    },
    {
        "name": "Ars Technica",
        "type": "html",
        "url": "https://arstechnica.com/",
        "base_url": "https://arstechnica.com",
        "selectors": [
            "article h2 a",
            "h2.entry-title a",
        ],
        "limit": 15,
    },
]


class TechNewsScraper(BaseNewsScraper):
    def __init__(self) -> None:
        super().__init__(category_name="technology", sources=TECH_SOURCES)
