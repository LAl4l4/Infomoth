"""InfoMoth category-specific news scrapers."""

from .tech_scraper import TechNewsScraper
from .politics_scraper import PoliticsNewsScraper
from .exchange_rate_scraper import ExchangeRateScraper
from .ai_skill_scraper import AISkillsScraper

__all__ = ["TechNewsScraper", "PoliticsNewsScraper", "ExchangeRateScraper", "AISkillsScraper"]
