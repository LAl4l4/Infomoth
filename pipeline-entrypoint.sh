#!/bin/sh
set -u

INTERVAL_SECONDS=3600

run_pipeline() {
    echo "[$(date)] Running Crawler..."
    if ! (cd /app/Crawler && python main.py); then
        echo "[$(date)] Crawler failed; skipping analysis and sync." >&2
        return 1
    fi

    echo "[$(date)] Running Analyser..."
    if ! (cd /app/Analyser && python main.py); then
        echo "[$(date)] Analyser failed; syncing crawler output without analysis." >&2
    fi

    sync_target="${SYNC_TARGET:-}"
    if [ -z "$sync_target" ]; then
        sync_target="$(python -c 'import json; from pathlib import Path; print(json.loads(Path("/app/Config/app-config.json").read_text()).get("pipeline", {}).get("syncTarget", ""))')"
    fi

    if [ -z "$sync_target" ]; then
        echo "[$(date)] No rsync target configured; set SYNC_TARGET or pipeline.syncTarget." >&2
        return 1
    fi

    echo "[$(date)] Syncing Shared/ to $sync_target..."
    if ! rsync -az --no-owner --no-group --delete /app/Shared/ "$sync_target"; then
        echo "[$(date)] rsync failed." >&2
        return 1
    fi

    echo "[$(date)] Pipeline complete."
}

trap 'exit 0' INT TERM

while true; do
    run_pipeline || echo "[$(date)] Pipeline cycle failed; it will retry next hour." >&2
    echo "[$(date)] Next pipeline run starts in one hour."
    sleep "$INTERVAL_SECONDS"
done
