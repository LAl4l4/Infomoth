from transformers import pipeline
import json
import os
import tempfile
from pathlib import Path


class FinanceAnalyser:
    
    def __init__(self):
        self.pipe = pipeline(
            "sentiment-analysis",
            model="ProsusAI/finbert",
            local_files_only=True
        )
        
    def analyse(self, path) -> None:
        self.loadJson(path)
        analysedNews = []
        
        valid_news = [news for news in self.data
                      if isinstance(news, dict) and isinstance(news.get("title"), str)
                      and news["title"].strip()]
        predictions = self.pipe([news["title"] for news in valid_news],
                                top_k=3, batch_size=8, truncation=True) if valid_news else []
        for news, prediction in zip(valid_news, predictions):
            results = {r['label']: r['score'] for r in prediction}

            sentimentScore = results.get('positive', 0) - results.get('negative', 0)
            
            analysedNews.append(
                {
                    **news,
                    "financeInfluence": {
                        "positive": results.get('positive', 0),
                        "neutral": results.get('neutral', 0),
                        "negative": results.get('negative', 0),
                        "sentiment_score": sentimentScore
                    }
                }
            )
            
        self.saveJson(path, analysedNews)
        print("analysing successed")
        
    def loadJson(self, path) -> None:
        with open(path, "r", encoding="utf-8") as f:
            self.data = json.load(f)
    
    def saveJson(self, path, payload: list[dict[str, str | list | dict]]) -> None:
        path = Path(path)
        temporary = None
        try:
            with tempfile.NamedTemporaryFile(mode="w", encoding="utf-8", dir=path.parent, delete=False) as file:
                temporary = Path(file.name)
                json.dump(payload, file, ensure_ascii=False, indent=2)
            temporary.chmod(path.stat().st_mode & 0o777 if path.exists() else 0o644)
            os.replace(temporary, path)
        finally:
            if temporary is not None:
                temporary.unlink(missing_ok=True)
