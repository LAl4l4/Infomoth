#!/bin/sh
set -u

PIPELINE_INTERVAL_SECONDS="${PIPELINE_INTERVAL_SECONDS:-3600}"
STOCK_REFRESH_SECONDS="${STOCK_REFRESH_SECONDS:-300}"

resolve_sync_target() {
    sync_target="${SYNC_TARGET:-}"
    if [ -z "$sync_target" ]; then
        sync_target="$(python -c 'import json; from pathlib import Path; print(json.loads(Path("/app/Config/app-config.json").read_text()).get("pipeline", {}).get("syncTarget", ""))')"
    fi
    printf '%s' "$sync_target"
}

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

    sync_target="$(resolve_sync_target)"

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

run_stock_refresh() {
    echo "[$(date)] Refreshing intraday US stock indices..."
    if ! (cd /app/Crawler && python main.py --stocks-only); then
        echo "[$(date)] US stock refresh failed; keeping the previous snapshot." >&2
        return 1
    fi

    sync_target="$(resolve_sync_target)"
    if [ -z "$sync_target" ]; then
        echo "[$(date)] No rsync target configured; cannot sync US stock indices." >&2
        return 1
    fi
    case "$sync_target" in
        */) stock_target="$sync_target" ;;
        *) stock_target="$sync_target/" ;;
    esac

    if ! rsync -az --no-owner --no-group /app/Shared/us_stock_indices.json "$stock_target"; then
        echo "[$(date)] US stock snapshot sync failed." >&2
        return 1
    fi
    echo "[$(date)] Intraday US stock indices refreshed."
}

trap 'exit 0' INT TERM

while true; do
    run_pipeline || echo "[$(date)] Pipeline cycle failed; it will retry on the next full cycle." >&2
    remaining="$PIPELINE_INTERVAL_SECONDS"
    while [ "$remaining" -gt 0 ]; do
        sleep_seconds="$STOCK_REFRESH_SECONDS"
        if [ "$sleep_seconds" -gt "$remaining" ]; then
            sleep_seconds="$remaining"
        fi
        sleep "$sleep_seconds"
        remaining=$((remaining - sleep_seconds))
        if [ "$remaining" -gt 0 ]; then
            run_stock_refresh || true
        fi
    done
done
