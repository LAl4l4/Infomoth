import { screen, fireEvent } from '@testing-library/react';
import { Routes, Route } from 'react-router-dom';
import Register from '../../Account/Register';
import { renderWithProviders } from '../testUtils';
import { register } from '../../API/auth';

jest.mock('../../API/auth', () => ({
  checkLogin: jest.fn(),
  register: jest.fn(),
}));

const mockRegister = register as jest.Mock;

function renderRegister() {
  return renderWithProviders(
    <Routes>
      <Route path="/register" element={<Register />} />
      <Route path="/" element={<div>首页</div>} />
      <Route path="/login" element={<div>登录页</div>} />
    </Routes>,
    { route: '/register' }
  );
}

function fillForm(overrides: Partial<Record<'email' | 'username' | 'password' | 'confirm', string>> = {}) {
  const values = {
    email: 'a@b.com',
    username: 'nick',
    password: 'secret1',
    confirm: 'secret1',
    ...overrides,
  };
  if (values.email) fireEvent.change(screen.getByPlaceholderText('you@example.com'), { target: { value: values.email } });
  if (values.username) fireEvent.change(screen.getByPlaceholderText('username'), { target: { value: values.username } });
  if (values.password) fireEvent.change(screen.getByPlaceholderText('至少 6 位'), { target: { value: values.password } });
  if (values.confirm) fireEvent.change(screen.getByPlaceholderText('再次输入密码'), { target: { value: values.confirm } });
}

beforeEach(() => {
  jest.clearAllMocks();
});

describe('Register page', () => {
  it('requires all fields to be filled', async () => {
    renderRegister();
    fireEvent.click(screen.getByRole('button', { name: '创建账户' }));
    expect(await screen.findByText('请完整填写所有必填项')).toBeInTheDocument();
    expect(mockRegister).not.toHaveBeenCalled();
  });

  it('shows an email validation hint for invalid addresses', () => {
    renderRegister();
    fireEvent.change(screen.getByPlaceholderText('you@example.com'), { target: { value: 'not-an-email' } });
    expect(screen.getByText('请输入有效的邮箱地址')).toBeInTheDocument();

    fireEvent.change(screen.getByPlaceholderText('you@example.com'), { target: { value: 'a@b.com' } });
    expect(screen.queryByText('请输入有效的邮箱地址')).not.toBeInTheDocument();
  });

  it('rejects mismatched passwords', async () => {
    renderRegister();
    fillForm({ confirm: 'different1' });
    fireEvent.click(screen.getByRole('button', { name: '创建账户' }));
    expect(await screen.findByText('两次输入的密码不一致')).toBeInTheDocument();
    expect(mockRegister).not.toHaveBeenCalled();
  });

  it('rejects passwords shorter than 6 characters', async () => {
    renderRegister();
    fillForm({ password: '12345', confirm: '12345' });
    fireEvent.click(screen.getByRole('button', { name: '创建账户' }));
    expect(await screen.findByText('密码至少需要 6 位字符')).toBeInTheDocument();
    expect(mockRegister).not.toHaveBeenCalled();
  });

  it('blocks @ characters in the username field', () => {
    renderRegister();
    const input = screen.getByPlaceholderText('username') as HTMLInputElement;
    fireEvent.change(input, { target: { value: 'nick@name' } });
    expect(input.value).toBe('');
  });

  it('registers successfully and navigates home', async () => {
    mockRegister.mockResolvedValue({ data: '注册成功' });
    renderRegister();
    fillForm();
    fireEvent.click(screen.getByRole('button', { name: '创建账户' }));

    expect(await screen.findByText('首页')).toBeInTheDocument();
    expect(mockRegister).toHaveBeenCalledWith('a@b.com', 'secret1', 'nick');
  });

  it('shows backend message when the username already exists', async () => {
    mockRegister.mockResolvedValue({ data: '用户名已存在' });
    renderRegister();
    fillForm();
    fireEvent.click(screen.getByRole('button', { name: '创建账户' }));

    expect(await screen.findByText('用户名已存在')).toBeInTheDocument();
  });

  it('shows network error when the request throws', async () => {
    mockRegister.mockRejectedValue(new Error('boom'));
    renderRegister();
    fillForm();
    fireEvent.click(screen.getByRole('button', { name: '创建账户' }));

    expect(await screen.findByText('网络错误')).toBeInTheDocument();
  });

  it('navigates to the login page', async () => {
    renderRegister();
    fireEvent.click(screen.getByRole('button', { name: '账户登录' }));
    expect(await screen.findByText('登录页')).toBeInTheDocument();
  });
});
