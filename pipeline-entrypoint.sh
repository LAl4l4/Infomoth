#!/bin/sh
set -e

echo "[$(date)] Running Crawler..."
cd /app/Crawler && python main.py

echo "[$(date)] Running Analyser..."
cd /app/Analyser && python main.py

if [ -n "$SYNC_TARGET" ]; then
    echo "[$(date)] Syncing Shared/ to $SYNC_TARGET..."
    rsync -az --delete /app/Shared/ "$SYNC_TARGET"
fi

echo "[$(date)] Pipeline complete."
