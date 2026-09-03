import { screen, fireEvent, waitFor } from '@testing-library/react';
import { Routes, Route } from 'react-router-dom';
import Settings from '../../Account/Settings';
import { renderWithProviders } from '../testUtils';
import {
  pullDisplaySettings,
  pullGeneralSettings,
  updateDisplaySettings,
  updateGeneralSettings,
} from '../../API/settings';
import { DEFAULT_DISPLAY_SETTINGS } from '../../displaySettings';

jest.mock('../../API/data', () => ({
  pullCurrencies: jest.fn(() => Promise.resolve(['USD', 'CNY', 'EUR', 'AUD'])),
}));

jest.mock('../../API/settings', () => ({
  pullGeneralSettings: jest.fn(),
  updateGeneralSettings: jest.fn(),
  pullDisplaySettings: jest.fn(),
  updateDisplaySettings: jest.fn(),
}));

const mockPull = pullGeneralSettings as jest.Mock;
const mockUpdate = updateGeneralSettings as jest.Mock;
const mockPullDisplay = pullDisplaySettings as jest.Mock;
const mockUpdateDisplay = updateDisplaySettings as jest.Mock;

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
  mockPullDisplay.mockResolvedValue(DEFAULT_DISPLAY_SETTINGS);
});

describe('Settings page', () => {
  it('loads the saved default page into the select', async () => {
    renderSettings();

    expect(await screen.findByText('设置已同步')).toBeInTheDocument();
    expect((screen.getByLabelText('默认首页') as HTMLSelectElement).value).toBe('3');
  });

  it('lists default pages in the main navigation order', async () => {
    renderSettings();
    await screen.findByText('设置已同步');

    const options = Array.from(
      (screen.getByLabelText('默认首页') as HTMLSelectElement).options
    ).map((option) => [option.value, option.text]);

    expect(options).toEqual([
      ['0', '概览'],
      ['1', '市场情绪'],
      ['2', '汇率'],
      ['3', '美股'],
      ['4', '市场走势'],
      ['5', '更多'],
    ]);
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
    expect(await screen.findByLabelText('主界面背景颜色')).toBeInTheDocument();

    fireEvent.click(screen.getByText('关于'));
    expect(screen.getByText('InfoMoth · v0.1.0')).toBeInTheDocument();

    fireEvent.click(screen.getByText('常规'));
    expect(screen.getByLabelText('默认首页')).toBeInTheDocument();
  });

  it('loads and saves all display colors', async () => {
    mockPullDisplay.mockResolvedValue({
      backgroundColor: '#112233',
      globeGlowColor: '#445566',
      globePointColor: '#778899',
      globeMarkerColor: '#AABBCC',
    });
    mockUpdateDisplay.mockImplementation((settings) => Promise.resolve(settings));
    renderSettings();

    fireEvent.click(screen.getByText('显示'));
    expect(await screen.findByText('显示设置已同步')).toBeInTheDocument();
    expect((screen.getByLabelText('主界面背景颜色') as HTMLInputElement).value).toBe('#112233');
    expect((screen.getByLabelText('地球光晕颜色') as HTMLInputElement).value).toBe('#445566');

    fireEvent.change(screen.getByLabelText('地球点颜色'), { target: { value: '#010203' } });
    fireEvent.change(screen.getByLabelText('Mark 颜色'), { target: { value: '#A0B0C0' } });
    fireEvent.click(screen.getByRole('button', { name: '保存显示设置' }));

    await waitFor(() => expect(mockUpdateDisplay).toHaveBeenCalledWith({
      backgroundColor: '#112233',
      globeGlowColor: '#445566',
      globePointColor: '#010203',
      globeMarkerColor: '#A0B0C0',
    }));
    expect(await screen.findByText('显示设置已保存')).toBeInTheDocument();
  });

  it('restores and persists the default display colors', async () => {
    mockPullDisplay.mockResolvedValue({
      backgroundColor: '#112233',
      globeGlowColor: '#445566',
      globePointColor: '#778899',
      globeMarkerColor: '#AABBCC',
    });
    mockUpdateDisplay.mockResolvedValue(DEFAULT_DISPLAY_SETTINGS);
    renderSettings();

    fireEvent.click(screen.getByText('显示'));
    await screen.findByText('显示设置已同步');
    fireEvent.click(screen.getByRole('button', { name: '恢复默认' }));

    await waitFor(() => expect(mockUpdateDisplay).toHaveBeenCalledWith(DEFAULT_DISPLAY_SETTINGS));
    expect((screen.getByLabelText('主界面背景颜色') as HTMLInputElement).value)
      .toBe(DEFAULT_DISPLAY_SETTINGS.backgroundColor.toLowerCase());
  });
});
