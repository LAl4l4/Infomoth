import type { DisplaySettingsData } from './customTypes';

export const DEFAULT_DISPLAY_SETTINGS: DisplaySettingsData = {
  backgroundColor: '#0C101C',
  globeGlowColor: '#00FFC6',
  globePointColor: '#FFFFFF',
  globeMarkerColor: '#00E5FF',
};

export function hexToRgb(color: string): [number, number, number] {
  const normalized = /^#[0-9A-F]{6}$/i.test(color) ? color : '#FFFFFF';
  return [
    Number.parseInt(normalized.slice(1, 3), 16) / 255,
    Number.parseInt(normalized.slice(3, 5), 16) / 255,
    Number.parseInt(normalized.slice(5, 7), 16) / 255,
  ];
}
