import { screen, fireEvent, waitFor } from '@testing-library/react';
import { Routes, Route } from 'react-router-dom';
import Login from '../../Account/Login';
import { renderWithProviders } from '../testUtils';
import { checkLogin } from '../../API/auth';

jest.mock('../../API/auth', () => ({
  checkLogin: jest.fn(),
  register: jest.fn(),
}));

const mockCheckLogin = checkLogin as jest.Mock;

function renderLogin() {
  return renderWithProviders(
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/" element={<div>首页</div>} />
      <Route path="/register" element={<div>注册页</div>} />
    </Routes>,
    { route: '/login' }
  );
}

beforeEach(() => {
  jest.clearAllMocks();
  localStorage.clear();
  window.alert = jest.fn();
});

describe('Login page', () => {
  it('logs in successfully and navigates home', async () => {
    mockCheckLogin.mockResolvedValue({ data: { result: '登录成功', token: 'tok' } });
    const { store } = renderLogin();

    fireEvent.change(screen.getByPlaceholderText('yours@example.com/username'), {
      target: { value: 'a@b.com' },
    });
    fireEvent.change(screen.getByPlaceholderText('password'), {
      target: { value: 'secret' },
    });
    fireEvent.click(screen.getByRole('button', { name: '登录' }));

    expect(await screen.findByText('首页')).toBeInTheDocument();
    expect(mockCheckLogin).toHaveBeenCalledWith('a@b.com', 'secret');
    expect(store.getState().login.isLoggedIn).toBe(true);
    expect(store.getState().login.token).toBe('tok');
  });

  it('shows the backend error message on failed login', async () => {
    mockCheckLogin.mockResolvedValue({ data: { result: '密码错误' } });
    renderLogin();

    fireEvent.change(screen.getByPlaceholderText('yours@example.com/username'), {
      target: { value: 'a@b.com' },
    });
    fireEvent.change(screen.getByPlaceholderText('password'), {
      target: { value: 'bad' },
    });
    fireEvent.click(screen.getByRole('button', { name: '登录' }));

    expect(await screen.findByText('密码错误')).toBeInTheDocument();
  });

  it('shows network error message when the request throws', async () => {
    mockCheckLogin.mockRejectedValue(new Error('boom'));
    renderLogin();

    fireEvent.change(screen.getByPlaceholderText('yours@example.com/username'), {
      target: { value: 'a@b.com' },
    });
    fireEvent.change(screen.getByPlaceholderText('password'), {
      target: { value: 'pw' },
    });
    fireEvent.click(screen.getByRole('button', { name: '登录' }));

    expect(await screen.findByText('网络错误')).toBeInTheDocument();
  });

  it('navigates to the register page', async () => {
    renderLogin();
    fireEvent.click(screen.getByRole('button', { name: '账号注册' }));
    expect(await screen.findByText('注册页')).toBeInTheDocument();
  });

  it('does not dispatch when fields are left empty and submitted', async () => {
    renderLogin();
    fireEvent.click(screen.getByRole('button', { name: '登录' }));
    await waitFor(() => expect(mockCheckLogin).toHaveBeenCalledWith('', ''));
  });
});
