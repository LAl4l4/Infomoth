import './IntroPage.css';
import './MarketSignalTab.css';
import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { DATA_REFRESH_MS, fetchMarketSignal, selectMarketSignal } from '../../Variable/dataCache';
import type { AppDispatch, MarketSignalInput } from '../../customTypes';
import RefreshStatus from './RefreshStatus';

const statusLabels = { available: '可用', stale: '已过期 · 不参与', missing: '缺失 · 不参与' };
const format = (value: number | null) => value === null ? '—' : value.toLocaleString('en-US', { maximumFractionDigits: 3 });

function InputHistory({ input }: { input: MarketSignalInput }) {
  const points = input.history;
  if (points.length < 2) return <p className="state-text">{input.label}：尚无足够历史记录。</p>;
  const values = points.map(p => p.value);
  const min = Math.min(...values), max = Math.max(...values);
  const start = Date.parse(points[0].date), end = Date.parse(points[points.length - 1].date);
  // Position by actual observation date; never synthesize weekends or missing values.
  const positions = points.map(p => `${40 + 640 * (Date.parse(p.date) - start) / Math.max(1, end - start)},${150 - 120 * (p.value - min) / (max - min || 1)}`);
  return <figure className="signal-history">
    <figcaption>{input.label} · 最近 {points.length} 条原始记录 · {input.unit}</figcaption>
    <svg viewBox="0 0 720 190" role="img" aria-label={`${input.label}历史数据`}>
      <text x="8" y="25">{format(max)}</text><text x="8" y="165">{format(min)}</text>
      <line x1="40" y1="150" x2="680" y2="150" stroke="currentColor" opacity=".2" />
      <polyline points={positions.join(' ')} fill="none" stroke="#67e8f9" strokeWidth="2.5" />
      <text x="40" y="185">{points[0].date}</text>
      <text x="680" y="185" textAnchor="end">{points[points.length - 1].date}</text>
    </svg>
    <p className="state-text">连线仅连接已有观测点，不代表缺失日期存在数据。</p>
  </figure>;
}

export default function MarketSignalTab() {
  const dispatch = useDispatch<AppDispatch>();
  const { data, loading, error, updatedAt } = useSelector(selectMarketSignal);
  const [selected, setSelected] = useState('vix');
  useEffect(() => {
    dispatch(fetchMarketSignal());
    const timer = window.setInterval(() => dispatch(fetchMarketSignal()), DATA_REFRESH_MS);
    return () => window.clearInterval(timer);
  }, [dispatch]);
  const prediction = data?.prediction;
  const selectedInput = prediction?.inputs.find(input => input.indicator === selected);
  return <section className="glass-panel signal-panel" aria-label="情绪预测">
    <p className="eyebrow">Sentiment & Market Inputs</p>
    <h2 className="panel-title">情绪预测 <span className="signal-badge">MOCK</span></h2>
    <p className="panel-lead">观察新闻情绪、期权、投资者仓位与宏观数据。线性加权仅用于演示，分数不是上涨概率或预期收益率。</p>
    {loading && <p className="state-text" role="status">{data ? '正在刷新…' : '加载中…'}</p>}
    {error && <p className="exchange-error" role="alert">{error}{data ? '（显示上次结果）' : ''}</p>}
    {prediction && <>
      <div className="signal-summary">
        <div>
          <span>{prediction.target} · 倾向分数</span>
          <strong>{prediction.score === null ? '数据不足' : format(prediction.score)}</strong>
          <span>{prediction.score === null ? '有效权重不足 50%' : prediction.score > 0 ? '偏多' : prediction.score < 0 ? '偏空' : '中性'} · 范围 −100～100</span>
        </div>
        <div><span>有效权重覆盖</span><strong>{Math.round(prediction.coverage * 100)}%</strong><span>{prediction.horizon}</span></div>
      </div>
      <p className="state-text">仅纳入日期有效的输入；每日指标有效期为 5 个自然日，周度指标为 14 天，新闻为 3 天。来源最近抓取失败时，仍可能保留有效期内的旧观测。</p>
      <div className="signal-sources" aria-label="采集来源状态">
        {data.sources.length === 0 && <span>尚无采集记录，请先运行指标爬虫并等待后端入库。</span>}
        {data.sources.map(source => <div key={source.source}>
          <b>{source.source.toUpperCase()}</b> · {source.status === 'ok' ? '抓取成功' : source.status === 'delayed' ? '公开数据延迟三个月' : source.status === 'partial' ? '部分指标未获取，保留旧数据' : '抓取失败，保留旧数据'}
          <small>最近尝试：{new Date(source.attemptedAt).toLocaleString()}</small>
        </div>)}
      </div>
      <div className="signal-table-wrap">
        <table className="signal-table">
          <caption>指标值、来源日期和模型贡献</caption>
          <thead><tr><th>输入</th><th>原始值</th><th>观测日期</th><th>状态</th><th>固定权重</th><th>有效权重</th><th>贡献分</th></tr></thead>
          <tbody>{prediction.inputs.map(input => <tr key={input.indicator}>
            <th scope="row"><button type="button" onClick={() => setSelected(input.indicator)}>{input.label}</button><small>{input.unit}</small></th>
            <td>{format(input.value)}</td><td>{input.date ?? '—'}</td>
            <td>{statusLabels[input.status]}{input.weight === 0 && ' · 仅展示'}</td>
            <td>{Math.round(input.weight * 100)}%</td><td>{Math.round(input.effectiveWeight * 100)}%</td>
            <td>{format(input.contribution)}</td>
          </tr>)}</tbody>
        </table>
      </div>
      {selectedInput && <>
        <label className="signal-select">查看指标历史 <select value={selected} onChange={e => setSelected(e.target.value)}>
          {prediction.inputs.map(input => <option key={input.indicator} value={input.indicator}>{input.label}</option>)}
        </select></label>
        <InputHistory input={selectedInput} />
        <div className="signal-detail">
          <p>标准化：<code>{selectedInput.transform}</code> · 当前值 {format(selectedInput.normalized)}</p>
          <p>来源：{selectedInput.sourceUrl ? <a href={selectedInput.sourceUrl} target="_blank" rel="noreferrer">{selectedInput.source.toUpperCase()}</a> : '已入库的 FinBERT 新闻情绪'}</p>
          <p>抓取时间：{selectedInput.fetchedAt === null ? '未提供' : new Date(selectedInput.fetchedAt).toLocaleString()}</p>
          <p>当前值首次入库：{selectedInput.availableAt === null ? '旧数据未记录' : new Date(selectedInput.availableAt).toLocaleString()}</p>
        </div>
      </>}
      <details className="signal-method"><summary>查看 mock 计算规则</summary>
        <p>分数 = 100 × Σ(有效权重 × 标准化输入)。有效权重 = 固定权重 ÷ 可用固定权重之和。每个输入先按固定中心与尺度转换，并截断至 −1～1。</p>
        <p>AAII 多空差 = 看多比例 − 看空比例；CFTC 净仓占比 = (多仓 − 空仓) / 未平仓量 × 100；五交易日动量 = (最新收盘价 / 五条记录前收盘价 − 1) × 100。</p>
        <p>权重与尺度均为人工设定，未经回测校准。过期和缺失数据不贡献分数；公开 NAAIM 延迟数据仍可查看历史。历史回补的入库时间不等于原始发布时间。</p>
        <p>{prediction.modelVersion} · 计算时间 {new Date(prediction.generatedAt).toLocaleString()}</p>
      </details>
    </>}
    <RefreshStatus updatedAt={updatedAt} onRefresh={() => { dispatch(fetchMarketSignal({ force: true })); }} />
  </section>;
}
