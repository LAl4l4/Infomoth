import './IntroPage.css';
import { useEffect, useMemo, useState } from "react";
import { pullCurrencies, pullExchangeRate, pullPopularAISkills, pullSentimentScore } from "../../API/data";


export default function Intro({ pagenum }) {
    let containerClass = 'Intromain';

    if (pagenum > 0) {
        containerClass = 'Intromain';
    } else if (pagenum === 0) {
        containerClass = 'Intromainup';
    }

    return (
        <div className={containerClass}>
            <div className="intro-grid">
                <div className="intro-left">
                    <div className="card card-top">
                        <h3 className="card-title">概览</h3>
                        <p className="card-text">欢迎来到发布博客主页，这里展示近期动态与导航。</p>
                    </div>

                    <div className="card-2x2">
                        <div className="card card-small">
                            <SentimentCard />
                        </div>
                        <div className="card card-small">
                            <AISkillsCard />
                        </div>
                        <div className="card card-small">
                            <h4 className="card-title">快速开始</h4>
                            <p className="card-text">新手指南与上手教程。</p>
                        </div>
                        <div className="card card-small">
                            <h4 className="card-title">资源链接</h4>
                            <p className="card-text">文档、示例与工具集合。</p>
                        </div>
                    </div>
                </div>

                <div className="intro-right">
                    <div className="card card-large">
                        <h3 className="card-title">汇率查询</h3>
                        <CurrencySelector />
                    </div>
                </div>
            </div>
        </div>
    );
}

function SentimentCard() {
    const [score, setScore] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        let mounted = true;
        pullSentimentScore()
            .then((value) => {
                if (!mounted) return;
                setScore(value);
            })
            .catch((e) => {
                if (!mounted) return;
                setError(e.message || "情绪数据加载失败");
            })
            .finally(() => {
                if (!mounted) return;
                setLoading(false);
            });

        return () => {
            mounted = false;
        };
    }, []);

    const sentimentColor = score !== null
        ? (score > 0.05 ? "var(--sentiment-positive, #16a34a)"
           : score < -0.05 ? "var(--sentiment-negative, #dc2626)"
           : "var(--sentiment-neutral, #6b7280)")
        : undefined;

    return (
        <>
            <h4 className="card-title">市场情绪</h4>
            {loading && <p className="card-text">加载中...</p>}
            {!loading && error && <p className="currency-error">{error}</p>}
            {!loading && !error && score === null && (
                <p className="card-text">今天暂无可用数据。</p>
            )}
            {!loading && !error && score !== null && (
                <div className="sentiment-widget">
                    <span className="sentiment-score" style={{ color: sentimentColor }}>
                        {score > 0 ? "+" : ""}{score.toFixed(4)}
                    </span>
                    <span className="sentiment-label">
                        {score > 0.05 ? "偏乐观" : score < -0.05 ? "偏悲观" : "中性"}
                    </span>
                </div>
            )}
        </>
    );
}

function AISkillsCard() {
    const [skills, setSkills] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        let mounted = true;
        pullPopularAISkills()
            .then((items) => {
                if (!mounted) return;
                setSkills(items.slice(0, 3));
            })
            .catch((e) => {
                if (!mounted) return;
                setError(e.message || "AI 技能数据加载失败");
            })
            .finally(() => {
                if (!mounted) return;
                setLoading(false);
            });

        return () => {
            mounted = false;
        };
    }, []);

    return (
        <>
            <h4 className="card-title">AI 热门技能</h4>
            {loading && <p className="card-text">加载中...</p>}
            {!loading && error && <p className="currency-error">{error}</p>}
            {!loading && !error && skills.length === 0 && (
                <p className="card-text">今天暂无可用数据。</p>
            )}
            {!loading && !error && skills.length > 0 && (
                <ol className="ai-skill-list">
                    {skills.map((item) => (
                        <li key={`${item.rank}-${item.skill}`}>
                            <span className="ai-skill-name">{item.skill}</span>
                            <span className="ai-skill-meta">{item.mentions} mentions</span>
                        </li>
                    ))}
                </ol>
            )}
        </>
    );
}

function CurrencySelector() {
    const [currencies, setCurrencies] = useState([]);
    const [base, setBase] = useState("USD");
    const [quote, setQuote] = useState("CNY");
    const [rate, setRate] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    useEffect(() => {
        let mounted = true;
        pullCurrencies()
            .then((list) => {
                if (!mounted) return;
                const sorted = [...list].sort();
                setCurrencies(sorted);
                setBase((prev) => (sorted.includes(prev) ? prev : (sorted[0] || "")));
                setQuote((prev) => (sorted.includes(prev) ? prev : (sorted[1] || sorted[0] || "")));
            })
            .catch((e) => {
                if (!mounted) return;
                setError(e.message || "货币列表加载失败");
            });
        return () => {
            mounted = false;
        };
    }, []);

    const canQuery = useMemo(
        () => Boolean(base) && Boolean(quote) && base !== quote,
        [base, quote]
    );

    useEffect(() => {
        if (!canQuery) {
            setRate(null);
            return;
        }
        let mounted = true;
        setLoading(true);
        setError("");
        pullExchangeRate(base, quote)
            .then((value) => {
                if (!mounted) return;
                setRate(value);
            })
            .catch((e) => {
                if (!mounted) return;
                setRate(null);
                setError(e.message || "汇率加载失败");
            })
            .finally(() => {
                if (!mounted) return;
                setLoading(false);
            });
        return () => {
            mounted = false;
        };
    }, [base, quote, canQuery]);

    return (
        <div className="currency-widget">
            <div className="currency-row">
                <label className="currency-field">
                    <span className="currency-label">基础货币</span>
                    <select value={base} onChange={(e) => setBase(e.target.value)}>
                        {currencies.map((cur) => (
                            <option key={`base-${cur}`} value={cur}>
                                {cur}
                            </option>
                        ))}
                    </select>
                </label>

                <label className="currency-field">
                    <span className="currency-label">目标货币</span>
                    <select value={quote} onChange={(e) => setQuote(e.target.value)}>
                        {currencies.map((cur) => (
                            <option key={`quote-${cur}`} value={cur}>
                                {cur}
                            </option>
                        ))}
                    </select>
                </label>
            </div>

            <div className="currency-result">
                {loading && <p className="card-text">加载中...</p>}
                {!loading && error && <p className="currency-error">{error}</p>}
                {!loading && !error && rate !== null && (
                    <p className="currency-rate">
                        1 {base} = {rate} {quote}
                    </p>
                )}
                {!loading && !error && !canQuery && (
                    <p className="card-text">请选择两个不同的货币。</p>
                )}
            </div>
        </div>
    );
}
