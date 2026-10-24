// Télécharge Chromium dans tools/.browsers/ (≈ 150 Mo, une seule fois).
import './lib/env.mjs';
import { spawnSync } from 'node:child_process';
import { BROWSERS_DIR } from './lib/env.mjs';

console.log(`Navigateurs Playwright → ${BROWSERS_DIR}`);
const r = spawnSync(process.execPath, ['node_modules/playwright/cli.js', 'install', 'chromium'], {
  stdio: 'inherit', env: process.env, cwd: new URL('.', import.meta.url),
});
process.exit(r.status ?? 1);
