import {
  loginSlice,
  logIn,
  logOut,
  checkLoginThunk,
  restoreSessionThunk,
  selectIsLoggedIn,
} from '../../Variable/login';
import { checkLogin, checkSession } from '../../API/auth';
import { createTestStore } from '../testUtils';
import type { RootState } from '../../customTypes';

jest.mock('../../API/auth', () => ({
  checkLogin: jest.fn(),
  checkSession: jest.fn(),
}));

const mockCheckLogin = checkLogin as jest.MockedFunction<typeof checkLogin>;
const mockCheckSession = checkSession as jest.MockedFunction<typeof checkSession>;
const reducer = loginSlice.reducer;

describe('login slice reducers', () => {
  it('returns initial state', () => {
    expect(reducer(undefined, { type: 'unknown' })).toEqual({
      isLoggedIn: false,
      sessionChecked: false,
      loading: false,
      error: null,
    });
  });

  it('logIn sets isLoggedIn', () => {
    const state = reducer(undefined, logIn());
    expect(state.isLoggedIn).toBe(true);
  });

  it('logOut clears login state', () => {
    const state = reducer(
      { isLoggedIn: true, sessionChecked: true, loading: false, error: null },
      logOut()
    );
    expect(state.isLoggedIn).toBe(false);
  });

  it('selectors read from state', () => {
    const state = {
      login: { isLoggedIn: true, sessionChecked: true, loading: false, error: null },
    } as RootState;
    expect(selectIsLoggedIn(state)).toBe(true);
  });
});

describe('checkLoginThunk', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();
  });

  it('fulfills and marks the session as logged in', async () => {
    mockCheckLogin.mockResolvedValue({
      data: { success: true, result: '登录成功' },
    } as never);

    const store = createTestStore();
    await store.dispatch(checkLoginThunk({ email: 'a@b.com', password: 'pw' }));

    const state = store.getState().login;
    expect(state.isLoggedIn).toBe(true);
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

  it('restores the logged-in state from the server cookie', async () => {
    mockCheckSession.mockResolvedValue({
      data: { success: true, result: '登录有效' },
    } as never);

    const store = createTestStore();
    await store.dispatch(restoreSessionThunk());

    expect(store.getState().login.isLoggedIn).toBe(true);
    expect(store.getState().login.sessionChecked).toBe(true);
  });
});
