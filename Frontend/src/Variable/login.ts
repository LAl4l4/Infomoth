import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import { checkLogin, checkSession } from '../API/auth';
import type { RootState } from '../customTypes';

interface LoginState {
  isLoggedIn: boolean;
  loading: boolean;
  error: string | null;
}

interface Credentials {
  email: string;
  password: string;
}

const initialState: LoginState = {
  isLoggedIn: false,
  loading: false,
  error: null,
};

export const checkLoginThunk = createAsyncThunk<
  void,
  Credentials,
  { rejectValue: string }
>('login/checkLogin', async ({ email, password }, { rejectWithValue }) => {
  try {
    const res = await checkLogin(email, password);

    if (res.data.result === '登录成功') {
      return;
    } else {
      return rejectWithValue(res.data.result);
    }
  } catch (err) {
    return rejectWithValue('网络错误');
  }
});

export const restoreSessionThunk = createAsyncThunk<boolean, void, { rejectValue: string }>(
  'login/restoreSession',
  async (_, { rejectWithValue }) => {
    try {
      const res = await checkSession();
      return res.data.result === '登录有效';
    } catch (err) {
      return rejectWithValue('网络错误');
    }
  }
);

export const loginSlice = createSlice({
  name: 'login',
  initialState,
  extraReducers: (builder) => {
    builder
      .addCase(checkLoginThunk.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(checkLoginThunk.fulfilled, (state, action) => {
        state.loading = false;
        state.isLoggedIn = true;
      })
      .addCase(checkLoginThunk.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload ?? null;
      })
      .addCase(restoreSessionThunk.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(restoreSessionThunk.fulfilled, (state, action) => {
        state.loading = false;
        state.isLoggedIn = action.payload;
      })
      .addCase(restoreSessionThunk.rejected, (state) => {
        state.loading = false;
        state.isLoggedIn = false;
      });
  },
  reducers: {
    logIn(state) {
      state.isLoggedIn = true;
    },
    logOut(state) {
      state.isLoggedIn = false;
    }
  }
});

export const { logIn, logOut } = loginSlice.actions;

export const selectIsLoggedIn = (state: RootState) => state.login.isLoggedIn;
