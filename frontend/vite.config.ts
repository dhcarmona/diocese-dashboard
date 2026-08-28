/// <reference types="vitest/config" />
import { execSync } from 'child_process'
import { readFileSync } from 'fs'
import { resolve } from 'path'
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

function getCommitHash(): string {
  if (process.env.SOURCE_VERSION) {
    return process.env.SOURCE_VERSION.slice(0, 7);
  }
  try {
    return execSync('git rev-parse --short=7 HEAD').toString().trim();
  } catch {
    return 'unknown';
  }
}

function getAppVersion(): string {
  try {
    const pomPath = resolve(new URL('..', import.meta.url).pathname, 'pom.xml');
    const pom = readFileSync(pomPath, 'utf-8');
    // Match the project-level <version>, which appears directly inside <project> before any child elements
    const match = pom.match(/<project[^>]*>[\s\S]*?<version>([^<]+)<\/version>/);
    return match ? match[1] : 'unknown';
  } catch {
    return 'unknown';
  }
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  define: {
    __APP_VERSION__: JSON.stringify(getAppVersion()),
    __BUILD_TIME__: JSON.stringify(new Date().toISOString()),
    __COMMIT_HASH__: JSON.stringify(getCommitHash()),
  },
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    globals: true,
    testTimeout: 15000,
  },
})
