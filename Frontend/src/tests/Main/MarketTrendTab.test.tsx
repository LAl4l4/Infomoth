import { act, screen, waitFor } from '@testing-library/react';
import MarketTrendTab, {
  getStockChangePercents,
  MARKET_TREND_REFRESH_MS,
} from '../../Main/Contents/MarketTrendTab';
import { renderWithProviders } from '../testUtils';
import { pullMarketTrends } from '../../API/data';
import type { MarketTrendPoint } from '../../customTypes';

jest.mock('../../API/data', () => ({
  pullMarketTrends: jest.fn(),
}));

const mockPull = pullMarketTrends as jest.Mock;

beforeEach(() => {
  jest.clearAllMocks();
  mockPull.mockResolvedValue({
    points: [
      {
        date: '2026-08-07',
        sentiment: 1,
        stocks: [{ symbol: '^GSPC', name: 'S&P 500', price: 100, changePercent: 1 }],
      },
      {
        date: '2026-08-08',
        sentiment: 2,
        stocks: [],
      },
      {
        date: '2026-08-09',
        sentiment: 3,
        stocks: [],
      },
      {
        date: '2026-08-10',
        sentiment: 4,
        stocks: [{ symbol: '^GSPC', name: 'S&P 500', price: 110, changePercent: 2 }],
      },
    ],
    correlations: [{
      symbol: '^GSPC',
      name: 'S&P 500',
      corr: 1,
      sampleSize: 3,
    }],
  });
});

afterEach(() => {
  jest.useRealTimers();
});

describe('MarketTrendTab', () => {
  it('uses Yahoo daily change percentages instead of normalizing prices from the first point', () => {
    const points: MarketTrendPoint[] = [
      {
        date: '2026-08-07',
        sentiment: null,
        stocks: [{ symbol: '^GSPC', name: 'S&P 500', price: 100, changePercent: 1.25 }],
      },
      {
        date: '2026-08-10',
        sentiment: null,
        stocks: [{ symbol: '^GSPC', name: 'S&P 500', price: 110, changePercent: -0.5 }],
      },
    ];

    expect(getStockChangePercents(points, '^GSPC')).toEqual([1.25, -0.5]);
  });

  it('shows the seven-day chart and calculated correlation', async () => {
    const { container } = renderWithProviders(<MarketTrendTab />);

    expect(await screen.findByText('7日市场走势')).toBeInTheDocument();
    expect(screen.getByText(/交易时段使用 5 分钟行情并自动刷新/)).toBeInTheDocument();
    expect(await screen.findByText('市场情绪 × 10')).toBeInTheDocument();
    expect(await screen.findByText('corr = 1.0000')).toBeInTheDocument();
    expect(screen.getByText('^GSPC · 3 个交易日样本')).toBeInTheDocument();
    expect(screen.getAllByText('Weekend')).toHaveLength(2);
    expect(container.querySelectorAll('polyline')).toHaveLength(2);
  });

  it('refreshes market trends every five minutes while the tab is open', async () => {
    jest.useFakeTimers();
    renderWithProviders(<MarketTrendTab />);

    await waitFor(() => expect(mockPull).toHaveBeenCalledTimes(1));

    await act(async () => {
      jest.advanceTimersByTime(MARKET_TREND_REFRESH_MS);
      await Promise.resolve();
    });

    expect(mockPull).toHaveBeenCalledTimes(2);
  });
});
