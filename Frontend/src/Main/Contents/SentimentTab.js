import './IntroPage.css';
import { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { fetchSentimentScore, selectSentimentScore } from '../../Variable/dataCache';

export default function SentimentTab() {
  const dispatch = useDispatch();
  const { data: score, loading, error } = useSelector(selectSentimentScore);

  useEffect(() => {
    dispatch(fetchSentimentScore());
  }, [dispatch]);

  const color = score !== null
    ? (score > 0.05 ? '#16a34a'
       : score < -0.05 ? '#FF4D4F'
       : '#6b7280')
    : '#ffffff';

  const label = score === null
    ? '—'
    : (score > 0.05 ? '偏乐观' : score < -0.05 ? '偏悲观' : '中性');

  const reading = score === null
    ? '今天暂无可用数据。'
    : (score > 0.05
        ? '市场整体情绪偏向乐观，新闻与舆情中正面信号占优。可继续关注后续走势，但注意短期过热风险。'
        : score < -0.05
          ? '市场整体情绪偏悲观，负面信号占优。建议谨慎操作，关注潜在风险事件。'
          : '市场情绪整体处于中性区间，多空信号较为均衡，无明显方向性。');

  return (
    <section className="glass-panel" aria-label="市场情绪">
      <p className="eyebrow">Market Sentiment</p>
      <h2 className="panel-title">市场情绪指数</h2>
      <p className="panel-lead">
        基于 FinBERT 对当日新闻语料的情感分析得出的综合情绪分数。
      </p>

      <div className="sentiment-stage">
        {loading && <p className="state-text">加载中…</p>}
        {!loading && error && <p className="exchange-error">{error}</p>}
        {!loading && !error && score === null && <p className="state-text">今天暂无可用数据。</p>}
        {!loading && !error && score !== null && (
          <>
            <div className="sentiment-score-xl" style={{ color }}>
              {score > 0 ? '+' : ''}{score.toFixed(4)}
            </div>
            <span className="sentiment-pill">{label}</span>
          </>
        )}
      </div>

      {!loading && !error && score !== null && (
        <p className="sentiment-reading">{reading}</p>
      )}
    </section>
  );
}
