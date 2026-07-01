import './IntroPage.css';
import { useEffect, useMemo, useState } from 'react';
import { pullCurrencies, pullExchangeRate } from '../../API/data';

export default function ExchangeRateTab() {
  const [currencies, setCurrencies] = useState([]);
  const [base, setBase] = useState('USD');
  const [quote, setQuote] = useState('CNY');
  const [rate, setRate] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    let mounted = true;
    pullCurrencies()
      .then((list) => {
        if (!mounted) return;
        const sorted = [...list].sort();
        setCurrencies(sorted);
        setBase((p) => (sorted.includes(p) ? p : (sorted[0] || '')));
        setQuote((p) => (sorted.includes(p) ? p : (sorted[1] || sorted[0] || '')));
      })
      .catch((e) => mounted && setError(e.message || '货币列表加载失败'));
    return () => { mounted = false; };
  }, []);

  const canQuery = useMemo(
    () => Boolean(base) && Boolean(quote) && base !== quote,
    [base, quote]
  );

  useEffect(() => {
    if (!canQuery) { setRate(null); return; }
    let mounted = true;
    setLoading(true);
    setError('');
    pullExchangeRate(base, quote)
      .then((v) => mounted && setRate(v))
      .catch((e) => {
        if (!mounted) return;
        setRate(null);
        setError(e.message || '汇率加载失败');
      })
      .finally(() => mounted && setLoading(false));
    return () => { mounted = false; };
  }, [base, quote, canQuery]);

  function swap() {
    setBase(quote);
    setQuote(base);
  }

  return (
    <section className="glass-panel" aria-label="汇率查询">
      <p className="eyebrow">Exchange Rate</p>
      <h2 className="panel-title">汇率查询</h2>
      <p className="panel-lead">
        选择基础货币与目标货币，实时换算今日汇率。
      </p>

      <div className="exchange-widget">
        <div className="exchange-pair">
          <label className="exchange-field">
            <span className="field-label">基础货币</span>
            <select
              className="exchange-select"
              value={base}
              onChange={(e) => setBase(e.target.value)}
            >
              {currencies.map((cur) => (
                <option key={`base-${cur}`} value={cur}>{cur}</option>
              ))}
            </select>
          </label>

          <span className="exchange-swap" aria-hidden onClick={swap}>⇄</span>

          <label className="exchange-field">
            <span className="field-label">目标货币</span>
            <select
              className="exchange-select"
              value={quote}
              onChange={(e) => setQuote(e.target.value)}
            >
              {currencies.map((cur) => (
                <option key={`quote-${cur}`} value={cur}>{cur}</option>
              ))}
            </select>
          </label>
        </div>

        <div className="exchange-result">
          {loading && <p className="state-text">加载中…</p>}
          {!loading && error && <p className="exchange-error">{error}</p>}
          {!loading && !error && !canQuery && (
            <p className="state-text">请选择两个不同的货币。</p>
          )}
          {!loading && !error && canQuery && rate !== null && (
            <>
              <p className="exchange-rate-xl">
                1 {base} = {rate} {quote}
              </p>
              <p className="exchange-rate-sub">基于今日最新数据</p>
            </>
          )}
        </div>
      </div>
    </section>
  );
}
