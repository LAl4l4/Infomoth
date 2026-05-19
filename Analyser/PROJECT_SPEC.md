# Analyser PROJECT_SPEC

## 1. Project Positioning
- **Type**: Python Batch Analysis Task
- **Responsibilities**:
  - Read crawler-generated news data from `../Shared`.
  - Apply finance-oriented sentiment analysis to news titles.
  - Write enriched analysis results back to the same JSON files.

## 2. Tech Stack & Frameworks
- **Language/Runtime**: Python `3.13.3`
- **Core Dependencies**:
  - `transformers`: model loading and inference pipeline
  - `torch`: runtime backend for transformer inference
  - `json` (stdlib): local JSON read/write
  - `ThreadPoolExecutor` (stdlib): concurrent processing of multiple news files
- **Primary Model**:
  - `ProsusAI/finbert`

## 3. Module Structure
- `main.py`:
  - Entry point for analysis tasks.
  - Reads these files under `../Shared`:
    - `politics_news.json`
    - `tech_news.json`
  - Dispatches analysis jobs in parallel.
- `analyser/finance.py`:
  - `FinanceAnalyser` implementation.
  - Uses FinBERT sentiment pipeline and injects `financeInfluence` into each news item.
- `analyser/modelDownloader.py`:
  - Optional utility to pre-download tokenizer/model artifacts to local `./models` cache.
- `analyser/__init__.py`:
  - Public package export.

## 4. Input/Output Contracts
- **Execution Command**: `python main.py`
- **Input Files** (`../Shared`):
  - `politics_news.json`
  - `tech_news.json`
- **Input Item Requirements**:
  - News item should contain `title`; items without valid `title` are skipped.
- **Output Behavior**:
  - Writes back to original input files in-place.
  - Keeps existing fields and appends:
    - `financeInfluence`: FinBERT sentiment output object.

## 5. Operational Notes
- The analyser mutates source JSON files directly; downstream readers must tolerate the additional `financeInfluence` field.
- The first execution may be slower due to model download/loading.
- Network access is required when model artifacts are not already cached locally.

## 6. AI Agent Development Guidelines (Analyser)
- Keep analysis output schema backward compatible by only appending fields unless explicitly requested.
- Keep read/write paths aligned with `Shared` directory conventions used by Crawler and Backend.
- Prefer updating `finance.py` for analysis logic changes; avoid hardcoding model behavior in `main.py`.
