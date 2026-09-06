import './IntroPage.css';
import { useEffect, useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import {
  fetchSentimentScore,
  fetchExchangeRate,
  fetchUsStockIndices,
  selectSentimentScore,
  selectExchangeRates,
  selectUsStockIndices,
} from '../../Variable/dataCache';
import { selectIsLoggedIn } from '../../Variable/login';
import { pullGeneralSettings } from '../../API/settings';
import type { AppDispatch } from '../../customTypes';

function OverviewSentimentRow() {
  const dispatch = useDispatch<AppDispatch>();
  const { data: sentiment, loading, error } = useSelector(selectSentimentScore);
  const score = sentiment?.normalizedScore ?? null;

  useEffect(() => {
    dispatch(fetchSentimentScore());
    const timer = window.setInterval(() => dispatch(fetchSentimentScore()), 5 * 60 * 1000);
    return () => window.clearInterval(timer);
  }, [dispatch]);

  let valueText;
  if (loading) valueText = '加载中…';
  else if (error && score == null) valueText = '更新失败';
  else if (score == null) valueText = '今日暂无';
  else valueText = `${score > 0 ? '+' : ''}${score.toFixed(4)}`;

  return (
    <div className="snapshot-row">
      <span className="snapshot-label">标准化市场情绪</span>
      <span className={'snapshot-value' + (score == null && !loading && !error ? ' muted' : '')}>
        {valueText}{error && score != null ? '（更新失败，显示上次结果）' : ''}
      </span>
    </div>
  );
}

function OverviewRateRow() {
  const dispatch = useDispatch<AppDispatch>();
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const { data: rates, loading, error } = useSelector(selectExchangeRates);
  const [pair, setPair] = useState<{ base: string; quote: string } | null>(null);

  useEffect(() => {
    if (!isLoggedIn) {
      setPair({ base: 'USD', quote: 'CNY' });
      return;
    }

    let active = true;

    pullGeneralSettings()
      .then((settings) => {
        if (active) {
          setPair({
            base: settings.defaultBaseCurrency || 'USD',
            quote: settings.defaultQuoteCurrency || 'CNY',
          });
        }
      })
      .catch(() => {
        if (active) setPair({ base: 'USD', quote: 'CNY' });
      });

    return () => {
      active = false;
    };
  }, [isLoggedIn]);

  useEffect(() => {
    if (!pair) return;
    dispatch(fetchExchangeRate(pair));
    const timer = window.setInterval(() => dispatch(fetchExchangeRate(pair)), 5 * 60 * 1000);
    return () => window.clearInterval(timer);
  }, [dispatch, pair]);

  const rate = pair ? rates[`${pair.base}-${pair.quote}`] : undefined;

  let valueText;
  if (!pair || loading) valueText = '加载中…';
  else if (error && rate == null) valueText = '更新失败';
  else if (rate === undefined || rate === null) valueText = '尚无数据';
  else valueText = `1 ${pair.base} = ${rate} ${pair.quote}`;

  return (
    <div className="snapshot-row">
      <span className="snapshot-label">汇率速查{pair ? ` (${pair.base}/${pair.quote})` : ''}</span>
      <span className={'snapshot-value' + (!pair || rate === undefined || rate === null ? ' muted' : '')}>{valueText}{error && rate != null ? '（更新失败，显示上次结果）' : ''}</span>
    </div>
  );
}

function OverviewStockRow() {
  const dispatch = useDispatch<AppDispatch>();
  const { data: indices, loading, error } = useSelector(selectUsStockIndices);

  useEffect(() => {
    dispatch(fetchUsStockIndices());
    const timer = window.setInterval(() => dispatch(fetchUsStockIndices()), 5 * 60 * 1000);
    return () => window.clearInterval(timer);
  }, [dispatch]);

  const sp = indices && indices.find((i) => i.symbol === '^GSPC');

  let valueText;
  if (loading) valueText = '加载中…';
  else if (error && !sp) valueText = '更新失败';
  else if (!sp) valueText = '尚无数据';
  else {
    const sign = sp.change >= 0 ? '+' : '';
    valueText = `${sp.price.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} (${sign}${sp.changePercent.toFixed(2)}%)`;
  }

  return (
    <div className="snapshot-row">
      <span className="snapshot-label">S&P 500</span>
      <span className={'snapshot-value' + (!sp && !loading ? ' muted' : '')}>{valueText}{sp ? ` · ${sp.date}` : ''}{error && sp ? '（更新失败，显示上次结果）' : ''}</span>
    </div>
  );
}

export default function OverviewTab() {
  return (
    <section className="glass-panel" aria-label="概览">
      <p className="eyebrow">Overview</p>
      <h2 className="panel-title">资讯概览</h2>
      <p className="panel-lead">
        欢迎来到 InfoMoth。这里汇聚市场情绪、汇率与美股行情，
        在顶部的标签栏中切换即可深入查看每一项数据。
      </p>

      <div className="overview-snapshot">
        <OverviewSentimentRow />
        <OverviewRateRow />
        <OverviewStockRow />
      </div>

      <p className="overview-foot">
        数据定时更新，行情以来源交易日期为准，如需详细解读请切换到对应标签页。
      </p>
    </section>
  );
}
