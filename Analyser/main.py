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
    return shared_directory if shared_directory.is_absolute() else (CONFIG_PATH.parent / shared_directory).resolve()


def main():
    financeAnalyserPolitic = FinanceAnalyser()
    financeAnalyserTech = FinanceAnalyser()
    config = load_config()
    shared_directory = resolve_shared_directory(config)
    politic_path = shared_directory / "politics_news.json"
    tech_path = shared_directory / "tech_news.json"
    
    with ThreadPoolExecutor(max_workers=2) as executor:
        futures = [
            executor.submit(financeAnalyserPolitic.analyse, politic_path),
            executor.submit(financeAnalyserTech.analyse, tech_path),
        ]
        for future in futures:
            future.result()

    print("Analyser executed")

if __name__ == "__main__":
    main()
