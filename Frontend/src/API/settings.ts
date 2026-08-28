import instance from './axios';
import type { DisplaySettingsData, GeneralSettingsData } from '../customTypes';

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

export async function pullDisplaySettings(): Promise<DisplaySettingsData> {
  const response = await instance.get<DisplaySettingsData>('/settings/display');
  return response.data;
}

export async function updateDisplaySettings(
  settings: DisplaySettingsData
): Promise<DisplaySettingsData> {
  const response = await instance.put<DisplaySettingsData>('/settings/display', settings);
  return response.data;
}
