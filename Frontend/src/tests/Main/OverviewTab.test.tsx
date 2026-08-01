import { screen } from '@testing-library/react';
import OverviewTab from '../../Main/Contents/OverviewTab';
import { renderWithProviders } from '../testUtils';
import {
  pullPopularAISkills,
  pullSentimentScore,
  pullExchangeRate,
  pullUsStockIndices,
} from '../../API/data';

jest.mock('../../API/data', () => ({
  pullCurrencies: jest.fn(),
  pullExchangeRate: jest.fn(),
  pullPopularAISkills: jest.fn(),
  pullSentimentScore: jest.fn(),
  pullUsStockIndices: jest.fn(),
}));

const mockSkills = pullPopularAISkills as jest.Mock;
const mockSentiment = pullSentimentScore as jest.Mock;
const mockRate = pullExchangeRate as jest.Mock;
const mockStocks = pullUsStockIndices as jest.Mock;

beforeEach(() => {
  jest.clearAllMocks();
  mockSkills.mockResolvedValue([{ rank: 1, skill: 'RAG', mentions: 42 }]);
  mockSentiment.mockResolvedValue(0.1234);
  mockRate.mockResolvedValue(7.2);
  mockStocks.mockResolvedValue([
    { symbol: '^GSPC', name: 'S&P 500', price: 6023.45, change: 10, changePercent: 0.17, date: '2026-07-30' },
  ]);
});

describe('OverviewTab', () => {
  it('renders the snapshot rows with fetched data', async () => {
    renderWithProviders(<OverviewTab />);

    expect(await screen.findByText('+0.1234')).toBeInTheDocument();
    expect(screen.getByText('1. RAG')).toBeInTheDocument();
    expect(screen.getByText('1 USD = 7.2 CNY')).toBeInTheDocument();
    expect(screen.getByText('6,023.45 (+0.17%)')).toBeInTheDocument();
  });

  it('shows fallback text when no data is available', async () => {
    mockSkills.mockResolvedValue([]);
    mockSentiment.mockResolvedValue(null as never);
    // sentiment thunk caches null; use a fresh store per test (renderWithProviders does)
    mockStocks.mockResolvedValue([]);
    mockRate.mockResolvedValue(undefined as never);

    renderWithProviders(<OverviewTab />);

    expect(await screen.findAllByText('今日暂无')).not.toHaveLength(0);
  });
});
