from __future__ import annotations

import json
from pathlib import Path


ROOT_DIR = Path(__file__).resolve().parents[2]
CONFIG_PATH = ROOT_DIR / "Config" / "app-config.json"


def load_shared_directory() -> Path:
    with CONFIG_PATH.open(encoding="utf-8") as file:
        config = json.load(file)

    directory = Path(config["shared"]["directory"])
    return directory if directory.is_absolute() else ROOT_DIR / directory
