import {
  loginSlice,
  logIn,
  logOut,
  storeToken,
  checkLoginThunk,
  selectIsLoggedIn,
  selectToken,
} from '../../Variable/login';
import { checkLogin } from '../../API/auth';
import { createTestStore } from '../testUtils';
import type { RootState } from '../../customTypes';

jest.mock('../../API/auth', () => ({
  checkLogin: jest.fn(),
}));

const mockCheckLogin = checkLogin as jest.MockedFunction<typeof checkLogin>;
const reducer = loginSlice.reducer;

describe('login slice reducers', () => {
  it('returns initial state', () => {
    expect(reducer(undefined, { type: 'unknown' })).toEqual({
      isLoggedIn: false,
      token: null,
      loading: false,
      error: null,
    });
  });

  it('logIn sets isLoggedIn', () => {
    const state = reducer(undefined, logIn());
    expect(state.isLoggedIn).toBe(true);
  });

  it('logOut clears login state and stored token', () => {
    localStorage.setItem('authToken', 'abc');
    const state = reducer(
      { isLoggedIn: true, token: 'abc', loading: false, error: null },
      logOut()
    );
    expect(state.isLoggedIn).toBe(false);
    expect(state.token).toBeNull();
    expect(localStorage.getItem('authToken')).toBeNull();
  });

  it('storeToken stores the token', () => {
    const state = reducer(undefined, storeToken('tok'));
    expect(state.token).toBe('tok');
  });

  it('selectors read from state', () => {
    const state = {
      login: { isLoggedIn: true, token: 't', loading: false, error: null },
    } as RootState;
    expect(selectIsLoggedIn(state)).toBe(true);
    expect(selectToken(state)).toBe('t');
  });
});

describe('checkLoginThunk', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();
  });

  it('fulfills and stores token on successful login', async () => {
    mockCheckLogin.mockResolvedValue({
      data: { result: '登录成功', token: 'tok123' },
    } as never);

    const store = createTestStore();
    await store.dispatch(checkLoginThunk({ email: 'a@b.com', password: 'pw' }));

    const state = store.getState().login;
    expect(state.isLoggedIn).toBe(true);
    expect(state.token).toBe('tok123');
    expect(state.loading).toBe(false);
    expect(state.error).toBeNull();
  });

  it('rejects with backend message on failed login', async () => {
    mockCheckLogin.mockResolvedValue({
      data: { result: '密码错误' },
    } as never);

    const store = createTestStore();
    await store.dispatch(checkLoginThunk({ email: 'a@b.com', password: 'bad' }));

    const state = store.getState().login;
    expect(state.isLoggedIn).toBe(false);
    expect(state.token).toBeNull();
    expect(state.error).toBe('密码错误');
  });

  it('rejects with network error message on exception', async () => {
    mockCheckLogin.mockRejectedValue(new Error('boom'));

    const store = createTestStore();
    await store.dispatch(checkLoginThunk({ email: 'a@b.com', password: 'pw' }));

    const state = store.getState().login;
    expect(state.isLoggedIn).toBe(false);
    expect(state.error).toBe('网络错误');
  });
});
