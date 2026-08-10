import { screen, fireEvent, waitFor } from '@testing-library/react';
import { Routes, Route } from 'react-router-dom';
import Settings from '../../Account/Settings';
import { renderWithProviders } from '../testUtils';
import { pullGeneralSettings, updateGeneralSettings } from '../../API/settings';

jest.mock('../../API/data', () => ({
  pullCurrencies: jest.fn(() => Promise.resolve(['USD', 'CNY', 'EUR', 'AUD'])),
}));

jest.mock('../../API/settings', () => ({
  pullGeneralSettings: jest.fn(),
  updateGeneralSettings: jest.fn(),
}));

const mockPull = pullGeneralSettings as jest.Mock;
const mockUpdate = updateGeneralSettings as jest.Mock;

function renderSettings() {
  return renderWithProviders(
    <Routes>
      <Route path="/settings" element={<Settings />} />
      <Route path="/" element={<div>首页</div>} />
    </Routes>,
    { route: '/settings' }
  );
}

beforeEach(() => {
  jest.clearAllMocks();
  mockPull.mockResolvedValue({
    defaultPage: 3,
    defaultBaseCurrency: 'USD',
    defaultQuoteCurrency: 'CNY',
  });
});

describe('Settings page', () => {
  it('loads the saved default page into the select', async () => {
    renderSettings();

    expect(await screen.findByText('设置已同步')).toBeInTheDocument();
    expect((screen.getByLabelText('默认首页') as HTMLSelectElement).value).toBe('3');
  });

  it('marks unsaved changes when the select changes', async () => {
    renderSettings();
    await screen.findByText('设置已同步');

    fireEvent.change(screen.getByLabelText('默认首页'), { target: { value: '1' } });

    expect(screen.getByText('有未保存的更改')).toBeInTheDocument();
  });

  it('saves and navigates home with the new default page', async () => {
    mockUpdate.mockResolvedValue({
      defaultPage: 1,
      defaultBaseCurrency: 'USD',
      defaultQuoteCurrency: 'CNY',
    });
    const { store } = renderSettings();
    await screen.findByText('设置已同步');

    fireEvent.change(screen.getByLabelText('默认首页'), { target: { value: '1' } });
    fireEvent.click(screen.getByRole('button', { name: '保存并返回首页' }));

    expect(await screen.findByText('首页')).toBeInTheDocument();
    expect(mockUpdate).toHaveBeenCalledWith(1, 'USD', 'CNY');
    expect(store.getState().page.pagenum).toBe(1);
  });

  it('restores the default page to 概览 (0)', async () => {
    mockUpdate.mockResolvedValue({
      defaultPage: 0,
      defaultBaseCurrency: 'USD',
      defaultQuoteCurrency: 'CNY',
    });
    renderSettings();
    await screen.findByText('设置已同步');

    fireEvent.click(screen.getByRole('button', { name: '恢复默认' }));

    await waitFor(() => expect(mockUpdate).toHaveBeenCalledWith(0, 'USD', 'CNY'));
    expect(await screen.findByText('已保存到你的账户')).toBeInTheDocument();
  });

  it('shows the error message when loading fails', async () => {
    mockPull.mockRejectedValue(new Error('未登录'));
    renderSettings();

    expect(await screen.findByText('未登录')).toBeInTheDocument();
  });

  it('loads and saves both default exchange currency sides', async () => {
    mockPull.mockResolvedValue({
      defaultPage: 3,
      defaultBaseCurrency: 'EUR',
      defaultQuoteCurrency: 'AUD',
    });
    mockUpdate.mockResolvedValue({
      defaultPage: 3,
      defaultBaseCurrency: 'EUR',
      defaultQuoteCurrency: 'AUD',
    });
    renderSettings();

    await screen.findByText('设置已同步');
    expect((screen.getByLabelText('默认基础货币') as HTMLSelectElement).value).toBe('EUR');
    expect((screen.getByLabelText('默认目标货币') as HTMLSelectElement).value).toBe('AUD');

    fireEvent.click(screen.getByRole('button', { name: '保存并返回首页' }));
    await waitFor(() => expect(mockUpdate).toHaveBeenCalledWith(3, 'EUR', 'AUD'));
  });

  it('keeps both currency selectors in one setting group', async () => {
    renderSettings();
    await screen.findByText('设置已同步');

    const baseSelect = screen.getByLabelText('默认基础货币');
    const quoteSelect = screen.getByLabelText('默认目标货币');

    expect(baseSelect.closest('.setting-group')).toBe(quoteSelect.closest('.setting-group'));
  });

  it('switches between settings tabs', async () => {
    renderSettings();
    await screen.findByText('设置已同步');

    fireEvent.click(screen.getByText('显示'));
    expect(screen.getByText(/待实现/)).toBeInTheDocument();

    fireEvent.click(screen.getByText('关于'));
    expect(screen.getByText('InfoMoth · v0.1.0')).toBeInTheDocument();

    fireEvent.click(screen.getByText('常规'));
    expect(screen.getByLabelText('默认首页')).toBeInTheDocument();
  });
});
