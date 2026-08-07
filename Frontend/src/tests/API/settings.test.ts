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
    const settings = {
      defaultPage: 3,
      defaultBaseCurrency: 'USD',
      defaultQuoteCurrency: 'CNY',
    };
    mockGet.mockResolvedValue({ data: settings });
    await expect(pullGeneralSettings()).resolves.toEqual(settings);
    expect(mockGet).toHaveBeenCalledWith('/settings/general');
  });

  it('updateGeneralSettings puts the default page', async () => {
    const settings = {
      defaultPage: 2,
      defaultBaseCurrency: 'EUR',
      defaultQuoteCurrency: 'AUD',
    };
    mockPut.mockResolvedValue({ data: settings });
    await expect(updateGeneralSettings(2, 'EUR', 'AUD')).resolves.toEqual(settings);
    expect(mockPut).toHaveBeenCalledWith('/settings/general', {
      defaultPage: 2,
      defaultBaseCurrency: 'EUR',
      defaultQuoteCurrency: 'AUD',
    });
  });
});
