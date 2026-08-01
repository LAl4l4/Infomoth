import { loadApiBaseUrl } from '../../API/config';

const mockFetch = jest.fn();
globalThis.fetch = mockFetch as unknown as typeof fetch;

beforeEach(() => {
  jest.clearAllMocks();
});

describe('loadApiBaseUrl', () => {
  it('returns the configured apiBaseUrl', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: async () => ({ frontend: { apiBaseUrl: 'http://api.example.com' } }),
    });
    await expect(loadApiBaseUrl()).resolves.toBe('http://api.example.com');
    expect(mockFetch).toHaveBeenCalledWith('/app-config.json');
  });

  it('falls back to window.location.origin when not configured', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: async () => ({}),
    });
    await expect(loadApiBaseUrl()).resolves.toBe(window.location.origin);
  });

  it('falls back to origin when apiBaseUrl is blank', async () => {
    mockFetch.mockResolvedValue({
      ok: true,
      json: async () => ({ frontend: { apiBaseUrl: '   ' } }),
    });
    await expect(loadApiBaseUrl()).resolves.toBe(window.location.origin);
  });

  it('throws when the config cannot be loaded', async () => {
    mockFetch.mockResolvedValue({ ok: false, json: async () => ({}) });
    await expect(loadApiBaseUrl()).rejects.toThrow(
      'Cannot load frontend runtime configuration'
    );
  });
});
