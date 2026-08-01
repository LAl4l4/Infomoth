import { screen } from '@testing-library/react';
import UsStockTab from '../../Main/Contents/UsStockTab';
import { renderWithProviders } from '../testUtils';
import { pullUsStockIndices } from '../../API/data';

jest.mock('../../API/data', () => ({
  pullCurrencies: jest.fn(),
  pullExchangeRate: jest.fn(),
  pullPopularAISkills: jest.fn(),
  pullSentimentScore: jest.fn(),
  pullUsStockIndices: jest.fn(),
}));

const mockPull = pullUsStockIndices as jest.Mock;

const INDICES = [
  { symbol: '^GSPC', name: 'S&P 500', price: 6023.45, change: 12.3, changePercent: 0.2, date: '2026-07-30' },
  { symbol: '^IXIC', name: 'Nasdaq', price: 19876.5, change: -45.6, changePercent: -0.23, date: '2026-07-30' },
  { symbol: '^RUT', name: 'Russell 2000', price: 987.65, change: 3.21, changePercent: 0.33, date: '2026-07-30' },
];

beforeEach(() => {
  jest.clearAllMocks();
});

describe('UsStockTab', () => {
  it('renders a card per index with formatted values', async () => {
    mockPull.mockResolvedValue(INDICES);
    renderWithProviders(<UsStockTab />);

    expect(await screen.findByText('S&P 500')).toBeInTheDocument();
    expect(screen.getByText('Nasdaq')).toBeInTheDocument();
    expect(screen.getByText('Russell 2000')).toBeInTheDocument();

    // >= 1000 prices use locale grouping, < 1000 use plain toFixed
    expect(screen.getByText('6,023.45')).toBeInTheDocument();
    expect(screen.getByText('987.65')).toBeInTheDocument();

    expect(screen.getByText('+12.30')).toBeInTheDocument();
    expect(screen.getByText('-45.60')).toBeInTheDocument();
    expect(screen.getByText('+0.20%')).toBeInTheDocument();
    expect(screen.getByText('-0.23%')).toBeInTheDocument();
  });

  it('applies up/down styling based on change sign', async () => {
    mockPull.mockResolvedValue(INDICES);
    const { container } = renderWithProviders(<UsStockTab />);

    await screen.findByText('S&P 500');
    expect(container.querySelectorAll('.stock-up')).toHaveLength(2);
    expect(container.querySelectorAll('.stock-down')).toHaveLength(1);
  });

  it('shows an error message when the fetch fails', async () => {
    mockPull.mockRejectedValue(new Error('加载失败'));
    renderWithProviders(<UsStockTab />);

    expect(await screen.findByText('加载失败')).toBeInTheDocument();
  });
});
