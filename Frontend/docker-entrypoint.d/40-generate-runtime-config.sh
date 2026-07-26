#!/bin/sh
set -eu

config_path=/Config/app-config.json
runtime_config_path=/usr/share/nginx/html/app-config.json

if [ ! -f "$config_path" ]; then
    echo "Missing frontend runtime configuration at $config_path" >&2
    exit 1
fi

jq '{frontend: .frontend}' "$config_path" > "$runtime_config_path"
