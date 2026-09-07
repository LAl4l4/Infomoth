import { act, fireEvent, screen, waitFor } from '@testing-library/react';
import MarketSignalTab from '../../Main/Contents/MarketSignalTab';
import { pullMarketSignal } from '../../API/data';
import { renderWithProviders } from '../testUtils';
import type { MarketSignalData } from '../../customTypes';
import { DATA_REFRESH_MS, fetchMarketSignal } from '../../Variable/dataCache';

jest.mock('../../API/data', () => ({ pullMarketSignal: jest.fn() }));
const fetchSignal = pullMarketSignal as jest.Mock;
const payload: MarketSignalData = {
  prediction: { modelVersion: 'linear-mock-v1', target: 'S&P 500', horizon: '未来5个交易日（未校准）',
    generatedAt: 1000, score: 25, coverage: .75, status: 'mock', inputs: [{
      indicator: 'vix', label: 'VIX 30天预期波动率', unit: '%', source: 'fred', sourceUrl: 'https://fred.stlouisfed.org/series/VIXCLS',
      date: '2026-09-04', value: 18, fetchedAt: 1000, availableAt: 1000, status: 'available',
      transform: 'clip((x - 20) / -15, -1, 1)', normalized: .133, weight: .15, effectiveWeight: .2, contribution: 2.66,
      history: [{ date: '2026-09-03', value: 19 }, { date: '2026-09-04', value: 18 }],
    }] }, sources: [{ source: 'naaim', status: 'delayed', message: '', attemptedAt: 1000 }],
};

beforeEach(() => { jest.clearAllMocks(); fetchSignal.mockResolvedValue(payload); });
afterEach(() => { jest.useRealTimers(); });

it('shows the mock score, coverage, actual dates, provenance and contributions', async () => {
  renderWithProviders(<MarketSignalTab />);
  expect(await screen.findByText('75%')).toBeInTheDocument();
  expect(screen.getByText('MOCK')).toBeInTheDocument();
  expect(screen.getByText('2.66')).toBeInTheDocument();
  expect(screen.getByRole('img', { name: 'VIX 30天预期波动率历史数据' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'FRED' })).toHaveAttribute('href', 'https://fred.stlouisfed.org/series/VIXCLS');
  expect(screen.getByText(/公开数据延迟三个月/)).toBeInTheDocument();
});

it('keeps cached data and its timestamp after failed refresh and allows retry', async () => {
  const { store } = renderWithProviders(<MarketSignalTab />);
  await screen.findByText('75%');
  const updatedAt = store.getState().dataCache.marketSignal.updatedAt;
  fetchSignal.mockRejectedValueOnce(new Error('offline'));
  fireEvent.click(screen.getByRole('button', { name: '刷新数据' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('显示上次结果');
  expect(screen.getByText('75%')).toBeInTheDocument();
  expect(store.getState().dataCache.marketSignal.updatedAt).toBe(updatedAt);
  fireEvent.click(screen.getByRole('button', { name: '刷新数据' }));
  await waitFor(() => expect(screen.queryByRole('alert')).not.toBeInTheDocument());
});

it('shows insufficient data rather than a fabricated neutral score', async () => {
  fetchSignal.mockResolvedValue({ ...payload, prediction: { ...payload.prediction, score: null, coverage: 0,
    status: 'insufficient_data', inputs: [] } });
  renderWithProviders(<MarketSignalTab />);
  expect(await screen.findByText('数据不足')).toBeInTheDocument();
  expect(screen.getByText(/有效权重不足 50%/)).toBeInTheDocument();
});

it('refreshes after expiry, deduplicates requests and stops polling after unmount', async () => {
  jest.useFakeTimers();
  const { store, unmount } = renderWithProviders(<MarketSignalTab />);
  await act(async () => { await Promise.resolve(); });
  await act(async () => { await store.dispatch(fetchMarketSignal()); });
  expect(fetchSignal).toHaveBeenCalledTimes(1);
  await act(async () => { jest.advanceTimersByTime(DATA_REFRESH_MS); });
  expect(fetchSignal).toHaveBeenCalledTimes(2);
  unmount();
  await act(async () => { jest.advanceTimersByTime(DATA_REFRESH_MS); });
  expect(fetchSignal).toHaveBeenCalledTimes(2);
});
