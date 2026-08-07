import { screen, fireEvent, waitFor } from '@testing-library/react';
import { Routes, Route } from 'react-router-dom';
import Profile from '../../Account/Profile';
import { renderWithProviders } from '../testUtils';
import { pullProfiles, updateProfile } from '../../API/prof';

jest.mock('../../API/prof', () => ({
  pullProfiles: jest.fn(),
  updateProfile: jest.fn(),
}));

jest.mock('../../API/auth', () => ({
  logout: jest.fn(() => Promise.resolve()),
}));

const mockPull = pullProfiles as jest.Mock;
const mockUpdate = updateProfile as jest.Mock;

const PROFILE = {
  bio: 'hello world',
  avatarUrl: 'http://img/avatar.png',
  birthday: '2000-01-01',
  gender: 'male',
};

function renderProfile() {
  return renderWithProviders(
    <Routes>
      <Route path="/profile" element={<Profile />} />
      <Route path="/" element={<div>首页</div>} />
    </Routes>,
    { route: '/profile' }
  );
}

beforeEach(() => {
  jest.clearAllMocks();
  window.alert = jest.fn();
  mockPull.mockResolvedValue(PROFILE);
});

describe('Profile page', () => {
  it('loads the profile and fills the form', async () => {
    renderProfile();

    expect(await screen.findByDisplayValue('hello world')).toBeInTheDocument();
    expect(screen.getByDisplayValue('2000-01-01')).toBeInTheDocument();
    expect(screen.getByDisplayValue('http://img/avatar.png')).toBeInTheDocument();
    expect(screen.getByDisplayValue('男')).toBeInTheDocument();
  });

  it('saves only dirty fields and refreshes the profile', async () => {
    mockUpdate.mockResolvedValue('保存成功');
    renderProfile();

    const bio = await screen.findByDisplayValue('hello world');
    fireEvent.change(bio, { target: { value: 'new bio' } });

    const saveBtn = screen.getByRole('button', { name: '保存修改' });
    expect(saveBtn).toBeEnabled();
    fireEvent.click(saveBtn);

    await waitFor(() => expect(mockUpdate).toHaveBeenCalledWith({ bio: 'new bio' }));
    await waitFor(() => expect(window.alert).toHaveBeenCalledWith('保存成功！'));
    // profile re-fetched after save
    expect(mockPull).toHaveBeenCalledTimes(2);
  });

  it('alerts an error when saving fails', async () => {
    mockUpdate.mockResolvedValue('保存失败：无权限');
    renderProfile();

    const bio = await screen.findByDisplayValue('hello world');
    fireEvent.change(bio, { target: { value: 'new bio' } });
    fireEvent.click(screen.getByRole('button', { name: '保存修改' }));

    await waitFor(() =>
      expect(window.alert).toHaveBeenCalledWith('保存失败: 保存失败：无权限')
    );
  });

  it('switches between side tabs', async () => {
    renderProfile();
    await screen.findByDisplayValue('hello world');

    fireEvent.click(screen.getByText('密码/邮箱'));
    expect(screen.getByRole('button', { name: '退出登录' })).toBeInTheDocument();
    expect(screen.queryByDisplayValue('hello world')).not.toBeInTheDocument();

    fireEvent.click(screen.getByText('其他设置'));
    expect(screen.getByText('waitforit')).toBeInTheDocument();

    fireEvent.click(screen.getByText('基本信息'));
    expect(screen.getByDisplayValue('hello world')).toBeInTheDocument();
  });

  it('logs out from the account section and navigates home', async () => {
    const { store } = renderProfile();
    store.dispatch({ type: 'login/logIn' });

    await screen.findByDisplayValue('hello world');
    fireEvent.click(screen.getByText('密码/邮箱'));
    fireEvent.click(screen.getByRole('button', { name: '退出登录' }));

    expect(await screen.findByText('首页')).toBeInTheDocument();
    expect(store.getState().login.isLoggedIn).toBe(false);
  });

  it('alerts when the profile fails to load', async () => {
    mockPull.mockRejectedValue(new Error('未登录'));
    renderProfile();

    await waitFor(() =>
      expect(window.alert).toHaveBeenCalledWith('获取用户信息失败: 未登录')
    );
  });
});
