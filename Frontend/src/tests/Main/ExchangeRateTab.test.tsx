import { screen, fireEvent } from '@testing-library/react';
import ExchangeRateTab from '../../Main/Contents/ExchangeRateTab';
import { createTestStore, renderWithProviders } from '../testUtils';
import { pullCurrencies, pullExchangeRate } from '../../API/data';
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

const mockCurrencies = pullCurrencies as jest.Mock;
const mockRate = pullExchangeRate as jest.Mock;
const mockSettings = pullGeneralSettings as jest.Mock;

beforeEach(() => {
  jest.clearAllMocks();
  mockCurrencies.mockResolvedValue(['CNY', 'EUR', 'USD']);
  mockSettings.mockResolvedValue({
    defaultPage: 3,
    defaultBaseCurrency: 'USD',
    defaultQuoteCurrency: 'CNY',
  });
  mockRate.mockImplementation((base: string, quote: string) => {
    if (base === 'USD' && quote === 'CNY') return Promise.resolve(7.2);
    if (base === 'CNY' && quote === 'USD') return Promise.resolve(0.14);
    return Promise.resolve(1.0);
  });
});

describe('ExchangeRateTab', () => {
  it('loads currencies and shows the USD/CNY rate by default', async () => {
    renderWithProviders(<ExchangeRateTab />);

    expect(await screen.findByText(/1 USD = 7.2 CNY/)).toBeInTheDocument();
    expect(mockRate).toHaveBeenCalledWith('USD', 'CNY');
    expect(mockSettings).not.toHaveBeenCalled();
  });

  it('fetches a new rate when the quote currency changes', async () => {
    renderWithProviders(<ExchangeRateTab />);
    await screen.findByText(/1 USD = 7.2 CNY/);

    const selects = screen.getAllByRole('combobox');
    fireEvent.change(selects[1], { target: { value: 'EUR' } });

    expect(await screen.findByText(/1 USD = 1 EUR/)).toBeInTheDocument();
    expect(mockRate).toHaveBeenCalledWith('USD', 'EUR');
  });

  it('swaps base and quote currencies', async () => {
    const { container } = renderWithProviders(<ExchangeRateTab />);
    await screen.findByText(/1 USD = 7.2 CNY/);

    fireEvent.click(container.querySelector('.exchange-swap')!);

    expect(await screen.findByText(/1 CNY = 0.14 USD/)).toBeInTheDocument();
    expect(mockRate).toHaveBeenCalledWith('CNY', 'USD');
  });

  it('asks for two different currencies when base equals quote', async () => {
    renderWithProviders(<ExchangeRateTab />);
    await screen.findByText(/1 USD = 7.2 CNY/);

    const selects = screen.getAllByRole('combobox');
    fireEvent.change(selects[0], { target: { value: 'CNY' } });
    fireEvent.change(selects[1], { target: { value: 'CNY' } });

    expect(await screen.findByText('请选择两个不同的货币。')).toBeInTheDocument();
  });

  it('shows an error message when currency loading fails', async () => {
    mockCurrencies.mockRejectedValue(new Error('加载失败'));
    renderWithProviders(<ExchangeRateTab />);

    expect(await screen.findByText('加载失败')).toBeInTheDocument();
  });

  it('applies both saved default currency sides', async () => {
    mockSettings.mockResolvedValue({
      defaultPage: 3,
      defaultBaseCurrency: 'CNY',
      defaultQuoteCurrency: 'EUR',
    });
    mockRate.mockImplementation((base: string, quote: string) => (
      base === 'CNY' && quote === 'EUR' ? Promise.resolve(0.13) : Promise.resolve(1)
    ));

    renderWithProviders(<ExchangeRateTab />, {
      store: createTestStore({
        login: { isLoggedIn: true, sessionChecked: true, loading: false, error: null },
      }),
    });

    expect(await screen.findByText(/1 CNY = 0.13 EUR/)).toBeInTheDocument();
    expect(mockRate).toHaveBeenCalledWith('CNY', 'EUR');
  });
});
