import { fireEvent, screen } from '@testing-library/react';
import MoreTab from '../../Main/Contents/MoreTab';
import { renderWithProviders } from '../testUtils';
import { pullPopularAISkills } from '../../API/data';

jest.mock('../../API/data', () => ({
  pullCurrencies: jest.fn(),
  pullExchangeRate: jest.fn(),
  pullPopularAISkills: jest.fn(),
  pullSentimentScore: jest.fn(),
  pullUsStockIndices: jest.fn(),
}));

const mockPull = pullPopularAISkills as jest.Mock;

beforeEach(() => {
  jest.clearAllMocks();
});

describe('MoreTab', () => {
  it('shows five skills by default and expands the full ranking', async () => {
    mockPull.mockResolvedValue([
      { rank: 1, skill: 'RAG', mentions: 120 },
      { rank: 2, skill: 'MCP', mentions: 95 },
      { rank: 3, skill: 'Agents', mentions: 82 },
      { rank: 4, skill: 'Embeddings', mentions: 70 },
      { rank: 5, skill: 'Fine-tuning', mentions: 61 },
      { rank: 6, skill: 'Computer vision', mentions: 54 },
    ]);
    renderWithProviders(<MoreTab />);

    expect(await screen.findByText('RAG')).toBeInTheDocument();
    expect(screen.getByText('Fine-tuning')).toBeInTheDocument();
    expect(screen.queryByText('Computer vision')).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole('button', { name: '查看全部 6 项' }));

    expect(screen.getByText('Computer vision')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '收起榜单' })).toHaveAttribute('aria-expanded', 'true');
  });

  it('shows an empty state when no skills are returned', async () => {
    mockPull.mockResolvedValue([]);
    renderWithProviders(<MoreTab />);

    expect(await screen.findByText('今天暂无可用数据。')).toBeInTheDocument();
  });

  it('shows an error message when the fetch fails', async () => {
    mockPull.mockRejectedValue(new Error('加载失败'));
    renderWithProviders(<MoreTab />);

    expect(await screen.findByText('加载失败')).toBeInTheDocument();
  });
});
