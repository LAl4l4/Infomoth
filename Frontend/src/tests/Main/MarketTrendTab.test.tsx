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
        date: '2026-08-06',
        sentiment: 1,
        stocks: [{ symbol: '^GSPC', name: 'S&P 500', price: 100 }],
      },
      {
        date: '2026-08-07',
        sentiment: 2,
        stocks: [{ symbol: '^GSPC', name: 'S&P 500', price: 110 }],
      },
      {
        date: '2026-08-08',
        sentiment: 3,
        stocks: [{ symbol: '^GSPC', name: 'S&P 500', price: 120 }],
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
    renderWithProviders(<MarketTrendTab />);

    expect(await screen.findByText('7日市场走势')).toBeInTheDocument();
    expect(await screen.findByText('corr = 1.0000')).toBeInTheDocument();
    expect(screen.getByText(/重合日/)).toBeInTheDocument();
  });
});
