import instance from '../../API/axios';
import {
  pullDisplaySettings,
  pullGeneralSettings,
  updateDisplaySettings,
  updateGeneralSettings,
} from '../../API/settings';

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

  it('pullDisplaySettings gets /settings/display', async () => {
    const settings = {
      backgroundColor: '#0C101C',
      globeGlowColor: '#00FFC6',
      globePointColor: '#FFFFFF',
      globeMarkerColor: '#00E5FF',
    };
    mockGet.mockResolvedValue({ data: settings });

    await expect(pullDisplaySettings()).resolves.toEqual(settings);
    expect(mockGet).toHaveBeenCalledWith('/settings/display');
  });

  it('updateDisplaySettings puts all four colors', async () => {
    const settings = {
      backgroundColor: '#112233',
      globeGlowColor: '#445566',
      globePointColor: '#778899',
      globeMarkerColor: '#AABBCC',
    };
    mockPut.mockResolvedValue({ data: settings });

    await expect(updateDisplaySettings(settings)).resolves.toEqual(settings);
    expect(mockPut).toHaveBeenCalledWith('/settings/display', settings);
  });
});
