import { screen, fireEvent, waitFor } from '@testing-library/react';
import { Routes, Route } from 'react-router-dom';
import PageShell from '../../Main/PageShell/PageShell';
import { renderWithProviders } from '../testUtils';
import { pullDisplaySettings, pullGeneralSettings } from '../../API/settings';
import { DEFAULT_DISPLAY_SETTINGS } from '../../displaySettings';
import { checkSession } from '../../API/auth';

jest.mock('../../Main/BackgroundGlobe/Globe', () => ({
  __esModule: true,
  default: ({ config }: { config?: {
    baseColor?: number[];
    glowColor?: number[];
    markerColor?: number[];
  } }) => (
    <div
      data-testid="globe-stub"
      data-point-color={config?.baseColor?.join(',')}
      data-glow-color={config?.glowColor?.join(',')}
      data-marker-color={config?.markerColor?.join(',')}
    />
  ),
}));

jest.mock('../../API/data', () => ({
  pullCurrencies: jest.fn(() => Promise.resolve(['USD', 'CNY'])),
  pullExchangeRate: jest.fn(() => Promise.resolve(7.2)),
  pullPopularAISkills: jest.fn(() => Promise.resolve([])),
  pullSentimentScore: jest.fn(() => Promise.resolve({ normalizedScore: 0.1, dailyAverage: 0.1 })),
  pullUsStockIndices: jest.fn(() => Promise.resolve([])),
  pullMarketTrends: jest.fn(() => Promise.resolve({ points: [], correlations: [] })),
}));

jest.mock('../../API/settings', () => ({
  pullGeneralSettings: jest.fn(),
  updateGeneralSettings: jest.fn(),
  pullDisplaySettings: jest.fn(),
  updateDisplaySettings: jest.fn(),
}));

jest.mock('../../API/auth', () => ({
  checkLogin: jest.fn(),
  checkSession: jest.fn(),
  logout: jest.fn(),
}));

const mockPullSettings = pullGeneralSettings as jest.Mock;
const mockPullDisplay = pullDisplaySettings as jest.Mock;
const mockCheckSession = checkSession as jest.Mock;

beforeEach(() => {
  jest.clearAllMocks();
  mockCheckSession.mockResolvedValue({ data: { success: true, result: '登录有效' } });
  mockPullSettings.mockResolvedValue({
    defaultPage: 0,
    defaultBaseCurrency: 'USD',
    defaultQuoteCurrency: 'CNY',
  });
  mockPullDisplay.mockResolvedValue(DEFAULT_DISPLAY_SETTINGS);
});

describe('PageShell', () => {
  it('defaults to the overview tab', async () => {
    renderWithProviders(<PageShell />);
    expect(await screen.findByRole('tab', { name: '概览' })).toHaveAttribute('aria-selected', 'true');
  });

  it('renders each tab panel when selected', async () => {
    renderWithProviders(<PageShell />);

    await screen.findByRole('tab', { name: '概览' });
    expect(screen.getAllByRole('tab').map((tab) => tab.textContent)).toEqual([
      '概览',
      '市场情绪',
      '汇率',
      '美股',
      '市场走势',
      '更多',
    ]);

    fireEvent.click(screen.getByRole('tab', { name: '市场情绪' }));
    expect(await screen.findByText('市场情绪指数')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('tab', { name: '汇率' }));
    expect(screen.getByText('汇率查询')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('tab', { name: '美股' }));
    expect(screen.getByText('美股主要指数')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('tab', { name: '市场走势' }));
    expect(await screen.findByText('7日市场走势')).toBeInTheDocument();

    fireEvent.click(screen.getByRole('tab', { name: '更多' }));
    expect(screen.getByText('AI 热门技能榜')).toBeInTheDocument();
  });

  it('loads the account default page when the session cookie is valid', async () => {
    mockCheckSession.mockResolvedValue({ data: { success: true, result: '登录有效' } });
    mockPullSettings.mockResolvedValue({
      defaultPage: 5,
      defaultBaseCurrency: 'USD',
      defaultQuoteCurrency: 'CNY',
    });

    const { store } = renderWithProviders(<PageShell />);

    expect(await screen.findByText('AI 热门技能榜')).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: '更多' })).toHaveAttribute('aria-selected', 'true');
    expect(store.getState().page.pagenum).toBe(5);
  });

  it('applies the saved background and globe colors', async () => {
    mockPullDisplay.mockResolvedValue({
      backgroundColor: '#112233',
      globeGlowColor: '#0000FF',
      globePointColor: '#FF0000',
      globeMarkerColor: '#00FF00',
    });

    const { container } = renderWithProviders(<PageShell />);

    await waitFor(() => expect(container.querySelector('.page-shell')).toHaveStyle({
      backgroundColor: '#112233',
    }));
    expect(screen.getByTestId('globe-stub')).toHaveAttribute('data-point-color', '1,0,0');
    expect(screen.getByTestId('globe-stub')).toHaveAttribute('data-glow-color', '0,0,1');
    expect(screen.getByTestId('globe-stub')).toHaveAttribute('data-marker-color', '0,1,0');
  });

  it('keeps the overview tab when settings fail to load', async () => {
    mockCheckSession.mockResolvedValue({ data: { success: true, result: '登录有效' } });
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

  it('switches tabs with the left and right arrow keys', async () => {
    const { store } = renderWithProviders(<PageShell />);
    await screen.findByRole('tab', { name: '概览' });

    fireEvent.keyDown(window, { key: 'ArrowRight' });
    expect(store.getState().page.pagenum).toBe(1);

    fireEvent.keyDown(window, { key: 'ArrowRight' });
    expect(store.getState().page.pagenum).toBe(2);
    expect(screen.getByRole('tab', { name: '汇率' })).toHaveAttribute('aria-selected', 'true');

    fireEvent.keyDown(window, { key: 'ArrowLeft' });
    expect(store.getState().page.pagenum).toBe(1);
  });

  it('switches to the next tab on a leftwards trackpad swipe', async () => {
    const { store, container } = renderWithProviders(<PageShell />);
    await screen.findByRole('tab', { name: '概览' });

    const shell = container.querySelector('.page-shell') as HTMLElement;
    fireEvent.wheel(shell, { deltaX: 180, deltaY: 0 });
    expect(store.getState().page.pagenum).toBe(1);
  });

  it('switches to the previous tab on a rightwards trackpad swipe', async () => {
    mockPullSettings.mockResolvedValue({
      defaultPage: 3,
      defaultBaseCurrency: 'USD',
      defaultQuoteCurrency: 'CNY',
    });

    const { store, container } = renderWithProviders(<PageShell />);
    await screen.findByRole('tab', { name: '美股' });

    const shell = container.querySelector('.page-shell') as HTMLElement;
    fireEvent.wheel(shell, { deltaX: -180, deltaY: 0 });
    expect(store.getState().page.pagenum).toBe(2);
  });

  it('does not switch tabs on a vertical wheel scroll', async () => {
    const { store, container } = renderWithProviders(<PageShell />);
    await screen.findByRole('tab', { name: '概览' });

    const shell = container.querySelector('.page-shell') as HTMLElement;
    fireEvent.wheel(shell, { deltaX: 0, deltaY: 100 });
    expect(store.getState().page.pagenum).toBe(0);
  });

  it('advances several tabs on one long swipe', async () => {
    const { store, container } = renderWithProviders(<PageShell />);
    await screen.findByRole('tab', { name: '概览' });

    const shell = container.querySelector('.page-shell') as HTMLElement;
    // 900px of accumulated horizontal distance = 5 tab steps.
    fireEvent.wheel(shell, { deltaX: 900, deltaY: 0 });
    expect(store.getState().page.pagenum).toBe(5);
  });

  it('switches back and forth on consecutive swipes without getting stuck', async () => {
    const { store, container } = renderWithProviders(<PageShell />);
    await screen.findByRole('tab', { name: '概览' });

    const shell = container.querySelector('.page-shell') as HTMLElement;

    fireEvent.wheel(shell, { deltaX: 180, deltaY: 0 });
    expect(store.getState().page.pagenum).toBe(1);

    fireEvent.wheel(shell, { deltaX: -180, deltaY: 0 });
    expect(store.getState().page.pagenum).toBe(0);
  });

  it('keeps the tab while wobbling inside its interval', async () => {
    const { store, container } = renderWithProviders(<PageShell />);
    await screen.findByRole('tab', { name: '概览' });

    const shell = container.querySelector('.page-shell') as HTMLElement;

    // 2.5 units: interval (1.5, 2.5] still belongs to the second tab over.
    fireEvent.wheel(shell, { deltaX: 450, deltaY: 0 });
    expect(store.getState().page.pagenum).toBe(2);

    // Back to 1.6 units: still inside (1.5, 2.5], so the tab stays put.
    fireEvent.wheel(shell, { deltaX: -162, deltaY: 0 });
    expect(store.getState().page.pagenum).toBe(2);

    // Back to 1.4 units: crossed into (0.5, 1.5], so the tab follows.
    fireEvent.wheel(shell, { deltaX: -36, deltaY: 0 });
    expect(store.getState().page.pagenum).toBe(1);
  });

  it('restarts accumulation at the active tab after a pause', async () => {
    let now = 1000;
    const nowSpy = jest.spyOn(Date, 'now').mockImplementation(() => now);

    try {
      const { store, container } = renderWithProviders(<PageShell />);
      await screen.findByRole('tab', { name: '概览' });

      const shell = container.querySelector('.page-shell') as HTMLElement;

      // First gesture: ~0.6 units, advances one tab.
      fireEvent.wheel(shell, { deltaX: 100, deltaY: 0 });
      expect(store.getState().page.pagenum).toBe(1);

      // After a pause, a new gesture starts at zero relative to tab 1, so the
      // same distance advances one more tab (instead of riding the old residue).
      now += 300;
      fireEvent.wheel(shell, { deltaX: 100, deltaY: 0 });
      expect(store.getState().page.pagenum).toBe(2);
    } finally {
      nowSpy.mockRestore();
    }
  });
});
