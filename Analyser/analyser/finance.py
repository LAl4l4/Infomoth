from transformers import pipeline
import json
import mysql.connector
import re
from datetime import date


class FinanceAnalyser:
    
    def __init__(self):
        self.pipe = pipeline(
            "sentiment-analysis",
            model="ProsusAI/finbert",
            local_files_only=True
        )
        
    def setupMysql(self, host, user, password, database):
        self.conn = mysql.connector.connect(
            host=host,
            user=user,
            password=password,
            database=database
        )

    def analyse(self, path) -> None:
        self.loadJson(path)
        analysedNews = []
        average = 0
        count = 0
        
        for news in self.data:
            try:
                title = news["title"]
            except Exception:
                print("Cannot find title from json; maybe a wrong json")
                raise Exception
            
            results = {r['label']: r['score'] for r in self.pipe(title, top_k=3)}
            
            sentimentScore = results.get('positive', 0) - results.get('negative', 0)
            #滚动平均
            count += 1
            average = average + (sentimentScore - average) / count
            
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
            
        self.average = average
        self.saveJson(path, analysedNews)
        print("analysing successed")

    def saveAverageToMysql(self, tableName) -> None:
        if not hasattr(self, "conn"):
            raise ValueError("MySQL connection is not set. Call setupMysql first.")

        if re.fullmatch(r"[A-Za-z_][A-Za-z0-9_]*", tableName) is None:
            raise ValueError("Invalid table name.")

        cursor = self.conn.cursor()
        cursor.execute(
            f"""
            CREATE TABLE IF NOT EXISTS `{tableName}` (
                ID INT AUTO_INCREMENT PRIMARY KEY,
                date DATE NOT NULL,
                sentimentScore DOUBLE NOT NULL
            )
            """
        )
        cursor.execute(
            f"INSERT INTO `{tableName}` (date, sentimentScore) VALUES (%s, %s)",
            (date.today().isoformat(), self.average),
        )
        self.conn.commit()
        cursor.close()
        
        print(f"Average sentiment score {self.average:.4f} saved to MySQL table '{tableName}'")
        
    def closeMysql(self):
        if hasattr(self, 'conn') and self.conn:
            self.conn.close()
            print("MySQL connection closed")
        
    def loadJson(self, path) -> None:
        with open(path, "r", encoding="utf-8") as f:
            self.data = json.load(f)
    
    def saveJson(self, path, payload: list[dict[str, str | list | dict]]) -> None:
        with open(path, "w+", encoding="utf-8") as f:
            json.dump(payload, f, ensure_ascii=False, indent=2)
