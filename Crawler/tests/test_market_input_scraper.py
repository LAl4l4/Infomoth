import io
import json
import zipfile
from datetime import date
from unittest.mock import patch

import pytest

from infomoth.market_input_scraper import MarketInputScraper
from main import save_market_inputs


@pytest.fixture
def scraper():
    result = MarketInputScraper()
    result.today = date(2026, 9, 7)
    return result


def archive(name, content):
    buffer = io.BytesIO()
    with zipfile.ZipFile(buffer, "w") as output:
        output.writestr(name, content)
    return buffer.getvalue()


def test_fred_zip_omits_missing_and_future_observations(scraper):
    content = "observation_date,VIXCLS,DGS10\n2026-09-04,18.2,.\n2026-09-05,,4.1\n2026-09-08,19,4\n"
    points = scraper.parse_fred(archive("daily.csv", content))
    assert [(p["indicator"], p["date"], p["value"]) for p in points] == [
        ("vix", "2026-09-04", 18.2), ("treasury10y", "2026-09-05", 4.1)]
    assert [(p["indicator"], p["value"]) for p in scraper.parse_fred(content.encode())] == [
        ("vix", 18.2), ("treasury10y", 4.1)]


def test_cboe_uses_selected_trading_date_not_crawl_date(scraper):
    html = r'<script>{\"selectedDate\":\"2026-09-04\"}</script><table><tr><td>EQUITY PUT/CALL RATIO</td><td>0.58</td></tr></table>'
    point = scraper.parse_cboe(html)[0]
    assert point["date"] == "2026-09-04"
    assert point["value"] == .58
    with pytest.raises(ValueError):
        scraper.parse_cboe(html.replace("selectedDate", "unknownDate"))


def test_aaii_reads_current_values_not_historical_averages(scraper):
    html = '<p>Week ending September 2, 2026</p><p>Bullish 39.7% Avg 37.5% Neutral 22.7% Bearish 37.6%</p>'
    points = scraper.parse_aaii(html)
    assert [(p["indicator"], p["value"]) for p in points] == [("aaii_bull", 39.7), ("aaii_bear", 37.6)]
    with pytest.raises(ValueError):
        scraper.parse_aaii("Request unsuccessful")


def test_naaim_keeps_delayed_observation_date(scraper):
    chart = {"data": {"labels": ["2026-05-27"], "datasets": [
        {"label": "S&P 500", "data": [7520]}, {"label": "NAAIM Number", "data": [98.39]}]}}
    html = "<canvas data-symfony--ux-chartjs--chart-view-value='" + json.dumps(chart) + "'></canvas>"
    point = scraper.parse_naaim(html)[0]
    assert point["date"] == "2026-05-27"
    assert point["value"] == 98.39


def test_cftc_does_not_mix_mini_and_consolidated_contracts(scraper):
    header = "CFTC_Contract_Market_Code,Report_Date_as_YYYY-MM-DD,Lev_Money_Positions_Long_All,Lev_Money_Positions_Short_All,Open_Interest_All\n"
    content = header + "13874+,2026-09-01,500,400,2000\n13874A,2026-09-01,100,200,1000\n"
    points = scraper.parse_cftc(archive("FinFutYY.txt", content))
    assert [p["value"] for p in points] == [100, 200, 1000]
    assert {p["date"] for p in points} == {"2026-09-01"}


def test_partial_failure_is_visible_and_other_sources_survive(scraper):
    point = scraper._point("vix", "2026-09-04", 18, "fred")
    with patch.object(scraper, "fetch_fred", return_value=[point]), \
         patch.object(scraper, "fetch_cboe", side_effect=ValueError("offline")), \
         patch.object(scraper, "fetch_aaii", return_value=[]), \
         patch.object(scraper, "fetch_naaim", return_value=[]), \
         patch.object(scraper, "fetch_cftc", return_value=[]):
        payload = scraper.scrape()
    assert payload["observations"] == [point]
    assert next(s for s in payload["sources"] if s["source"] == "cboe")["status"] == "error"


def test_merge_retains_dates_and_history_on_partial_failure(tmp_path):
    path = tmp_path / "market_inputs.json"
    old = {"indicator": "vix", "date": "2026-09-03", "value": 20}
    new = {**old, "date": "2026-09-04", "value": 18}
    save_market_inputs(path, {"observations": [old], "sources": []})
    save_market_inputs(path, {"observations": [new], "sources": []})
    save_market_inputs(path, {"observations": [], "sources": [{"source": "fred", "status": "error"}]})
    result = json.loads(path.read_text())
    assert result["observations"] == [old, new]
    assert result["sources"][0]["status"] == "error"


@pytest.mark.parametrize("value", [float("nan"), float("inf")])
def test_nonfinite_values_are_rejected(scraper, value):
    with pytest.raises(ValueError):
        scraper._point("vix", "2026-09-04", value, "fred")
