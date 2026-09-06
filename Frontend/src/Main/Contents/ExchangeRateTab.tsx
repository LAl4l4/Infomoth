import './IntroPage.css';
import RefreshStatus from './RefreshStatus';
import { useEffect, useMemo, useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import {
  fetchCurrencies,
  fetchExchangeRate,
  selectCurrencies,
  selectExchangeRates,
} from '../../Variable/dataCache';
import { selectIsLoggedIn } from '../../Variable/login';
import { pullGeneralSettings } from '../../API/settings';
import type { AppDispatch } from '../../customTypes';

export default function ExchangeRateTab() {
  const dispatch = useDispatch<AppDispatch>();
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const { data: currencies, loading: curLoading, error: curError } = useSelector(selectCurrencies);
  const { data: rates, loading: rateLoading, error: rateError, updatedAt } = useSelector(selectExchangeRates);

  const [base, setBase] = useState('USD');
  const [quote, setQuote] = useState('CNY');
  const [preferredPair, setPreferredPair] = useState({ base: 'USD', quote: 'CNY' });

  useEffect(() => {
    dispatch(fetchCurrencies());
  }, [dispatch]);

  useEffect(() => {
    if (!isLoggedIn) {
      setPreferredPair({ base: 'USD', quote: 'CNY' });
      return;
    }

    pullGeneralSettings()
      .then((settings) => {
        setPreferredPair({
          base: settings.defaultBaseCurrency || 'USD',
          quote: settings.defaultQuoteCurrency || 'CNY',
        });
      })
      .catch(() => {
        // Keep USD/CNY when account settings are unavailable.
      });
  }, [isLoggedIn]);

  useEffect(() => {
    if (currencies && currencies.length > 0) {
      const sorted = [...currencies].sort();
      const nextBase = sorted.includes(preferredPair.base)
        ? preferredPair.base
        : (sorted[0] || '');
      const nextQuote = sorted.includes(preferredPair.quote) && preferredPair.quote !== nextBase
        ? preferredPair.quote
        : (sorted.find((currency) => currency !== nextBase) || sorted[0] || '');
      setBase(nextBase);
      setQuote(nextQuote);
    }
  }, [currencies, preferredPair]);

  const canQuery = useMemo(
    () => Boolean(base) && Boolean(quote) && base !== quote,
    [base, quote]
  );

  useEffect(() => {
    if (!canQuery) return;
    dispatch(fetchExchangeRate({ base, quote }));
    const timer = window.setInterval(() => dispatch(fetchExchangeRate({ base, quote })), 5 * 60 * 1000);
    return () => window.clearInterval(timer);
  }, [base, quote, canQuery, dispatch]);

  const rate = canQuery ? rates[`${base}-${quote}`] : null;
  const loading = curLoading || rateLoading;
  const error = curError || rateError;

  function swap() {
    setBase(quote);
    setQuote(base);
  }

  return (
    <section className="glass-panel" aria-label="汇率查询">
      <p className="eyebrow">Exchange Rate</p>
      <h2 className="panel-title">汇率查询</h2>
      <p className="panel-lead">
        选择基础货币与目标货币，查询来源提供的汇率。
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
              {(currencies || []).map((cur) => (
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
              {(currencies || []).map((cur) => (
                <option key={`quote-${cur}`} value={cur}>{cur}</option>
              ))}
            </select>
          </label>
        </div>

        <div className="exchange-result">
          {loading && <p className="state-text">加载中…</p>}
          {!loading && error && <p className="exchange-error">{error}{rate != null ? '（显示上次结果）' : ''}</p>}
          {!loading && !error && !canQuery && (
            <p className="state-text">请选择两个不同的货币。</p>
          )}
          {!loading && canQuery && rate !== undefined && rate !== null && (
            <>
              <p className="exchange-rate-xl">
                1 {base} = {rate} {quote}
              </p>
              <p className="exchange-rate-sub">以来源最近发布的数据为准</p>
            </>
          )}
        </div>
      </div>
      <RefreshStatus updatedAt={updatedAt?.[`${base}-${quote}`]} onRefresh={() => {
        dispatch(fetchCurrencies({ force: true }));
        if (canQuery) dispatch(fetchExchangeRate({ base, quote, force: true }));
      }} />
    </section>
  );
}
