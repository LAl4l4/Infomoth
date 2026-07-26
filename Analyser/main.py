from analyser import FinanceAnalyser
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor
import json


ROOT_DIR = Path(__file__).resolve().parent
CONFIG_PATH = ROOT_DIR.parent / "Config" / "app-config.json"

def load_config():
    with open(CONFIG_PATH, "r", encoding="utf-8") as file:
        return json.load(file)


def resolve_shared_directory(config):
    shared_directory = Path(config["shared"]["directory"])
    return shared_directory if shared_directory.is_absolute() else ROOT_DIR / shared_directory


def load_mysql_config(config):
    try:
        return config["mysql"]
    except KeyError as error:
        raise KeyError(f"Missing mysql config in {CONFIG_PATH}") from error


def main():
    financeAnalyserPolitic = FinanceAnalyser()
    financeAnalyserTech = FinanceAnalyser()
    config = load_config()
    mysql_config = load_mysql_config(config)
    shared_directory = resolve_shared_directory(config)
    politic_path = shared_directory / "politics_news.json"
    tech_path = shared_directory / "tech_news.json"
    
    with ThreadPoolExecutor(max_workers=2) as executor:
        executor.submit(financeAnalyserPolitic.analyse, politic_path)
        executor.submit(financeAnalyserTech.analyse, tech_path)

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
