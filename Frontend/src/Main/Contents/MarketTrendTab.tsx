import './IntroPage.css';
import { useEffect, useMemo, useState } from 'react';
import { pullMarketTrends } from '../../API/data';
import type { MarketTrendData, MarketTrendPoint } from '../../customTypes';

const CHART_WIDTH = 760;
const CHART_HEIGHT = 320;
const PLOT_LEFT = 48;
const PLOT_RIGHT = 18;
const PLOT_TOP = 22;
const PLOT_BOTTOM = 42;
export const MARKET_TREND_REFRESH_MS = 5 * 60 * 1000;

interface ChartSeries {
  key: string;
  label: string;
  color: string;
  values: Array<number | null>;
}

export function getStockChangePercents(points: MarketTrendPoint[], symbol: string): Array<number | null> {
  return points.map((point) => {
    const stock = point.stocks.find((item) => item.symbol === symbol);
    return stock?.changePercent ?? null;
  });
}

function isWeekend(date: string): boolean {
  const [year, month, day] = date.split('-').map(Number);
  const weekday = new Date(Date.UTC(year, month - 1, day)).getUTCDay();
  return weekday === 0 || weekday === 6;
}

function buildSegments(
  values: Array<number | null>,
  min: number,
  max: number,
  width: number,
  height: number
): string[][] {
  const segment: string[] = [];
  const xStep = values.length > 1 ? width / (values.length - 1) : width;

  values.forEach((value, index) => {
    if (value === null || !Number.isFinite(value)) {
      return;
    }
    const x = PLOT_LEFT + index * xStep;
    const y = PLOT_TOP + height - ((value - min) / (max - min)) * height;
    segment.push(`${x.toFixed(2)},${y.toFixed(2)}`);
  });

  return segment.length > 1 ? [segment] : [];
}

function TrendChart({ points, series }: { points: MarketTrendPoint[]; series: ChartSeries[] }) {
  const allValues = series.reduce<Array<number | null>>(
    (values, item) => values.concat(item.values),
    []
  )
    .filter((value): value is number => value !== null && Number.isFinite(value));
  if (allValues.length === 0) {
    return <p className="state-text">近 7 天暂无可绘制的数据。</p>;
  }

  const rawMin = Math.min(...allValues);
  const rawMax = Math.max(...allValues);
  const spread = rawMax - rawMin;
  const padding = spread === 0 ? 1 : spread * 0.12;
  const min = rawMin - padding;
  const max = rawMax + padding;
  const plotHeight = CHART_HEIGHT - PLOT_TOP - PLOT_BOTTOM;
  const plotWidth = CHART_WIDTH - PLOT_LEFT - PLOT_RIGHT;
  const gridValues = [0, 1, 2, 3, 4].map((step) => min + ((max - min) * step) / 4);

  return (
    <div className="trend-chart-wrap">
      <svg
        className="trend-chart"
        viewBox={`0 0 ${CHART_WIDTH} ${CHART_HEIGHT}`}
        role="img"
        aria-label="近七日市场情绪与美股每日涨跌幅"
      >
        {gridValues.map((value, index) => {
          const y = PLOT_TOP + plotHeight - (index / 4) * plotHeight;
          return (
            <g key={`grid-${index}`}>
              <line
                className="trend-grid-line"
                x1={PLOT_LEFT}
                x2={CHART_WIDTH - PLOT_RIGHT}
                y1={y}
                y2={y}
              />
              <text className="trend-axis-label" x={PLOT_LEFT - 8} y={y + 4} textAnchor="end">
                {value.toFixed(1)}
              </text>
            </g>
          );
        })}

        {points.map((point, index) => {
          const x = PLOT_LEFT + (points.length > 1 ? (index * plotWidth) / (points.length - 1) : 0);
          return (
            <text
              className="trend-axis-label"
              key={point.date}
              x={x}
              textAnchor="middle"
            >
              <tspan x={x} y={CHART_HEIGHT - 16}>{point.date.slice(5)}</tspan>
              {isWeekend(point.date) && (
                <tspan className="trend-weekend-label" x={x} y={CHART_HEIGHT - 4}>Weekend</tspan>
              )}
            </text>
          );
        })}

        {series.map((item) => buildSegments(item.values, min, max, plotWidth, plotHeight).map((segment, index) => (
          <polyline
            className="trend-line"
            key={`${item.key}-${index}`}
            points={segment.join(' ')}
            stroke={item.color}
          />
        )))}
      </svg>

      <div className="trend-legend">
        {series.map((item) => (
          <span className="trend-legend-item" key={item.key}>
            <i className="trend-dot" style={{ backgroundColor: item.color }} />
            {item.label}
          </span>
        ))}
      </div>
    </div>
  );
}

export default function MarketTrendTab() {
  const [data, setData] = useState<MarketTrendData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    let hasData = false;
    let refreshing = false;

    const refresh = async () => {
      if (refreshing) return;
      refreshing = true;
      try {
        const result = await pullMarketTrends();
        if (active) {
          hasData = true;
          setData(result);
          setError(null);
        }
      } catch (reason) {
        if (active && !hasData) {
          setError(reason instanceof Error ? reason.message : '加载失败');
        }
      } finally {
        refreshing = false;
        if (active) setLoading(false);
      }
    };

    void refresh();
    const refreshTimer = window.setInterval(() => {
      void refresh();
    }, MARKET_TREND_REFRESH_MS);

    return () => {
      active = false;
      window.clearInterval(refreshTimer);
    };
  }, []);

  const series = useMemo<ChartSeries[]>(() => {
    if (!data) return [];
    const stockMeta = new Map<string, string>();
    data.points.forEach((point) => point.stocks.forEach((stock) => {
      stockMeta.set(stock.symbol, stock.name);
    }));

    const stockColors = ['#FBBF24', '#60A5FA', '#A78BFA', '#34D399', '#FB7185', '#22D3EE'];
    const stockSeries = Array.from(stockMeta.entries()).map(([symbol, name], index) => ({
      key: symbol,
      label: name || symbol,
      color: stockColors[index % stockColors.length],
      values: getStockChangePercents(data.points, symbol),
    }));

    return [
      {
        key: 'sentiment',
        label: '市场情绪 × 10',
        color: '#FFFFFF',
        values: data.points.map((point) => point.sentiment === null ? null : point.sentiment * 10),
      },
      ...stockSeries,
    ];
  }, [data]);

  return (
    <section className="glass-panel" aria-label="市场走势">
      <p className="eyebrow">Market Trend</p>
      <h2 className="panel-title">7日市场走势</h2>
      <p className="panel-lead">
        对比近 7 个自然日的市场情绪与 Yahoo Finance 美股涨跌幅；交易时段使用 5 分钟行情并自动刷新，corr 使用持久化的交易日数据。周末不生成行情点，曲线连接前后交易日。
      </p>

      {loading && <p className="state-text">加载中…</p>}
      {!loading && error && <p className="exchange-error">{error}</p>}
      {!loading && !error && data && (
        <>
          <TrendChart points={data.points} series={series} />
          <div className="trend-correlation-heading">
            <h3>情绪与美股相关性</h3>
            <span>corr = cov / (sd(sentiment) × sd(stock))</span>
          </div>
          <div className="trend-correlation-grid">
            {data.correlations.length === 0 && <p className="state-text">暂无足够的重合数据。</p>}
            {data.correlations.map((item) => (
              <div className="trend-correlation-card" key={item.symbol}>
                <div>
                  <strong>{item.name || item.symbol}</strong>
                  <span>{item.symbol} · {item.sampleSize} 个交易日样本</span>
                </div>
                <b>corr = {item.corr === null ? '暂无' : item.corr.toFixed(4)}</b>
              </div>
            ))}
          </div>
        </>
      )}
    </section>
  );
}
