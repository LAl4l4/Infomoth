from analyser import FinanceAnalyser
from pathlib import Path


ROOT_DIR = Path(__file__).resolve().parent
SHARED_DIR = ROOT_DIR.parent / "Shared"
POLITIC_PATH = SHARED_DIR / "politics_news.json"


def main():
    financeAnalyser = FinanceAnalyser()
    
    financeAnalyser.analyse(POLITIC_PATH)


if __name__ == "__main__":
    main()