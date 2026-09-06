import instance from '../../API/axios';
import { checkLogin, checkSession, logout, register } from '../../API/auth';

jest.mock('../../API/axios', () => ({
  __esModule: true,
  default: { get: jest.fn(), post: jest.fn(), put: jest.fn() },
}));

const mockPost = instance.post as jest.Mock;
const mockGet = instance.get as jest.Mock;

beforeEach(() => {
  jest.clearAllMocks();
  localStorage.clear();
});

describe('checkLogin', () => {
  it('posts to /auth/login with username/pass in JSON body', async () => {
    mockPost.mockResolvedValue({ data: { success: true, result: '登录成功', token: 'tok' } });

    const res = await checkLogin('a@b.com', 'pw');

    expect(mockPost).toHaveBeenCalledWith('/auth/login', { username: 'a@b.com', pass: 'pw' });
    expect(res.data.result).toBe('登录成功');
  });

  it('does not expose or store a token on success', async () => {
    mockPost.mockResolvedValue({ data: { success: true, result: '登录成功', token: 'tok' } });
    await checkLogin('a@b.com', 'pw');
    expect(localStorage.getItem('authToken')).toBeNull();
  });

  it('does not save a token on failure', async () => {
    mockPost.mockResolvedValue({ data: { result: '密码错误' } });
    await checkLogin('a@b.com', 'bad');
    expect(localStorage.getItem('authToken')).toBeNull();
  });
});

describe('register', () => {
  it('posts to /auth/register with username/pass/email in JSON body', async () => {
    mockPost.mockResolvedValue({ data: { success: true, result: '注册成功' } });

    const res = await register('a@b.com', 'pw', 'nick');

    expect(mockPost).toHaveBeenCalledWith('/auth/register', { username: 'nick', pass: 'pw', email: 'a@b.com' });
    expect(res.data).toEqual({ success: true, result: '注册成功' });
  });
});

describe('checkSession', () => {
  it('checks the server-managed session cookie', async () => {
    mockGet.mockResolvedValue({ data: { success: true, result: '登录有效' } });

    await expect(checkSession()).resolves.toMatchObject({ data: { success: true, result: '登录有效' } });
    expect(mockGet).toHaveBeenCalledWith('/auth/session');
  });
});

describe('logout', () => {
  it('asks the server to clear the session cookie', async () => {
    mockPost.mockResolvedValue({ data: undefined });

    await logout();

    expect(mockPost).toHaveBeenCalledWith('/auth/logout');
  });
});
