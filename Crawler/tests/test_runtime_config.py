from __future__ import annotations

import json

from infomoth import runtime_config


def test_load_shared_directory_resolves_relative_path_from_config_directory(monkeypatch, tmp_path):
    config_path = tmp_path / "Config" / "app-config.json"
    config_path.parent.mkdir()
    config_path.write_text(json.dumps({"shared": {"directory": "../Shared"}}), encoding="utf-8")
    monkeypatch.setattr(runtime_config, "CONFIG_PATH", config_path)

    assert runtime_config.load_shared_directory() == tmp_path / "Shared"


def test_load_shared_directory_preserves_absolute_path(monkeypatch, tmp_path):
    config_path = tmp_path / "Config" / "app-config.json"
    config_path.parent.mkdir()
    shared_directory = tmp_path / "external-shared"
    config_path.write_text(json.dumps({"shared": {"directory": str(shared_directory)}}), encoding="utf-8")
    monkeypatch.setattr(runtime_config, "CONFIG_PATH", config_path)

    assert runtime_config.load_shared_directory() == shared_directory
