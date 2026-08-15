import instance from '../../API/axios';
import {
  pullCurrencies,
  pullExchangeRate,
  pullPopularAISkills,
  pullSentimentScore,
  pullUsStockIndices,
  pullMarketTrends,
} from '../../API/data';

jest.mock('../../API/axios', () => ({
  __esModule: true,
  default: { get: jest.fn(), post: jest.fn(), put: jest.fn() },
}));

const mockGet = instance.get as jest.Mock;

beforeEach(() => {
  jest.clearAllMocks();
});

describe('data API', () => {
  it('pullCurrencies requests /data/currencies and returns the array', async () => {
    mockGet.mockResolvedValue({ data: ['USD', 'CNY'] });
    await expect(pullCurrencies()).resolves.toEqual(['USD', 'CNY']);
    expect(mockGet).toHaveBeenCalledWith('/data/currencies');
  });

  it('pullCurrencies rejects on non-array response', async () => {
    mockGet.mockResolvedValue({ data: { bad: true } });
    await expect(pullCurrencies()).rejects.toThrow('Invalid currencies response');
  });

  it('pullExchangeRate passes base/quote as params', async () => {
    mockGet.mockResolvedValue({ data: 7.2 });
    await expect(pullExchangeRate('USD', 'CNY')).resolves.toBe(7.2);
    expect(mockGet).toHaveBeenCalledWith('/data/exchangerate', {
      params: { base: 'USD', quote: 'CNY' },
    });
  });

  it('pullExchangeRate rejects on non-number response', async () => {
    mockGet.mockResolvedValue({ data: 'NaN' });
    await expect(pullExchangeRate('USD', 'CNY')).rejects.toThrow(
      'Invalid exchange rate response'
    );
  });

  it('pullPopularAISkills returns the skill list', async () => {
    const skills = [{ rank: 1, skill: 'RAG', mentions: 5 }];
    mockGet.mockResolvedValue({ data: skills });
    await expect(pullPopularAISkills()).resolves.toEqual(skills);
    expect(mockGet).toHaveBeenCalledWith('/data/ai-skills');
  });

  it('pullPopularAISkills rejects on non-array response', async () => {
    mockGet.mockResolvedValue({ data: null });
    await expect(pullPopularAISkills()).rejects.toThrow('Invalid AI skills response');
  });

  it('pullSentimentScore returns instant and daily rolling scores', async () => {
    const sentiment = { instant: -0.1, dailyAverage: 0.05 };
    mockGet.mockResolvedValue({ data: sentiment });
    await expect(pullSentimentScore()).resolves.toEqual(sentiment);
    expect(mockGet).toHaveBeenCalledWith('/data/sentiment');
  });

  it('pullSentimentScore rejects on non-number response', async () => {
    mockGet.mockResolvedValue({ data: { instant: 'bad', dailyAverage: 0.1 } });
    await expect(pullSentimentScore()).rejects.toThrow('Invalid sentiment response');
  });

  it('pullUsStockIndices returns the index list', async () => {
    const indices = [{ symbol: '^GSPC', name: 'S&P 500', price: 1, change: 1, changePercent: 1, date: 'd' }];
    mockGet.mockResolvedValue({ data: indices });
    await expect(pullUsStockIndices()).resolves.toEqual(indices);
    expect(mockGet).toHaveBeenCalledWith('/data/us-stock-indices');
  });

  it('pullUsStockIndices rejects on non-array response', async () => {
    mockGet.mockResolvedValue({ data: 'oops' });
    await expect(pullUsStockIndices()).rejects.toThrow('Invalid US stock indices response');
  });

  it('pullMarketTrends returns points and correlations', async () => {
    const trend = { points: [], correlations: [] };
    mockGet.mockResolvedValue({ data: trend });

    await expect(pullMarketTrends()).resolves.toEqual(trend);
    expect(mockGet).toHaveBeenCalledWith('/data/market-trends');
  });

  it('pullMarketTrends rejects an invalid response', async () => {
    mockGet.mockResolvedValue({ data: { points: [] } });

    await expect(pullMarketTrends()).rejects.toThrow('Invalid market trends response');
  });
});
