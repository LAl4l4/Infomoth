from __future__ import annotations

import logging
import re
from collections import defaultdict
from datetime import datetime, timezone
from typing import Dict, List

import feedparser
import requests
from requests.exceptions import RequestException, Timeout

LOGGER = logging.getLogger(__name__)

_TIMEOUT = 15
_HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
    "Accept-Language": "en-US,en;q=0.9",
    "Accept-Encoding": "gzip, deflate, br",
    "Connection": "keep-alive",
    "Sec-Fetch-Dest": "document",
    "Sec-Fetch-Mode": "navigate",
    "Sec-Fetch-Site": "none",
    "Cache-Control": "max-age=0",
}
_HN_RSS_URL = "https://news.ycombinator.com/rss"
_STORY_LIMIT = 80

SKILL_KEYWORDS = {
    "Prompt Engineering": ("prompt engineering", "prompting", "prompt design"),
    "Python": ("python", "python coding", "python scripting"),
    "Machine Learning": ("machine learning", "ml model", "mlops"),
    "Deep Learning": ("deep learning", "neural network", "transformer"),
    "LLM Fine-tuning": ("fine-tuning", "finetuning", "lora", "qlora"),
    "RAG": ("rag", "retrieval augmented generation", "vector database"),
    "AI Agent Development": ("ai agent", "agentic", "autonomous agent"),
    "Model Deployment": ("model deployment", "serving", "inference api"),
    "Data Engineering": ("data pipeline", "feature engineering", "data cleaning"),
    "AI Evaluation": ("model evaluation", "benchmark", "evals"),
}


class AISkillsScraper:
    """Derive today's popular AI skills from Hacker News RSS feed."""

    def scrape(self) -> List[Dict[str, object]]:
        skill_counts: Dict[str, int] = defaultdict(int)
        sample_titles: Dict[str, str] = {}
        today = datetime.now(timezone.utc).strftime("%Y-%m-%d")

        for story in self._fetch_top_stories():
            text = f"{story.get('title', '')} {story.get('text', '')}".lower()
            for skill, keywords in SKILL_KEYWORDS.items():
                if self._contains_any_keyword(text, keywords):
                    skill_counts[skill] += 1
                    sample_titles.setdefault(skill, story.get("title", ""))

        ranked = sorted(skill_counts.items(), key=lambda item: (-item[1], item[0]))

        results: List[Dict[str, object]] = []
        for idx, (skill, mentions) in enumerate(ranked[:8], start=1):
            results.append(
                {
                    "rank": idx,
                    "skill": skill,
                    "mentions": mentions,
                    "date": today,
                    "source": "Hacker News RSS",
                    "sample_post_title": sample_titles.get(skill, ""),
                }
            )

        LOGGER.info("Fetched %s AI skills for %s", len(results), today)
        return results

    def _fetch_top_stories(self) -> List[Dict[str, str]]:
        try:
            response = requests.get(
                _HN_RSS_URL,
                headers=_HEADERS,
                timeout=_TIMEOUT,
            )
            response.raise_for_status()
            parsed = feedparser.parse(response.content)
        except Timeout:
            LOGGER.warning("Timeout fetching Hacker News RSS")
            return []
        except RequestException as exc:
            LOGGER.warning("Request failed for Hacker News RSS: %s", exc)
            return []

        entries = getattr(parsed, "entries", [])
        if not entries:
            LOGGER.warning("No entries found in Hacker News RSS")
            return []

        stories: List[Dict[str, str]] = []
        for entry in entries[:_STORY_LIMIT]:
            stories.append(
                {
                    "title": getattr(entry, "title", "") or "",
                    "text": getattr(entry, "summary", "") or "",
                }
            )
        return stories

    def _contains_any_keyword(self, text: str, keywords: tuple[str, ...]) -> bool:
        for keyword in keywords:
            escaped = re.escape(keyword)
            pattern = rf"(^|[^a-z0-9]){escaped}([^a-z0-9]|$)"
            if re.search(pattern, text):
                return True
        return False


if __name__ == "__main__":
    scraper = AISkillsScraper()
    skills = scraper.scrape()
    for skill in skills:
        print(f"{skill['rank']}. {skill['skill']} - {skill['mentions']} mentions - Sample post: {skill['sample_post_title']}")
