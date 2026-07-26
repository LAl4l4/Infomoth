import { readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';

interface AppConfig {
  frontend?: {
    apiBaseUrl?: string;
  };
}

async function generateRuntimeConfig(): Promise<void> {
  const configPath = path.resolve(process.cwd(), '../Config/app-config.json');
  const outputPath = path.resolve(process.cwd(), 'public/app-config.json');
  const config = JSON.parse(await readFile(configPath, 'utf8')) as AppConfig;

  await writeFile(outputPath, `${JSON.stringify({ frontend: config.frontend }, null, 2)}\n`);
}

void generateRuntimeConfig();
