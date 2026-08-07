import instance from './axios';
import type { GeneralSettingsData } from '../customTypes';

export async function pullGeneralSettings(): Promise<GeneralSettingsData> {
  const response = await instance.get<GeneralSettingsData>('/settings/general');
  return response.data;
}

export async function updateGeneralSettings(
  defaultPage: number,
  defaultBaseCurrency: string = 'USD',
  defaultQuoteCurrency: string = 'CNY'
): Promise<GeneralSettingsData> {
  const response = await instance.put<GeneralSettingsData>('/settings/general', {
    defaultPage,
    defaultBaseCurrency,
    defaultQuoteCurrency,
  });
  return response.data;
}
