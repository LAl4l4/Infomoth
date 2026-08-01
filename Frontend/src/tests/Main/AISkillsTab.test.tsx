import { screen } from '@testing-library/react';
import AISkillsTab from '../../Main/Contents/AISkillsTab';
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

describe('AISkillsTab', () => {
  it('renders the ranked skill list', async () => {
    mockPull.mockResolvedValue([
      { rank: 1, skill: 'RAG', mentions: 120 },
      { rank: 2, skill: 'MCP', mentions: 95 },
    ]);
    renderWithProviders(<AISkillsTab />);

    expect(await screen.findByText('RAG')).toBeInTheDocument();
    expect(screen.getByText('MCP')).toBeInTheDocument();
    expect(screen.getByText('01')).toBeInTheDocument();
    expect(screen.getByText('02')).toBeInTheDocument();
    expect(screen.getByText('120 mentions')).toBeInTheDocument();
  });

  it('shows an empty state when no skills are returned', async () => {
    mockPull.mockResolvedValue([]);
    renderWithProviders(<AISkillsTab />);

    expect(await screen.findByText('今天暂无可用数据。')).toBeInTheDocument();
  });

  it('shows an error message when the fetch fails', async () => {
    mockPull.mockRejectedValue(new Error('加载失败'));
    renderWithProviders(<AISkillsTab />);

    expect(await screen.findByText('加载失败')).toBeInTheDocument();
  });
});
