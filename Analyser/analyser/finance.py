from transformers import pipeline
import json


class FinanceAnalyser:

    def __init__(self):
        self.pipe = pipeline(
            "sentiment-analysis",
            model="ProsusAI/finbert"
        )


    def analyse(self, path) -> None:
        self.loadJson(path)
        analysedNews = []
        
        for news in self.data:
            try:
                title = news["title"]
            except Exception:
                continue
            
            analysedNews.append(
                {
                    **news,
                    "financeInfluence": self.pipe(title)[0]
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