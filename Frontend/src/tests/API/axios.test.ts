/**
 * Tests for the shared axios instance's interceptors.
 * The axios module is mocked so the interceptor functions can be captured
 * at module load time and invoked directly.
 */

jest.mock('axios', () => {
  const instance = {
    defaults: {} as { baseURL?: string },
    interceptors: {
      request: { use: jest.fn() },
      response: { use: jest.fn() },
    },
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
  };
  return {
    __esModule: true,
    default: { create: () => instance },
    __instance: instance,
  };
});

jest.mock('../../API/config', () => ({
  loadApiBaseUrl: jest.fn(() => Promise.resolve('http://api.test')),
}));

// Import after mocks so module-level interceptor registration hits the mocks.
import '../../API/axios';
import { loadApiBaseUrl } from '../../API/config';

type ReqFulfilled = (config: {
  headers: Record<string, string>;
  [k: string]: unknown;
}) => Promise<{ headers: Record<string, string>; [k: string]: unknown }>;

type RespRejected = (error: {
  response?: { status?: number; data?: unknown };
  message?: string;
}) => Promise<never>;

const mockInstance = (jest.requireMock('axios') as { __instance: {
  defaults: { baseURL?: string };
  interceptors: {
    request: { use: jest.Mock };
    response: { use: jest.Mock };
  };
} }).__instance;

const mockLoadApiBaseUrl = loadApiBaseUrl as jest.Mock;

const reqFulfilled = mockInstance.interceptors.request.use.mock.calls[0][0] as ReqFulfilled;
const respRejected = mockInstance.interceptors.response.use.mock.calls[0][1] as RespRejected;

beforeEach(() => {
  localStorage.clear();
  mockLoadApiBaseUrl.mockResolvedValue('http://api.test');
  delete mockInstance.defaults.baseURL;
});

describe('request interceptor', () => {
  it('resolves and caches the base URL', async () => {
    const config = await reqFulfilled({ headers: {} });
    expect(mockInstance.defaults.baseURL).toBe('http://api.test');
    expect(config.headers.Authorization).toBeUndefined();

    // second call reuses the cached baseURL without reloading config
    mockLoadApiBaseUrl.mockClear();
    await reqFulfilled({ headers: {} });
    expect(mockLoadApiBaseUrl).not.toHaveBeenCalled();
  });

  it('does not attach an Authorization header from local storage', async () => {
    localStorage.setItem('authToken', 'tok');
    const config = await reqFulfilled({ headers: {} });
    expect(config.headers.Authorization).toBeUndefined();
  });
});

describe('response interceptor', () => {
  it('leaves token storage untouched on 401', async () => {
    localStorage.setItem('authToken', 'tok');
    await expect(
      respRejected({ response: { status: 401, data: '未授权' } })
    ).rejects.toBeTruthy();
    expect(localStorage.getItem('authToken')).toBe('tok');
  });

  it('copies a string error body into error.message', async () => {
    const error = { response: { status: 500, data: '服务器错误' }, message: 'old' };
    await expect(respRejected(error)).rejects.toMatchObject({ message: '服务器错误' });
  });

  it('copies data.error into error.message', async () => {
    const error = { response: { status: 400, data: { error: '参数错误' } }, message: 'old' };
    await expect(respRejected(error)).rejects.toMatchObject({ message: '参数错误' });
  });

  it('passes through errors without a response body untouched', async () => {
    const error = { message: 'network down' };
    await expect(respRejected(error)).rejects.toMatchObject({ message: 'network down' });
  });
});
