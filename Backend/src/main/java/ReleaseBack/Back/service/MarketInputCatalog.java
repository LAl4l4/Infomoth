package ReleaseBack.Back.service;

import java.util.List;

/** Fixed v1 feature scales are transparent mock assumptions, not fitted coefficients. */
final class MarketInputCatalog {
    private MarketInputCatalog() {}

    record Spec(String code, String label, String unit, String source, String url,
                int maxAgeDays, double center, double scale, double weight) {
        String transform() {
            return weight == 0 ? "Context only" : "clip((x - " + center + ") / " + scale + ", -1, 1)";
        }
    }

    static final List<Spec> SPECS = List.of(
        new Spec("tech_sentiment", "科技新闻情绪", "z-score", "news", "", 3, 0, 3, .10),
        new Spec("politics_sentiment", "政治新闻情绪", "z-score", "news", "", 3, 0, 3, .10),
        new Spec("vix", "VIX 30天预期波动率", "%", "fred", "https://fred.stlouisfed.org/series/VIXCLS", 5, 20, -15, .15),
        new Spec("vix3m", "VIX 3个月预期波动率", "%", "fred", "https://fred.stlouisfed.org/series/VXVCLS", 5, 0, 1, 0),
        new Spec("put_call", "股票期权 Put/Call", "ratio", "cboe", "https://www.cboe.com/markets/us/options/market-statistics/daily", 5, .7, -.4, .10),
        new Spec("aaii_bull", "AAII 看多比例", "%", "aaii", "https://www.aaii.com/sentimentsurvey", 14, 0, 1, 0),
        new Spec("aaii_bear", "AAII 看空比例", "%", "aaii", "https://www.aaii.com/sentimentsurvey", 14, 0, 1, 0),
        new Spec("aaii_spread", "AAII 多空差", "pp", "aaii", "https://www.aaii.com/sentimentsurvey", 14, 0, 40, .10),
        new Spec("naaim", "NAAIM 股票敞口（公开延迟）", "%", "naaim", "https://naaim.org/programs/naaim-exposure-index/", 14, 50, 50, .10),
        new Spec("cot_long", "E-mini 杠杆基金多仓", "contracts", "cftc", "https://www.cftc.gov/MarketReports/CommitmentsofTraders/index.htm", 14, 0, 1, 0),
        new Spec("cot_short", "E-mini 杠杆基金空仓", "contracts", "cftc", "https://www.cftc.gov/MarketReports/CommitmentsofTraders/index.htm", 14, 0, 1, 0),
        new Spec("cot_oi", "E-mini 未平仓量", "contracts", "cftc", "https://www.cftc.gov/MarketReports/CommitmentsofTraders/index.htm", 14, 0, 1, 0),
        new Spec("cot_net", "杠杆基金净仓 / 未平仓量", "%", "cftc", "https://www.cftc.gov/MarketReports/CommitmentsofTraders/index.htm", 14, 0, 30, .10),
        new Spec("sp500", "标普500收盘价", "points", "fred", "https://fred.stlouisfed.org/series/SP500", 5, 0, 1, 0),
        new Spec("momentum5", "标普500五交易日动量", "%", "fred", "https://fred.stlouisfed.org/series/SP500", 5, 0, 5, .10),
        new Spec("hy_spread", "美国高收益债信用利差", "pp", "fred", "https://fred.stlouisfed.org/series/BAMLH0A0HYM2", 5, 4, -3, .10),
        new Spec("treasury10y", "美国10年期国债收益率", "%", "fred", "https://fred.stlouisfed.org/series/DGS10", 5, 4, -2, .05)
    );

    static boolean accepts(String code, String source) {
        return SPECS.stream().anyMatch(s -> s.code().equals(code) && s.source().equals(source))
            && !List.of("tech_sentiment", "politics_sentiment", "momentum5", "aaii_spread", "cot_net").contains(code);
    }
}
