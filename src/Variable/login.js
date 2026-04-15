import {createSlice} from '@reduxjs/toolkit';
import { createAsyncThunk } from '@reduxjs/toolkit';
import { checkLogin } from '../API/auth';


export const checkLoginThunk = createAsyncThunk(
  'login/checkLogin',
  async ({ email, password }, { rejectWithValue }) => {
    try {
      const res = await checkLogin(email, password);

      if (res.data.result === '登录成功') {
        return {
          token: res.data.token
        };
      } else {
        return rejectWithValue(res.data.result);
      }
    } catch (err) {
      return rejectWithValue('网络错误');
    }
  }
);

export const loginSlice = createSlice({
    name: 'login',
    initialState: {
        isLoggedIn: false,
        token: null,
        loading: false,
        error: null
    },
    extraReducers: (builder) => {
        builder
        .addCase(checkLoginThunk.pending, (state) => {
            state.loading = true;
            state.error = null;
        })
        .addCase(checkLoginThunk.fulfilled, (state, action) => {
            state.loading = false;
            state.isLoggedIn = true;
            state.token = action.payload.token;
        })
        .addCase(checkLoginThunk.rejected, (state, action) => {
            state.loading = false;
            state.error = action.payload;
        });
    },
    reducers: {
        logIn(state) {
            state.isLoggedIn = true;
        },
        logOut(state) {
            state.isLoggedIn = false;
            state.token = null;
            localStorage.removeItem('authToken');
        },
        storeToken(state, action) {
            state.token = action.payload;
        }
    }
});

export const { logIn, logOut, storeToken } = loginSlice.actions;

export const selectIsLoggedIn = (state) => state.login.isLoggedIn;
export const selectToken = (state) => state.login.token;