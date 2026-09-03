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
  }, [dispatch]);

  let valueText;
  if (loading) valueText = '加载中…';
  else if (error) valueText = '暂无';
  else if (score == null) valueText = '今日暂无';
  else valueText = `${score > 0 ? '+' : ''}${score.toFixed(4)}`;

  return (
    <div className="snapshot-row">
      <span className="snapshot-label">归一化市场情绪</span>
      <span className={'snapshot-value' + (score == null && !loading && !error ? ' muted' : '')}>
        {valueText}
      </span>
    </div>
  );
}

function OverviewRateRow() {
  const dispatch = useDispatch<AppDispatch>();
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const { data: rates, loading } = useSelector(selectExchangeRates);
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
    if (pair) dispatch(fetchExchangeRate(pair));
  }, [dispatch, pair]);

  const rate = pair ? rates[`${pair.base}-${pair.quote}`] : undefined;

  let valueText;
  if (!pair || loading) valueText = '加载中…';
  else if (rate === undefined || rate === null) valueText = '今日暂无';
  else valueText = `1 ${pair.base} = ${rate} ${pair.quote}`;

  return (
    <div className="snapshot-row">
      <span className="snapshot-label">汇率速查{pair ? ` (${pair.base}/${pair.quote})` : ''}</span>
      <span className={'snapshot-value' + (!pair || rate === undefined || rate === null ? ' muted' : '')}>{valueText}</span>
    </div>
  );
}

function OverviewStockRow() {
  const dispatch = useDispatch<AppDispatch>();
  const { data: indices, loading } = useSelector(selectUsStockIndices);

  useEffect(() => {
    dispatch(fetchUsStockIndices());
  }, [dispatch]);

  const sp = indices && indices.find((i) => i.symbol === '^GSPC');

  let valueText;
  if (loading) valueText = '加载中…';
  else if (!sp) valueText = '今日暂无';
  else {
    const sign = sp.change >= 0 ? '+' : '';
    valueText = `${sp.price.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} (${sign}${sp.changePercent.toFixed(2)}%)`;
  }

  return (
    <div className="snapshot-row">
      <span className="snapshot-label">S&P 500</span>
      <span className={'snapshot-value' + (!sp && !loading ? ' muted' : '')}>{valueText}</span>
    </div>
  );
}

export default function OverviewTab() {
  return (
    <section className="glass-panel" aria-label="概览">
      <p className="eyebrow">Overview</p>
      <h2 className="panel-title">今日资讯概览</h2>
      <p className="panel-lead">
        欢迎来到 InfoMoth。这里汇聚当日市场情绪、汇率与美股行情，
        在顶部的标签栏中切换即可深入查看每一项数据。
      </p>

      <div className="overview-snapshot">
        <OverviewSentimentRow />
        <OverviewRateRow />
        <OverviewStockRow />
      </div>

      <p className="overview-foot">
        数据由后端定时抓取与更新，如需详细解读请切换到对应标签页。
      </p>
    </section>
  );
}
