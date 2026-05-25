from analyser import FinanceAnalyser
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
import json


ROOT_DIR = Path(__file__).resolve().parent
SHARED_DIR = ROOT_DIR.parent / "Shared"
CONFIG_PATH = ROOT_DIR.parent / "Config" / "app-config.json"
POLITIC_PATH = SHARED_DIR / "politics_news.json"
TECH_PATH = SHARED_DIR / "tech_news.json"

def load_mysql_config():
    with open(CONFIG_PATH, "r", encoding="utf-8") as file:
        config = json.load(file)

    try:
        return config["analyser"]["mysql"]
    except KeyError as error:
        raise KeyError(f"Missing analyser mysql config in {CONFIG_PATH}") from error

def main():
    financeAnalyserPolitic = FinanceAnalyser()
    financeAnalyserTech = FinanceAnalyser()
    mysql_config = load_mysql_config()
    
    with ThreadPoolExecutor(max_workers=2) as executor:
        executor.submit(financeAnalyserPolitic.analyse, POLITIC_PATH)
        executor.submit(financeAnalyserTech.analyse, TECH_PATH)

    print("Analyser executed")

    financeAnalyserPolitic.setupMysql(**mysql_config)
    financeAnalyserPolitic.saveAverageToMysql("politics_average")
    financeAnalyserTech.setupMysql(**mysql_config)
    financeAnalyserTech.saveAverageToMysql("tech_average")
    
    print("Average saved to mysql")
    
    financeAnalyserPolitic.closeMysql()
    financeAnalyserTech.closeMysql()
    
    print("MySQL connections closed successfully")

if __name__ == "__main__":
    main()
