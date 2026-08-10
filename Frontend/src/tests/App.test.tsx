import { render, screen, fireEvent } from '@testing-library/react';
import { Provider } from 'react-redux';
import App from '../App';
import { createTestStore } from './testUtils';
import { checkSession } from '../API/auth';

// cobe needs WebGL which jsdom lacks; replace the globe with a stub.
jest.mock('../Main/BackgroundGlobe/Globe', () => ({
  __esModule: true,
  default: () => <div data-testid="globe-stub" />,
}));

jest.mock('../API/data', () => ({
  pullCurrencies: jest.fn(() => Promise.resolve(['USD', 'CNY'])),
  pullExchangeRate: jest.fn(() => Promise.resolve(7.2)),
  pullPopularAISkills: jest.fn(() => Promise.resolve([{ rank: 1, skill: 'RAG', mentions: 1 }])),
  pullSentimentScore: jest.fn(() => Promise.resolve(0.1)),
  pullUsStockIndices: jest.fn(() => Promise.resolve([])),
  pullMarketTrends: jest.fn(() => Promise.resolve({ points: [], correlations: [] })),
}));

jest.mock('../API/settings', () => ({
  pullGeneralSettings: jest.fn(() => Promise.resolve({
    defaultPage: 0,
    defaultBaseCurrency: 'USD',
    defaultQuoteCurrency: 'CNY',
  })),
  updateGeneralSettings: jest.fn(),
}));

jest.mock('../API/auth', () => ({
  checkLogin: jest.fn(),
  checkSession: jest.fn(),
  logout: jest.fn(),
  register: jest.fn(),
}));

const mockCheckSession = checkSession as jest.Mock;

function renderApp() {
  const store = createTestStore();
  return {
    store,
    ...render(
      <Provider store={store}>
        <App />
      </Provider>
    ),
  };
}

describe('App', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockCheckSession.mockResolvedValue({ data: { result: '登录有效' } });
  });

  it('renders the home shell with the tab bar and overview', async () => {
    renderApp();

    expect(await screen.findByRole('tablist')).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: '概览' })).toHaveAttribute('aria-selected', 'true');
    expect(await screen.findByText('今日资讯概览')).toBeInTheDocument();
  });

  it('switches tabs when a tab is clicked', async () => {
    const { store } = renderApp();
    await screen.findByText('今日资讯概览');

    fireEvent.click(screen.getByRole('tab', { name: '美股' }));

    expect(store.getState().page.pagenum).toBe(4);
    expect(await screen.findByText('美股主要指数')).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: '美股' })).toHaveAttribute('aria-selected', 'true');
  });

  it('switches to the seven-day market trend tab', async () => {
    renderApp();

    await screen.findByText('今日资讯概览');

    fireEvent.click(screen.getByRole('tab', { name: '市场走势' }));

    expect(await screen.findByText('7日市场走势')).toBeInTheDocument();
  });

  it('renders the login page at /login', () => {
    window.history.pushState({}, '', '/login');
    renderApp();

    expect(screen.getByRole('heading', { name: '欢迎回来' })).toBeInTheDocument();
    window.history.pushState({}, '', '/');
  });

  it('redirects an unauthenticated home visit to login', async () => {
    mockCheckSession.mockResolvedValue({ data: { result: '未登录' } });
    renderApp();

    expect(await screen.findByRole('heading', { name: '欢迎回来' })).toBeInTheDocument();
  });
});
