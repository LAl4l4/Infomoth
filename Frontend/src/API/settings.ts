import instance from './axios';
import type { GeneralSettingsData } from '../customTypes';

export async function pullGeneralSettings(): Promise<GeneralSettingsData> {
  const response = await instance.get<GeneralSettingsData>('/settings/general');
  return response.data;
}

export async function updateGeneralSettings(defaultPage: number): Promise<GeneralSettingsData> {
  const response = await instance.put<GeneralSettingsData>('/settings/general', { defaultPage });
  return response.data;
}
