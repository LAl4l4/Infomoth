import {
  fetchAISkills,
  fetchSentimentScore,
  fetchCurrencies,
  fetchExchangeRate,
  fetchUsStockIndices,
  clearDataCache,
  selectAISkills,
  selectSentimentScore,
  selectCurrencies,
  selectExchangeRates,
  selectUsStockIndices,
} from '../../Variable/dataCache';
import {
  pullCurrencies,
  pullExchangeRate,
  pullPopularAISkills,
  pullSentimentScore,
  pullUsStockIndices,
} from '../../API/data';
import { createTestStore } from '../testUtils';
import type { RootState } from '../../customTypes';

jest.mock('../../API/data', () => ({
  pullCurrencies: jest.fn(),
  pullExchangeRate: jest.fn(),
  pullPopularAISkills: jest.fn(),
  pullSentimentScore: jest.fn(),
  pullUsStockIndices: jest.fn(),
}));

const mockSkills = pullPopularAISkills as jest.Mock;
const mockSentiment = pullSentimentScore as jest.Mock;
const mockCurrencies = pullCurrencies as jest.Mock;
const mockRate = pullExchangeRate as jest.Mock;
const mockStocks = pullUsStockIndices as jest.Mock;

beforeEach(() => {
  jest.clearAllMocks();
});

describe('dataCache thunks', () => {
  it('fetchAISkills loads data and caches it on second dispatch', async () => {
    mockSkills.mockResolvedValue([{ rank: 1, skill: 'RAG', mentions: 10 }]);
    const store = createTestStore();

    await store.dispatch(fetchAISkills());
    await store.dispatch(fetchAISkills());

    expect(mockSkills).toHaveBeenCalledTimes(1);
    expect(selectAISkills(store.getState() as RootState).data).toHaveLength(1);
  });

  it('fetchSentimentScore stores the score and does not refetch', async () => {
    const sentiment = { normalizedScore: 0.75, dailyAverage: 0.6 };
    mockSentiment.mockResolvedValue(sentiment);
    const store = createTestStore();

    await store.dispatch(fetchSentimentScore());
    await store.dispatch(fetchSentimentScore());

    expect(mockSentiment).toHaveBeenCalledTimes(1);
    expect(selectSentimentScore(store.getState() as RootState).data).toEqual(sentiment);
  });

  it('fetchCurrencies caches the list', async () => {
    mockCurrencies.mockResolvedValue(['USD', 'CNY']);
    const store = createTestStore();

    await store.dispatch(fetchCurrencies());
    await store.dispatch(fetchCurrencies());

    expect(mockCurrencies).toHaveBeenCalledTimes(1);
    expect(selectCurrencies(store.getState() as RootState).data).toEqual(['USD', 'CNY']);
  });

  it('fetchExchangeRate caches per currency pair', async () => {
    mockRate.mockResolvedValue(7.2);
    const store = createTestStore();

    await store.dispatch(fetchExchangeRate({ base: 'USD', quote: 'CNY' }));
    await store.dispatch(fetchExchangeRate({ base: 'USD', quote: 'CNY' }));

    expect(mockRate).toHaveBeenCalledTimes(1);
    expect(selectExchangeRates(store.getState() as RootState).data['USD-CNY']).toBe(7.2);

    // a different pair triggers a new request
    await store.dispatch(fetchExchangeRate({ base: 'USD', quote: 'EUR' }));
    expect(mockRate).toHaveBeenCalledTimes(2);
  });

  it('fetchUsStockIndices loads and caches indices', async () => {
    mockStocks.mockResolvedValue([
      { symbol: '^GSPC', name: 'S&P 500', price: 6000, change: 10, changePercent: 0.17, date: '2026-07-30' },
    ]);
    const store = createTestStore();

    await store.dispatch(fetchUsStockIndices());
    await store.dispatch(fetchUsStockIndices());

    expect(mockStocks).toHaveBeenCalledTimes(1);
    expect(selectUsStockIndices(store.getState() as RootState).data).toHaveLength(1);
  });

  it('sets error when a fetch fails', async () => {
    mockSkills.mockRejectedValue(new Error('加载失败'));
    const store = createTestStore();

    await store.dispatch(fetchAISkills());

    const entry = selectAISkills(store.getState() as RootState);
    expect(entry.loading).toBe(false);
    expect(entry.error).toBe('加载失败');
  });
});

describe('clearDataCache', () => {
  it('resets all cached entries', async () => {
    mockSentiment.mockResolvedValue({ normalizedScore: 0.5, dailyAverage: 0.4 });
    const store = createTestStore();
    await store.dispatch(fetchSentimentScore());
    expect(selectSentimentScore(store.getState() as RootState).data?.normalizedScore).toBe(0.5);

    store.dispatch(clearDataCache());

    const state = store.getState() as RootState;
    expect(selectSentimentScore(state).data).toBeNull();
    expect(selectAISkills(state).data).toBeNull();
    expect(selectExchangeRates(state).data).toEqual({});
  });
});


describe('cache refresh', () => {
  afterEach(() => jest.restoreAllMocks());

  it('refreshes expired data, preserves stale values on failure, and supports forced retry', async () => {
    const now = jest.spyOn(Date, 'now').mockReturnValue(1000);
    const original = [{ symbol: '^GSPC', price: 6000 }];
    mockStocks.mockResolvedValueOnce(original).mockRejectedValueOnce(new Error('offline'))
      .mockResolvedValueOnce([{ symbol: '^GSPC', price: 6100 }]);
    const store = createTestStore();
    await store.dispatch(fetchUsStockIndices());
    now.mockReturnValue(301001);
    await store.dispatch(fetchUsStockIndices());
    expect(selectUsStockIndices(store.getState()).data).toEqual(original);
    expect(selectUsStockIndices(store.getState()).error).toBe('offline');
    expect(selectUsStockIndices(store.getState()).updatedAt).toBe(1000);
    await store.dispatch(fetchUsStockIndices({ force: true }));
    expect(selectUsStockIndices(store.getState()).data?.[0].price).toBe(6100);
    expect(selectUsStockIndices(store.getState()).error).toBeNull();
  });

  it('expires exchange rates independently per pair', async () => {
    const now = jest.spyOn(Date, 'now').mockReturnValue(1000);
    mockRate.mockResolvedValue(7);
    const store = createTestStore();
    await store.dispatch(fetchExchangeRate({ base: 'USD', quote: 'CNY' }));
    now.mockReturnValue(201000);
    await store.dispatch(fetchExchangeRate({ base: 'AUD', quote: 'CNY' }));
    now.mockReturnValue(301001);
    await store.dispatch(fetchExchangeRate({ base: 'USD', quote: 'CNY' }));
    await store.dispatch(fetchExchangeRate({ base: 'AUD', quote: 'CNY' }));
    expect(mockRate).toHaveBeenCalledTimes(3);
  });
});
