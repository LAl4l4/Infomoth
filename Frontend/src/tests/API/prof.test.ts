import instance from '../../API/axios';
import { pullProfiles, updateProfile } from '../../API/prof';

jest.mock('../../API/axios', () => ({
  __esModule: true,
  default: { get: jest.fn(), post: jest.fn(), put: jest.fn() },
}));

const mockGet = instance.get as jest.Mock;
const mockPost = instance.post as jest.Mock;

beforeEach(() => {
  jest.clearAllMocks();
});

describe('pullProfiles', () => {
  it('gets /auth/pullProfiles and returns data', async () => {
    const profile = { bio: 'b', avatarUrl: 'a', birthday: '2000-01-01', gender: 'male' };
    mockGet.mockResolvedValue({ data: profile });
    await expect(pullProfiles()).resolves.toEqual(profile);
    expect(mockGet).toHaveBeenCalledWith('/auth/pullProfiles');
  });

  it('throws when the server returns null', async () => {
    mockGet.mockResolvedValue({ data: null });
    await expect(pullProfiles()).rejects.toThrow('No response from server');
  });
});

describe('updateProfile', () => {
  it('posts the patch payload to /auth/pushProfile', async () => {
    mockPost.mockResolvedValue({ data: '保存成功' });
    await expect(updateProfile({ bio: 'new' })).resolves.toBe('保存成功');
    expect(mockPost).toHaveBeenCalledWith('/auth/pushProfile', { bio: 'new' });
  });

  it('throws on falsy payload', async () => {
    await expect(updateProfile(null as never)).rejects.toThrow('Invalid data');
  });

  it('throws when the server returns null', async () => {
    mockPost.mockResolvedValue({ data: null });
    await expect(updateProfile({ bio: 'x' })).rejects.toThrow('No response from server');
  });
});
