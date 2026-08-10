import { screen } from '@testing-library/react';
import OverviewTab from '../../Main/Contents/OverviewTab';
import { createTestStore, renderWithProviders } from '../testUtils';
import {
  pullPopularAISkills,
  pullSentimentScore,
  pullExchangeRate,
  pullUsStockIndices,
} from '../../API/data';
import { pullGeneralSettings } from '../../API/settings';

jest.mock('../../API/data', () => ({
  pullCurrencies: jest.fn(),
  pullExchangeRate: jest.fn(),
  pullPopularAISkills: jest.fn(),
  pullSentimentScore: jest.fn(),
  pullUsStockIndices: jest.fn(),
}));

jest.mock('../../API/settings', () => ({
  pullGeneralSettings: jest.fn(),
}));

const mockSkills = pullPopularAISkills as jest.Mock;
const mockSentiment = pullSentimentScore as jest.Mock;
const mockRate = pullExchangeRate as jest.Mock;
const mockStocks = pullUsStockIndices as jest.Mock;
const mockSettings = pullGeneralSettings as jest.Mock;

beforeEach(() => {
  jest.clearAllMocks();
  mockSkills.mockResolvedValue([{ rank: 1, skill: 'RAG', mentions: 42 }]);
  mockSentiment.mockResolvedValue(0.1234);
  mockRate.mockResolvedValue(7.2);
  mockStocks.mockResolvedValue([
    { symbol: '^GSPC', name: 'S&P 500', price: 6023.45, change: 10, changePercent: 0.17, date: '2026-07-30' },
  ]);
  mockSettings.mockResolvedValue({
    defaultPage: 0,
    defaultBaseCurrency: 'USD',
    defaultQuoteCurrency: 'CNY',
  });
});

describe('OverviewTab', () => {
  it('renders the snapshot rows with fetched data', async () => {
    renderWithProviders(<OverviewTab />);

    expect(await screen.findByText('+0.1234')).toBeInTheDocument();
    expect(screen.getByText('1. RAG')).toBeInTheDocument();
    expect(screen.getByText('1 USD = 7.2 CNY')).toBeInTheDocument();
    expect(screen.getByText('6,023.45 (+0.17%)')).toBeInTheDocument();
    expect(mockSettings).not.toHaveBeenCalled();
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

  it('uses the saved exchange-rate currency pair', async () => {
    mockSettings.mockResolvedValue({
      defaultPage: 0,
      defaultBaseCurrency: 'AUD',
      defaultQuoteCurrency: 'CNY',
    });
    mockRate.mockResolvedValue(4.7);

    renderWithProviders(<OverviewTab />, {
      store: createTestStore({
        login: { isLoggedIn: true, sessionChecked: true, loading: false, error: null },
      }),
    });

    expect(await screen.findByText('1 AUD = 4.7 CNY')).toBeInTheDocument();
    expect(screen.getByText('汇率速查 (AUD/CNY)')).toBeInTheDocument();
    expect(mockRate).toHaveBeenCalledWith('AUD', 'CNY');
  });
});
