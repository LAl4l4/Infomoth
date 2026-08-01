from transformers import pipeline
import json


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
        
        for news in self.data:
            title = news.get("title")
            if not isinstance(title, str) or not title.strip():
                print("Cannot find a valid title from json; skipping item")
                continue
            
            results = {r['label']: r['score'] for r in self.pipe(title, top_k=3)}
            
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
        with open(path, "w+", encoding="utf-8") as f:
            json.dump(payload, f, ensure_ascii=False, indent=2)
