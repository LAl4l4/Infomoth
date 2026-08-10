import { screen, fireEvent } from '@testing-library/react';
import { Routes, Route } from 'react-router-dom';
import PageShell from '../../Main/PageShell/PageShell';
import { renderWithProviders } from '../testUtils';
import { pullGeneralSettings } from '../../API/settings';
import { checkSession } from '../../API/auth';

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
  pullMarketTrends: jest.fn(() => Promise.resolve({ points: [], correlations: [] })),
}));

jest.mock('../../API/settings', () => ({
  pullGeneralSettings: jest.fn(),
  updateGeneralSettings: jest.fn(),
}));

jest.mock('../../API/auth', () => ({
  checkLogin: jest.fn(),
  checkSession: jest.fn(),
  logout: jest.fn(),
}));

const mockPullSettings = pullGeneralSettings as jest.Mock;
const mockCheckSession = checkSession as jest.Mock;

beforeEach(() => {
  jest.clearAllMocks();
  mockCheckSession.mockResolvedValue({ data: { result: '登录有效' } });
  mockPullSettings.mockResolvedValue({
    defaultPage: 0,
    defaultBaseCurrency: 'USD',
    defaultQuoteCurrency: 'CNY',
  });
});

describe('PageShell', () => {
  it('defaults to the overview tab', async () => {
    renderWithProviders(<PageShell />);
    expect(await screen.findByRole('tab', { name: '概览' })).toHaveAttribute('aria-selected', 'true');
  });

  it('renders each tab panel when selected', async () => {
    renderWithProviders(<PageShell />);

    await screen.findByRole('tab', { name: '概览' });
    fireEvent.click(screen.getByRole('tab', { name: '市场情绪' }));
    expect(await screen.findByText('市场情绪指数')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('tab', { name: 'AI技能' }));
    expect(screen.getByText('AI 热门技能榜')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('tab', { name: '汇率' }));
    expect(screen.getByText('汇率查询')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('tab', { name: '美股' }));
    expect(screen.getByText('美股主要指数')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('tab', { name: '市场走势' }));
    expect(await screen.findByText('7日市场走势')).toBeInTheDocument();
  });

  it('loads the account default page when the session cookie is valid', async () => {
    mockCheckSession.mockResolvedValue({ data: { result: '登录有效' } });
    mockPullSettings.mockResolvedValue({
      defaultPage: 2,
      defaultBaseCurrency: 'USD',
      defaultQuoteCurrency: 'CNY',
    });

    const { store } = renderWithProviders(<PageShell />);

    expect(await screen.findByText('AI 热门技能榜')).toBeInTheDocument();
    expect(store.getState().page.pagenum).toBe(2);
  });

  it('keeps the overview tab when settings fail to load', async () => {
    mockCheckSession.mockResolvedValue({ data: { result: '登录有效' } });
    mockPullSettings.mockRejectedValue(new Error('未登录'));

    renderWithProviders(<PageShell />);

    expect(await screen.findByRole('tab', { name: '概览' })).toHaveAttribute('aria-selected', 'true');
  });

  it('redirects to login without loading settings when the session is invalid', async () => {
    mockCheckSession.mockResolvedValue({ data: { result: '未登录' } });

    renderWithProviders(
      <Routes>
        <Route path="/" element={<PageShell />} />
        <Route path="/login" element={<div>登录页</div>} />
      </Routes>
    );

    expect(await screen.findByText('登录页')).toBeInTheDocument();
    expect(mockPullSettings).not.toHaveBeenCalled();
  });
});
