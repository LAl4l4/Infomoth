import { screen } from '@testing-library/react';
import SentimentTab from '../../Main/Contents/SentimentTab';
import { renderWithProviders } from '../testUtils';
import { pullSentimentScore } from '../../API/data';

jest.mock('../../API/data', () => ({
  pullCurrencies: jest.fn(),
  pullExchangeRate: jest.fn(),
  pullPopularAISkills: jest.fn(),
  pullSentimentScore: jest.fn(),
  pullUsStockIndices: jest.fn(),
}));

const mockPull = pullSentimentScore as jest.Mock;

beforeEach(() => {
  jest.clearAllMocks();
});

describe('SentimentTab', () => {
  it('shows a positive reading for an optimistic score', async () => {
    mockPull.mockResolvedValue({ instant: 0.3, dailyAverage: 0.2 });
    renderWithProviders(<SentimentTab />);

    expect(await screen.findByText('+0.3000')).toBeInTheDocument();
    expect(screen.getByText('+0.2000')).toBeInTheDocument();
    expect(screen.getByText('瞬时情绪')).toBeInTheDocument();
    expect(screen.getByText('今日滚动平均')).toBeInTheDocument();
    expect(screen.getAllByText('偏乐观')).toHaveLength(2);
    expect(screen.getByText(/市场整体情绪偏向乐观/)).toBeInTheDocument();
  });

  it('shows a negative reading for a pessimistic score', async () => {
    mockPull.mockResolvedValue({ instant: -0.2, dailyAverage: -0.1 });
    renderWithProviders(<SentimentTab />);

    expect(await screen.findByText('-0.2000')).toBeInTheDocument();
    expect(screen.getAllByText('偏悲观')).toHaveLength(2);
  });

  it('shows a neutral reading for a score near zero', async () => {
    mockPull.mockResolvedValue({ instant: 0.01, dailyAverage: 0.02 });
    renderWithProviders(<SentimentTab />);

    expect(await screen.findByText('+0.0100')).toBeInTheDocument();
    expect(screen.getAllByText('中性')).toHaveLength(2);
  });

  it('shows an error message when the fetch fails', async () => {
    mockPull.mockRejectedValue(new Error('加载失败'));
    renderWithProviders(<SentimentTab />);

    expect(await screen.findByText('加载失败')).toBeInTheDocument();
  });
});
