"""InfoMoth category-specific news scrapers."""

from .tech_scraper import TechNewsScraper
from .politics_scraper import PoliticsNewsScraper
from .exchange_rate_scraper import ExchangeRateScraper
from .ai_skill_scraper import AISkillsScraper
from .us_stock_index_scraper import USStockIndexScraper

__all__ = [
    "TechNewsScraper",
    "PoliticsNewsScraper",
    "ExchangeRateScraper",
    "AISkillsScraper",
    "USStockIndexScraper",
]
