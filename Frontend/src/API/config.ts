interface RuntimeConfig {
  frontend?: {
    apiBaseUrl?: string;
  };
}

export async function loadApiBaseUrl(): Promise<string> {
  const response = await fetch('/app-config.json');
  if (!response.ok) {
    throw new Error('Cannot load frontend runtime configuration');
  }

  const config = (await response.json()) as RuntimeConfig;
  return config.frontend?.apiBaseUrl?.trim() || window.location.origin;
}
