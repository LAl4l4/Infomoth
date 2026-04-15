from __future__ import annotations

import logging
import time
from dataclasses import asdict, dataclass
from typing import Any, Dict, Iterable, List, Optional, Sequence

import feedparser
import requests
from bs4 import BeautifulSoup
from requests import Response
from requests.exceptions import RequestException, Timeout


LOGGER = logging.getLogger(__name__)


@dataclass
class NewsItem:
    title: str
    link: str
    summary: str
    published_date: str
    source: str


class BaseNewsScraper:
    USER_AGENTS: Sequence[str] = (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    )

    def __init__(self, category_name: str, sources: Sequence[Dict[str, Any]], timeout: int = 15):
        self.category_name = category_name
        self.sources = sources
        self.timeout = timeout
        self.items: List[NewsItem] = []
        self._seen_keys: set[str] = set()

    def scrape(self) -> List[Dict[str, str]]:
        for source in self.sources:
            source_type = source.get("type", "rss")
            if source_type == "rss":
                if not self._scrape_rss_source(source):
                    self._scrape_selenium_source(source)
            elif source_type == "html":
                self._scrape_html_source(source)
            elif source_type == "selenium":
                self._scrape_selenium_source(source)
            else:
                LOGGER.warning("Unknown source type '%s' for source '%s'", source_type, source.get("name"))

            time.sleep(source.get("sleep_seconds", 0.4))

        return [asdict(item) for item in self.items]

    def _build_headers(self, user_agent_index: int = 0) -> Dict[str, str]:
        return {
            "User-Agent": self.USER_AGENTS[user_agent_index % len(self.USER_AGENTS)],
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language": "en-US,en;q=0.9",
        }

    def _get(self, url: str) -> Optional[Response]:
        for i in range(len(self.USER_AGENTS)):
            try:
                response = requests.get(url, headers=self._build_headers(i), timeout=self.timeout)
                if response.status_code == 403:
                    LOGGER.warning("403 Forbidden while requesting %s (attempt %s/%s)", url, i + 1, len(self.USER_AGENTS))
                    continue
                response.raise_for_status()
                return response
            except Timeout:
                LOGGER.warning("Request timeout for %s (attempt %s/%s)", url, i + 1, len(self.USER_AGENTS))
            except RequestException as exc:
                LOGGER.warning("Request failed for %s: %s", url, exc)
                break
        return None

    def _append_item(self, title: str, link: str, summary: str, published_date: str, source: str) -> None:
        title = (title or "").strip()
        link = (link or "").strip()
        if not title or not link:
            return

        dedupe_key = f"{title}|{link}"
        if dedupe_key in self._seen_keys:
            return

        self._seen_keys.add(dedupe_key)
        self.items.append(
            NewsItem(
                title=title,
                link=link,
                summary=(summary or "").strip(),
                published_date=(published_date or "").strip(),
                source=source,
            )
        )

    def _scrape_rss_source(self, source: Dict[str, Any]) -> bool:
        response = self._get(source["url"])
        if response is None:
            return False

        parsed = feedparser.parse(response.content)
        limit = source.get("limit", 15)

        for entry in parsed.entries[:limit]:
            self._append_item(
                title=entry.get("title", ""),
                link=entry.get("link", ""),
                summary=entry.get("summary", "") or entry.get("description", ""),
                published_date=entry.get("published", "") or entry.get("updated", ""),
                source=source["name"],
            )
        return True

    def _extract_html_items(self, html: str, source: Dict[str, Any]) -> Iterable[Dict[str, str]]:
        soup = BeautifulSoup(html, "html.parser")
        selectors = source.get("selectors", [])
        limit = source.get("limit", 15)

        for selector in selectors:
            nodes = soup.select(selector)
            if not nodes:
                continue

            extracted = 0
            for node in nodes:
                link_node = node if node.name == "a" else node.select_one("a[href]")
                if link_node is None:
                    continue

                href = link_node.get("href", "").strip()
                if not href:
                    continue

                if href.startswith("/"):
                    href = source.get("base_url", "").rstrip("/") + href

                title = link_node.get_text(" ", strip=True)
                summary_node = node.select_one(source.get("summary_selector", "")) if source.get("summary_selector") else None
                summary = summary_node.get_text(" ", strip=True) if summary_node else ""
                published_node = node.select_one(source.get("published_selector", "")) if source.get("published_selector") else None
                published_date = published_node.get_text(" ", strip=True) if published_node else ""

                if title and href:
                    extracted += 1
                    yield {
                        "title": title,
                        "link": href,
                        "summary": summary,
                        "published_date": published_date,
                    }

                if extracted >= limit:
                    break

            if extracted:
                return

    def _scrape_html_source(self, source: Dict[str, Any]) -> None:
        response = self._get(source["url"])
        if response is None:
            return

        for item in self._extract_html_items(response.text, source):
            self._append_item(
                title=item["title"],
                link=item["link"],
                summary=item.get("summary", ""),
                published_date=item.get("published_date", ""),
                source=source["name"],
            )

    def _scrape_selenium_source(self, source: Dict[str, Any]) -> None:
        try:
            from selenium import webdriver
            from selenium.webdriver.chrome.options import Options as ChromeOptions
            from selenium.webdriver.common.by import By
            from selenium.webdriver.firefox.options import Options as FirefoxOptions
        except ImportError:
            LOGGER.warning("Selenium is not installed; skipping source %s", source.get("name"))
            return

        browser = source.get("browser", "chrome").lower()
        driver = None
        try:
            if browser == "firefox":
                options = FirefoxOptions()
                options.add_argument("--headless")
                driver = webdriver.Firefox(options=options)
            else:
                options = ChromeOptions()
                options.add_argument("--headless=new")
                options.add_argument("--disable-gpu")
                options.add_argument(f"--user-agent={self.USER_AGENTS[0]}")
                driver = webdriver.Chrome(options=options)

            driver.get(source["url"])
            driver.implicitly_wait(source.get("wait_seconds", 5))

            cards = driver.find_elements(By.CSS_SELECTOR, source["article_selector"])
            for card in cards[: source.get("limit", 15)]:
                try:
                    if card.tag_name.lower() == "a":
                        link_node = card
                    else:
                        link_node = card.find_element(By.CSS_SELECTOR, source.get("link_selector", "a"))
                    title = link_node.text.strip()
                    href = link_node.get_attribute("href")
                    summary = ""
                    published_date = ""

                    if source.get("summary_selector"):
                        summary = card.find_element(By.CSS_SELECTOR, source["summary_selector"]).text.strip()
                    if source.get("published_selector"):
                        published_date = card.find_element(By.CSS_SELECTOR, source["published_selector"]).text.strip()

                    self._append_item(title, href, summary, published_date, source["name"])
                except Exception:
                    continue
        except Exception as exc:
            LOGGER.warning("Selenium scrape failed for %s: %s", source.get("name"), exc)
        finally:
            if driver:
                driver.quit()
