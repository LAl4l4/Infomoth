import { screen, fireEvent } from '@testing-library/react';
import PageShell from '../../Main/PageShell/PageShell';
import { renderWithProviders } from '../testUtils';
import { pullGeneralSettings } from '../../API/settings';

jest.mock('../../Main/BackgroundGlobe/Globe', () => ({
  __esModule: true,
  default: () => <div data-testid="globe-stub" />,
}));

jest.mock('../../API/data', () => ({
  pullCurrencies: jest.fn(() => Promise.resolve(['USD', 'CNY'])),
  pullExchangeRate: jest.fn(() => Promise.resolve(7.2)),
  pullPopularAISkills: jest.fn(() => Promise.resolve([])),
  pullSentimentScore: jest.fn(() => Promise.resolve(0.1)),
  pullUsStockIndices: jest.fn(() => Promise.resolve([])),
}));

jest.mock('../../API/settings', () => ({
  pullGeneralSettings: jest.fn(),
  updateGeneralSettings: jest.fn(),
}));

const mockPullSettings = pullGeneralSettings as jest.Mock;

beforeEach(() => {
  jest.clearAllMocks();
  localStorage.clear();
});

describe('PageShell', () => {
  it('defaults to the overview tab', () => {
    renderWithProviders(<PageShell />);
    expect(screen.getByRole('tab', { name: '概览' })).toHaveAttribute('aria-selected', 'true');
  });

  it('renders each tab panel when selected', async () => {
    renderWithProviders(<PageShell />);

    fireEvent.click(screen.getByRole('tab', { name: '市场情绪' }));
    expect(await screen.findByText('市场情绪指数')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('tab', { name: 'AI技能' }));
    expect(screen.getByText('AI 热门技能榜')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('tab', { name: '汇率' }));
    expect(screen.getByText('汇率查询')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('tab', { name: '美股' }));
    expect(screen.getByText('美股主要指数')).toBeInTheDocument();
  });

  it('loads the account default page when a token exists', async () => {
    localStorage.setItem('authToken', 'tok');
    mockPullSettings.mockResolvedValue({ defaultPage: 2 });

    const { store } = renderWithProviders(<PageShell />);

    expect(await screen.findByText('AI 热门技能榜')).toBeInTheDocument();
    expect(store.getState().page.pagenum).toBe(2);
  });

  it('keeps the overview tab when settings fail to load', async () => {
    localStorage.setItem('authToken', 'tok');
    mockPullSettings.mockRejectedValue(new Error('未登录'));

    renderWithProviders(<PageShell />);

    expect(screen.getByRole('tab', { name: '概览' })).toHaveAttribute('aria-selected', 'true');
  });

  it('does not load settings when logged out', () => {
    renderWithProviders(<PageShell />);
    expect(mockPullSettings).not.toHaveBeenCalled();
  });
});
