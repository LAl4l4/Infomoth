import './IntroPage.css';
import { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import {
  fetchUsStockIndices,
  selectUsStockIndices,
} from '../../Variable/dataCache';
import type { AppDispatch } from '../../customTypes';
import type { UsStockIndex } from '../../customTypes';

function formatPrice(v: number): string {
  return v >= 1000 ? v.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : v.toFixed(2);
}

function formatChange(v: number): string {
  const sign = v >= 0 ? '+' : '';
  return sign + v.toFixed(2);
}

function formatPercent(v: number): string {
  const sign = v >= 0 ? '+' : '';
  return sign + v.toFixed(2) + '%';
}

export default function UsStockTab() {
  const dispatch = useDispatch<AppDispatch>();
  const { data: indices, loading, error } = useSelector(selectUsStockIndices);

  useEffect(() => {
    dispatch(fetchUsStockIndices());
  }, [dispatch]);

  return (
    <section className="glass-panel" aria-label="美股指数">
      <p className="eyebrow">US Stock Indices</p>
      <h2 className="panel-title">美股主要指数</h2>
      <p className="panel-lead">
        追踪标普500、道琼斯、纳斯达克及罗素2000的最新行情。
      </p>

      {loading && <p className="state-text">加载中…</p>}
      {!loading && error && <p className="exchange-error">{error}</p>}
      {!loading && !error && indices && (
        <div className="stock-grid">
          {indices.map((idx: UsStockIndex) => {
            const up = idx.change >= 0;
            return (
              <div className={'stock-card' + (up ? ' stock-up' : ' stock-down')} key={idx.symbol}>
                <p className="stock-name">{idx.name}</p>
                <p className="stock-price">{formatPrice(idx.price)}</p>
                <div className="stock-change-row">
                  <span className="stock-change">{formatChange(idx.change)}</span>
                  <span className="stock-change-pct">{formatPercent(idx.changePercent)}</span>
                </div>
                <p className="stock-date">{idx.date}</p>
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
}
