import instance from '../../API/axios';
import { pullGeneralSettings, updateGeneralSettings } from '../../API/settings';

jest.mock('../../API/axios', () => ({
  __esModule: true,
  default: { get: jest.fn(), post: jest.fn(), put: jest.fn() },
}));

const mockGet = instance.get as jest.Mock;
const mockPut = instance.put as jest.Mock;

beforeEach(() => {
  jest.clearAllMocks();
});

describe('settings API', () => {
  it('pullGeneralSettings gets /settings/general', async () => {
    mockGet.mockResolvedValue({ data: { defaultPage: 3 } });
    await expect(pullGeneralSettings()).resolves.toEqual({ defaultPage: 3 });
    expect(mockGet).toHaveBeenCalledWith('/settings/general');
  });

  it('updateGeneralSettings puts the default page', async () => {
    mockPut.mockResolvedValue({ data: { defaultPage: 2 } });
    await expect(updateGeneralSettings(2)).resolves.toEqual({ defaultPage: 2 });
    expect(mockPut).toHaveBeenCalledWith('/settings/general', { defaultPage: 2 });
  });
});
