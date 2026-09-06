# Analyser PROJECT_SPEC

## 1. Project Positioning
- **Type**: Python Batch Analysis Task
- **Responsibilities**:
  - Read crawler-generated news data from `shared.directory` in `Config/app-config.json`.
  - Apply finance-oriented sentiment analysis to news titles.
  - Write enriched analysis results back to the same JSON files.

## 2. Tech Stack & Frameworks
- **Language/Runtime**: Python `3.13.3`
- **Core Dependencies**:
  - `transformers`: model loading and inference pipeline
  - `torch`: runtime backend for transformer inference
  - `json` (stdlib): local JSON read/write
  - Single shared FinBERT pipeline: sequential source processing with batches of eight titles
- **Primary Model**:
  - `ProsusAI/finbert`

## 3. Module Structure
- `main.py`:
  - Entry point for analysis tasks.
  - Reads these files under the configured shared directory:
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
- **Input Files** (`shared.directory`):
  - `politics_news.json`
  - `tech_news.json`
- **Input Item Requirements**:
  - News item should contain `title`; items without valid `title` are skipped.
- **Output Behavior**:
  - Writes back to original input files in-place.
  - Keeps existing fields and appends:
    - `financeInfluence`: FinBERT sentiment output object.
  - Does not connect to or write to MySQL; database persistence belongs to Backend.

## 5. Operational Notes
- The analyser mutates source JSON files directly; downstream readers must tolerate the additional `financeInfluence` field.
- The first execution may be slower due to model download/loading.
- Network access is required when model artifacts are not already cached locally.

## 6. AI Agent Development Guidelines (Analyser)
- Keep analysis output schema backward compatible by only appending fields unless explicitly requested.
- Keep read/write paths aligned with `shared.directory`.
- Keep database persistence out of Analyser; Backend owns database writes.
- Prefer updating `finance.py` for analysis logic changes; avoid hardcoding model behavior in `main.py`.

## Model reuse and failure handling
- One FinBERT instance processes both source files sequentially, using batches of eight titles and truncation to the model input limit. Both sources are attempted even if one fails; the process raises after completing the attempts so Pipeline can report analysis failure and still sync available output.
- JSON output replaces the prior snapshot atomically only after inference and serialization complete. Failed analysis preserves the input file.
