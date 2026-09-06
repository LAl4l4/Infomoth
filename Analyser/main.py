from analyser import FinanceAnalyser
from pathlib import Path
import logging
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
    analyser = FinanceAnalyser()
    shared_directory = resolve_shared_directory(load_config())
    failures = []
    for name in ("politics_news.json", "tech_news.json"):
        try:
            analyser.analyse(shared_directory / name)
        except Exception as error:
            logging.exception("Cannot analyse %s; continuing with other sources", name)
            failures.append(error)
    if failures:
        raise RuntimeError("analysis failed") from failures[0]

    print("Analyser executed")

if __name__ == "__main__":
    main()
