# InfoMoth

InfoMoth scrapes global news into two categories:
- Technology (`tech_news.json`)
- Global Politics (`politics_news.json`)

The scraper flow is:
1. RSS/Atom with `feedparser` (first choice)
2. Standard HTML with `requests` + `BeautifulSoup`
3. Headless Selenium only for JS-rendered pages

## Setup

Install dependencies:

```bash
pip install -r requirements.txt
```

## Selenium Driver Requirements

Selenium is used only for JS-rendered sources. You must install one of:
- Chrome + ChromeDriver
- Firefox + GeckoDriver

Make sure the driver is compatible with your browser version and available in your `PATH`.

## Run

```bash
python main.py
```

## Output

Running the script writes exactly these JSON outputs in the project root:
- `tech_news.json`
- `politics_news.json`

Each record includes:
- `title`
- `link`
- `summary`
- `published_date`
- `source`

## Notes

- Chinese-specific sources and language-specific parsing have been removed.
- HTTP calls use rotating custom User-Agents and explicit timeout handling.
- 403 Forbidden responses are handled with retries using alternate User-Agents.
