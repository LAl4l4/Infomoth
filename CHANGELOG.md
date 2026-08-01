# Changelog

- `08/01/2026`:
    - Added a container-owned hourly Pipeline schedule that runs Crawler, Analyser, and rsync sequentially.
    - Removed Analyser's direct MySQL persistence; database writes are reserved for Backend.
    - Added staged, auto-starting `deployapp` and `deploypipe` Make targets for build, package, upload, and remote startup.
    - Removed FinBERT files from the Pipeline image and build context; `make submit-models` now uploads them independently to the Pipeline VM for read-only volume mounting.

- `07/26/2026`: 
    - Moved frontend, backend, crawler, analyser, and pipeline cross-service settings into runtime config; added deployable database schemas with automatic initialization.
