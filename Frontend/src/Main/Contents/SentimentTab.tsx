import './IntroPage.css';
import RefreshStatus from './RefreshStatus';
import { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { fetchSentimentScore, selectSentimentScore } from '../../Variable/dataCache';
import type { AppDispatch } from '../../customTypes';

function scorePresentation(score: number | null) {
  if (score === null) {
    return { color: '#ffffff', label: '暂无' };
  }
  if (score > 0.5) {
    return { color: '#16a34a', label: '偏乐观' };
  }
  if (score < -0.5) {
    return { color: '#FF4D4F', label: '偏悲观' };
  }
  return { color: '#9ca3af', label: '中性' };
}

function SentimentMetric({
  title,
  score,
  caption,
}: {
  title: string;
  score: number | null;
  caption: string;
}) {
  const { color, label } = scorePresentation(score);

  return (
    <div className="sentiment-metric">
      <p className="sentiment-metric-title">{title}</p>
      <div className="sentiment-score-xl" style={{ color }}>
        {score === null ? '—' : `${score > 0 ? '+' : ''}${score.toFixed(4)}`}
      </div>
      <span className="sentiment-pill">{label}</span>
      <p className="sentiment-metric-caption">{caption}</p>
    </div>
  );
}

export default function SentimentTab() {
  const dispatch = useDispatch<AppDispatch>();
  const { data: sentiment, loading, error, updatedAt } = useSelector(selectSentimentScore);

  useEffect(() => {
    dispatch(fetchSentimentScore());
    const timer = window.setInterval(() => dispatch(fetchSentimentScore()), 5 * 60 * 1000);
    return () => window.clearInterval(timer);
  }, [dispatch]);

  const readingScore = sentiment?.normalizedScore ?? null;

  const reading = readingScore === null
    ? '今天暂无可用数据。'
    : (readingScore > 0.5
        ? '市场整体情绪偏向乐观，新闻与舆情中正面信号占优。可继续关注后续走势，但注意短期过热风险。'
        : readingScore < -0.5
          ? '市场整体情绪偏悲观，负面信号占优。建议谨慎操作，关注潜在风险事件。'
          : '市场情绪整体处于中性区间，多空信号较为均衡，无明显方向性。');

  return (
    <section className="glass-panel" aria-label="市场情绪">
      <p className="eyebrow">Market Sentiment</p>
      <h2 className="panel-title">市场情绪指数</h2>
      <p className="panel-lead">
        标准化指数使用当前 FinBERT 原始分数、全历史滚动平均与滚动标准差计算；
        今日均值只聚合今天持久化的标准化指数。
      </p>

      <div className="sentiment-stage">
        {loading && <p className="state-text">加载中…</p>}
        {!loading && error && <p className="exchange-error">{error}{sentiment !== null ? '（显示上次结果）' : ''}</p>}
        {!loading && (!error || sentiment !== null) && (
          <div className="sentiment-grid">
            <SentimentMetric
              title="标准化情绪"
              score={sentiment?.normalizedScore ?? null}
              caption="(原始分数 − 滚动平均) ÷ 滚动标准差"
            />
            <SentimentMetric
              title="今日标准化均值"
              score={sentiment?.dailyAverage ?? null}
              caption="当天全部标准化样本的平均值"
            />
          </div>
        )}
      </div>

      {!loading && (!error || sentiment !== null) && readingScore !== null && (
        <p className="sentiment-reading">{reading}</p>
      )}
      <RefreshStatus updatedAt={updatedAt} onRefresh={() => { dispatch(fetchSentimentScore({ force: true })); }} />
    </section>
  );
}
