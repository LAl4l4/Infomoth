# Fixes

## Sentiment samples written more than once

The Backend still checks the sentiment JSON files on its existing schedule, but it now stores the SHA-256 fingerprint of each file in `sentiment_file_state`. The table has exactly one row (`ID = 1`) with one hash column for politics and one for technology. A scheduler run compares the current file hash with that row and only inserts a sentiment sample, then updates the hash, when the JSON changed. The checkpoint is transactional with the sample insert, so a restart does not lose the deduplication state and the stored state remains O(1).

To repair rows produced before the checkpoint existed, run [`scripts/fix_sentiment_duplicates.sql`](scripts/fix_sentiment_duplicates.sql) against the deployed MySQL 8 database after the updated Backend has started once:

```sh
docker compose -f docker-compose.app.yml exec -T mysql \
  mysql -uroot infomoth < scripts/fix_sentiment_duplicates.sql
```

Historical rows do not contain a fingerprint, so the repair identifies the old scheduler's repeated writes as consecutive rows with the same raw sentiment score and keeps the first row in each run. It removes the later duplicates, then rebuilds the cumulative mean, Bessel-corrected sample standard deviation, and sample count for both sentiment tables. It runs in a transaction and is idempotent. Take the normal database backup before running a delete-based repair.
