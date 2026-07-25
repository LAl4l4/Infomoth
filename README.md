# InfoMoth

> A clearer way to follow the news, markets, and signals shaping tomorrow.

InfoMoth brings technology news, global politics, market data, exchange rates, AI trends, and financial sentiment into one focused workspace.

## Try InfoMoth

**[Open the live app →](https://149.118.67.207.nip.io)**

No setup is required to explore the product.

## Why InfoMoth?

Important signals are scattered across news sites, market dashboards, and trend reports. Reading them separately makes it difficult to see the relationship between what is happening, how markets are moving, and what deserves attention next.

InfoMoth is designed to make that first pass simpler:

- See the day's most relevant technology and politics news in one place.
- Put news alongside exchange rates and major US stock indexes.
- Use finance-oriented sentiment analysis as an additional signal while reading.
- Follow emerging AI skills and areas of growing interest.
- Keep personal accounts, profiles, and preferences in the same workspace.

The goal is not to replace judgment. It is to reduce the friction between *finding information* and *understanding what may matter*.

## What you get

### News

A focused stream of technology and international politics coverage, normalized into a consistent reading experience.

### Market context

Exchange rates and major US stock index snapshots provide context around the stories in the feed.

### Sentiment signals

FinBERT-based analysis adds a finance-oriented sentiment signal to news items. It is a supporting indicator, not investment advice.

### AI trend tracking

A dedicated view of AI skill trends helps surface topics and capabilities that are gaining attention.

### A personal workspace

JWT-protected accounts, profiles, settings, and a configurable home experience keep the product useful beyond a single visit.

## How it works

InfoMoth follows a simple daily loop:

1. News and market data are collected from external sources.
2. News is enriched with financial sentiment analysis.
3. The finished data is presented through the web app and API.
4. The pipeline can run independently from the application, making the product suitable for a small self-hosted deployment.

The product is currently deployed across two Oracle Cloud Always Free instances: one for the application and one for the data pipeline.

## Run your own instance

The fastest self-hosted path is Docker Compose:

```bash
docker compose -f docker-compose.app.yml up -d --build
```

Then open [http://localhost](http://localhost).

To run the data pipeline:

```bash
docker compose -f docker-compose.pipeline.yml build
docker compose -f docker-compose.pipeline.yml run --rm pipeline
```

For the complete Oracle Cloud deployment process, including the two-VM setup, SSH synchronization, scheduled runs, backups, and rollback, see [DEPLOYMENT.md](DEPLOYMENT.md).

## Project status

InfoMoth is under active development. Product behavior, data sources, and deployment details may evolve.

## License

InfoMoth is licensed under the [Apache License 2.0](LICENSE).

Third-party data sources and model artifacts may have their own terms and licenses. Users are responsible for complying with those terms when operating their own instance.

