import { createSlice, createAsyncThunk, type PayloadAction } from '@reduxjs/toolkit';
import { pullProfiles } from '../API/prof';
import { logOut } from './login';
import type { RootState, UserData, ProfileData } from '../customTypes';

const initialUserData: UserData = {
  bio: '',
  avatar: '',
  birthday: '',
  gender: '',
};

interface ProfileState {
  userData: UserData;
  loading: boolean;
  error: string | null;
}

const initialState: ProfileState = {
  userData: initialUserData,
  loading: false,
  error: null,
};

// 异步action：从数据库获取用户数据
export const getUserProfile = createAsyncThunk<
  ProfileData,
  void,
  { rejectValue: string }
>('profile/getUserProfile', async (_, { rejectWithValue }) => {
  try {
    const response = await pullProfiles();

    //这里做错误handle
    if (!response) {
      return rejectWithValue('Failed to fetch profile data');
    }

    //API已经返回数据
    return response;
  } catch (error) {
    return rejectWithValue(error instanceof Error ? error.message : 'Unknown error');
  }
});

export const profileSlice = createSlice({
  name: 'profile',
  initialState,
  reducers: {
    clearProfile(state) {
      state.userData = { ...initialUserData };
      state.error = null;
    },
    updateUserData(state, action: PayloadAction<Partial<UserData>>) {
      state.userData = { ...state.userData, ...action.payload };
    }
  },
  extraReducers: (builder) => {
    builder
      .addCase(getUserProfile.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(getUserProfile.fulfilled, (state, action) => {
        state.loading = false;
        const { bio, avatarUrl, birthday, gender } = action.payload;
        state.userData = {
          bio,
          avatar: avatarUrl,
          birthday,
          gender
        };
      })
      .addCase(getUserProfile.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload ?? null;
      })
      //监听logout action，清空profile
      .addCase(logOut, (state) => {
        state.userData = { ...initialUserData };
        state.error = null;
      });
  }
});

export const { clearProfile, updateUserData } = profileSlice.actions;

export const selectUserData = (state: RootState) => state.profile.userData;
export const selectProfileLoading = (state: RootState) => state.profile.loading;
export const selectProfileError = (state: RootState) => state.profile.error;
