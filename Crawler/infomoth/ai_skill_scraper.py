from __future__ import annotations

import logging
import re
from collections import defaultdict
from datetime import datetime, timezone
from typing import Dict, List

import requests
from requests.exceptions import RequestException, Timeout

LOGGER = logging.getLogger(__name__)

_TIMEOUT = 15
_HEADERS = {
    "User-Agent": "InfoMothSkillCrawler/1.0",
    "Accept": "application/json",
}
_SUBREDDITS = ("artificial", "MachineLearning", "ChatGPT")
_POST_LIMIT = 80

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
    """Derive today's popular AI skills from top daily AI community discussions."""

    def scrape(self) -> List[Dict[str, object]]:
        skill_counts: Dict[str, int] = defaultdict(int)
        sample_titles: Dict[str, str] = {}
        today = datetime.now(timezone.utc).strftime("%Y-%m-%d")

        for subreddit in _SUBREDDITS:
            for post in self._fetch_top_posts(subreddit):
                text = f"{post.get('title', '')} {post.get('selftext', '')}".lower()
                for skill, keywords in SKILL_KEYWORDS.items():
                    if self._contains_any_keyword(text, keywords):
                        skill_counts[skill] += 1
                        sample_titles.setdefault(skill, post.get("title", ""))

        ranked = sorted(skill_counts.items(), key=lambda item: (-item[1], item[0]))

        results: List[Dict[str, object]] = []
        for idx, (skill, mentions) in enumerate(ranked[:8], start=1):
            results.append(
                {
                    "rank": idx,
                    "skill": skill,
                    "mentions": mentions,
                    "date": today,
                    "source": "Reddit top daily posts",
                    "sample_post_title": sample_titles.get(skill, ""),
                }
            )

        LOGGER.info("Fetched %s AI skills for %s", len(results), today)
        return results

    def _fetch_top_posts(self, subreddit: str) -> List[Dict[str, str]]:
        url = f"https://www.reddit.com/r/{subreddit}/top.json"
        try:
            response = requests.get(
                url,
                headers=_HEADERS,
                params={"t": "day", "limit": _POST_LIMIT},
                timeout=_TIMEOUT,
            )
            response.raise_for_status()
            payload = response.json()
        except Timeout:
            LOGGER.warning("Timeout fetching subreddit %s", subreddit)
            return []
        except RequestException as exc:
            LOGGER.warning("Request failed for subreddit %s: %s", subreddit, exc)
            return []
        except ValueError as exc:
            LOGGER.warning("Invalid JSON response for subreddit %s: %s", subreddit, exc)
            return []

        children = payload.get("data", {}).get("children", [])
        return [item.get("data", {}) for item in children if isinstance(item, dict)]

    def _contains_any_keyword(self, text: str, keywords: tuple[str, ...]) -> bool:
        for keyword in keywords:
            escaped = re.escape(keyword)
            pattern = rf"(^|[^a-z0-9]){escaped}([^a-z0-9]|$)"
            if re.search(pattern, text):
                return True
        return False
