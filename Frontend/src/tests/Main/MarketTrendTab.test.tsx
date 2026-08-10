import { screen } from '@testing-library/react';
import MarketTrendTab from '../../Main/Contents/MarketTrendTab';
import { renderWithProviders } from '../testUtils';
import { pullMarketTrends } from '../../API/data';

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

describe('MarketTrendTab', () => {
  it('shows the seven-day chart and calculated correlation', async () => {
    const { container } = renderWithProviders(<MarketTrendTab />);

    expect(await screen.findByText('7日市场走势')).toBeInTheDocument();
    expect(screen.getByText(/Crawler 抓取并持久化的交易日涨跌幅/)).toBeInTheDocument();
    expect(await screen.findByText('市场情绪 × 10')).toBeInTheDocument();
    expect(await screen.findByText('corr = 1.0000')).toBeInTheDocument();
    expect(screen.getByText('^GSPC · 3 个交易日样本')).toBeInTheDocument();
    expect(screen.getAllByText('Weekend')).toHaveLength(2);
    expect(container.querySelectorAll('polyline')).toHaveLength(2);
  });
});
