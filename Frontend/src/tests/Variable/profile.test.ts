import {
  profileSlice,
  clearProfile,
  updateUserData,
  getUserProfile,
  selectUserData,
  selectProfileLoading,
  selectProfileError,
} from '../../Variable/profile';
import { logOut } from '../../Variable/login';
import { pullProfiles } from '../../API/prof';
import { createTestStore } from '../testUtils';
import { RootState, ProfileData } from '../../customTypes';

jest.mock('../../API/prof', () => ({
  pullProfiles: jest.fn(),
}));

const mockPullProfiles = pullProfiles as jest.MockedFunction<typeof pullProfiles>;
const reducer = profileSlice.reducer;

const emptyUserData = { bio: '', avatar: '', birthday: '', gender: '' };

describe('profile slice reducers', () => {
  it('returns initial state', () => {
    expect(reducer(undefined, { type: 'unknown' })).toEqual({
      userData: emptyUserData,
      loading: false,
      error: null,
    });
  });

  it('updateUserData merges partial data', () => {
    const state = reducer(undefined, updateUserData({ bio: 'hello' }));
    expect(state.userData).toEqual({ ...emptyUserData, bio: 'hello' });
  });

  it('clearProfile resets user data', () => {
    const filled = reducer(undefined, updateUserData({ bio: 'x', gender: 'male' }));
    const state = reducer(filled, clearProfile());
    expect(state.userData).toEqual(emptyUserData);
  });

  it('logOut action clears the profile', () => {
    const filled = reducer(undefined, updateUserData({ bio: 'x' }));
    const state = reducer(filled, logOut());
    expect(state.userData).toEqual(emptyUserData);
    expect(state.error).toBeNull();
  });

  it('selectors read from state', () => {
    const state = {
      profile: { userData: { ...emptyUserData, bio: 'b' }, loading: true, error: 'e' },
    } as RootState;
    expect(selectUserData(state).bio).toBe('b');
    expect(selectProfileLoading(state)).toBe(true);
    expect(selectProfileError(state)).toBe('e');
  });
});

describe('getUserProfile thunk', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('fulfills and maps backend payload to userData', async () => {
    const payload: ProfileData = {
      bio: 'bio',
      avatarUrl: 'http://img',
      birthday: '2000-01-01',
      gender: 'female',
    };
    mockPullProfiles.mockResolvedValue(payload);

    const store = createTestStore();
    await store.dispatch(getUserProfile());

    const state = store.getState().profile;
    expect(state.loading).toBe(false);
    expect(state.error).toBeNull();
    expect(state.userData).toEqual({
      bio: 'bio',
      avatar: 'http://img',
      birthday: '2000-01-01',
      gender: 'female',
    });
  });

  it('rejects with error message on failure', async () => {
    mockPullProfiles.mockRejectedValue(new Error('未登录'));

    const store = createTestStore();
    await store.dispatch(getUserProfile());

    const state = store.getState().profile;
    expect(state.loading).toBe(false);
    expect(state.error).toBe('未登录');
  });
});
