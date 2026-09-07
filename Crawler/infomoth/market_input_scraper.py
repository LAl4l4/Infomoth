"""Acquire raw model inputs; normalization and prediction belong to Backend.

Observation dates are never substituted with crawl dates. fetchedAt records the
successful retrieval time, not a claim about the original publication time.
"""
from __future__ import annotations

import csv
import io
import json
import logging
import math
import re
import zipfile
from concurrent.futures import ThreadPoolExecutor, as_completed
from datetime import date, datetime, timedelta, timezone

import requests
from bs4 import BeautifulSoup

LOGGER = logging.getLogger(__name__)
FRED_URL = "https://fred.stlouisfed.org/graph/fredgraph.csv"
CBOE_URL = "https://www.cboe.com/markets/us/options/market-statistics/daily"
AAII_URL = "https://www.aaii.com/sentimentsurvey"
NAAIM_URL = "https://index.naaim.org/embeddable/chart"
FRED_SERIES = {
    "SP500": "sp500", "VIXCLS": "vix", "VXVCLS": "vix3m",
    "DGS10": "treasury10y", "BAMLH0A0HYM2": "hy_spread",
}


class MarketInputScraper:
    """Independent public-source adapters with explicit partial-failure status."""

    def __init__(self):
        self.today = datetime.now(timezone.utc).date()

    @staticmethod
    def _get(url, **kwargs):
        response = requests.get(url, timeout=30, **kwargs)
        response.raise_for_status()
        return response

    def _point(self, indicator, observed, value, source):
        observed = date.fromisoformat(str(observed)[:10])
        number = float(value)
        if not math.isfinite(number) or observed > self.today:
            raise ValueError("Invalid market observation")
        return {"indicator": indicator, "date": observed.isoformat(), "value": number,
                "source": source, "fetchedAt": datetime.now(timezone.utc).isoformat()}

    def scrape(self):
        observations, sources = [], []
        jobs = {"fred": self.fetch_fred, "cboe": self.fetch_cboe,
                "aaii": self.fetch_aaii, "naaim": self.fetch_naaim, "cftc": self.fetch_cftc}
        with ThreadPoolExecutor(max_workers=5) as executor:
            futures = {executor.submit(fn): name for name, fn in jobs.items()}
            for future in as_completed(futures):
                name = futures[future]
                try:
                    points = future.result()
                    if not points:
                        raise ValueError("No dated observations returned")
                    observations.extend(points)
                    partial = name == "fred" and not set(FRED_SERIES.values()).issubset({p["indicator"] for p in points})
                    sources.append({"source": name, "status": "delayed" if name == "naaim" else "partial" if partial else "ok",
                                    "message": "Public data has a three-month delay" if name == "naaim" else "Some series unavailable" if partial else "",
                                    "attemptedAt": datetime.now(timezone.utc).isoformat()})
                except Exception as exc:
                    LOGGER.warning("Market input source %s failed: %s", name, exc)
                    sources.append({"source": name, "status": "error",
                                    "message": "Source unavailable or response format changed",
                                    "attemptedAt": datetime.now(timezone.utc).isoformat()})
        return {"observations": observations, "sources": sources}

    def fetch_fred(self):
        # Separate requests prevent one slow series from discarding the other inputs.
        def fetch(series):
            response = self._get(FRED_URL, params={"id": series,
                                "cosd": (self.today - timedelta(days=400)).isoformat()})
            return self.parse_fred(response.content)

        points = []
        with ThreadPoolExecutor(max_workers=3) as executor:
            futures = {executor.submit(fetch, series): series for series in FRED_SERIES}
            for future in as_completed(futures):
                try:
                    points.extend(future.result())
                except Exception as exc:
                    LOGGER.warning("FRED series %s unavailable: %s", futures[future], exc)
        return points

    def parse_fred(self, content):
        # FRED packages series with different frequencies into separate CSVs.
        if zipfile.is_zipfile(io.BytesIO(content)):
            with zipfile.ZipFile(io.BytesIO(content)) as archive:
                tables = [archive.read(n).decode("utf-8-sig") for n in archive.namelist() if n.endswith(".csv")]
        else:
            tables = [content.decode("utf-8-sig")]
        points = []
        cutoff = (self.today - timedelta(days=400)).isoformat()
        for table in tables:
            for row in csv.DictReader(io.StringIO(table)):
                observed = row.get("observation_date", row.get("DATE", ""))
                if not cutoff <= observed <= self.today.isoformat():
                    continue
                for series, indicator in FRED_SERIES.items():
                    if row.get(series, "") not in ("", "."):
                        points.append(self._point(indicator, observed, row[series], "fred"))
        return points

    def fetch_cboe(self):
        return self.parse_cboe(self._get(CBOE_URL).text)

    def parse_cboe(self, html):
        # Read the source-selected session date, including Next.js escaped data.
        unescaped = html.replace('\\"', '"')
        match = re.search(r'"selectedDate"\s*:\s*"(\d{4}-\d{2}-\d{2})"', unescaped)
        if not match:
            raise ValueError("Cboe session date missing")
        points = []
        for row in BeautifulSoup(html, "html.parser").select("tr"):
            cells = [cell.get_text(" ", strip=True) for cell in row.select("td")]
            if len(cells) >= 2 and cells[0] == "EQUITY PUT/CALL RATIO":
                points.append(self._point("put_call", match[1], cells[1], "cboe"))
        return points

    def fetch_aaii(self):
        return self.parse_aaii(self._get(AAII_URL, headers={"User-Agent": "InfoMoth/1.0 (market research)"}).text)

    def parse_aaii(self, html):
        text = BeautifulSoup(html, "html.parser").get_text(" ", strip=True)
        match = re.search(r"Week ending\s+(\w+\s+\d{1,2},\s*\d{4})", text, re.I)
        if not match:
            raise ValueError("AAII survey date missing; subscription or access may be required")
        observed = datetime.strptime(match[1], "%B %d, %Y").date()
        current = text[match.end():]
        points = []
        for label, code in [("Bullish", "aaii_bull"), ("Bearish", "aaii_bear")]:
            value = re.search(label + r"\s+(\d+(?:\.\d+)?)\s*%", current)
            if not value or not 0 <= float(value[1]) <= 100:
                raise ValueError("AAII percentage missing")
            points.append(self._point(code, observed, value[1], "aaii"))
        if sum(point["value"] for point in points) > 100.1:
            raise ValueError("Invalid AAII survey percentages")
        return points

    def fetch_naaim(self):
        return self.parse_naaim(self._get(NAAIM_URL).text)

    def parse_naaim(self, html):
        # Only use the publicly embedded, delayed series; never the undated headline.
        attr = "data-symfony--ux-chartjs--chart-view-value"
        node = BeautifulSoup(html, "html.parser").select_one(f"[{attr}]")
        if node is None:
            raise ValueError("NAAIM dated chart missing")
        chart = json.loads(node[attr])["data"]
        dataset = next(item for item in chart["datasets"] if item["label"] == "NAAIM Number")
        if len(chart["labels"]) != len(dataset["data"]):
            raise ValueError("NAAIM date/value count mismatch")
        return [self._point("naaim", day, value, "naaim")
                for day, value in zip(chart["labels"], dataset["data"])
                if value is not None and day >= (self.today - timedelta(days=400)).isoformat()]

    def fetch_cftc(self):
        points = []
        # Keep last year's report around the January boundary.
        for year in sorted({self.today.year, (self.today - timedelta(days=90)).year}):
            url = f"https://www.cftc.gov/files/dea/history/fut_fin_txt_{year}.zip"
            points.extend(self.parse_cftc(self._get(url).content))
        return points

    def parse_cftc(self, content):
        with zipfile.ZipFile(io.BytesIO(content)) as archive:
            name = next(n for n in archive.namelist() if n.lower().endswith(".txt"))
            rows = csv.DictReader(io.StringIO(archive.read(name).decode("utf-8-sig")))
            points = []
            for row in rows:
                # One contract only: mixing E-mini, micro and consolidated double-counts positions.
                if row["CFTC_Contract_Market_Code"].strip() != "13874A":
                    continue
                observed = row["Report_Date_as_YYYY-MM-DD"]
                fields = {"cot_long": "Lev_Money_Positions_Long_All",
                          "cot_short": "Lev_Money_Positions_Short_All", "cot_oi": "Open_Interest_All"}
                for code, field in fields.items():
                    points.append(self._point(code, observed, row[field], "cftc"))
            return points
