import { defineConfig } from 'vitest/config';
import path from 'node:path';

export default defineConfig({
  cacheDir: '.vite-vitest',
  resolve: {
    dedupe: ['react', 'react-dom', 'prism-react-renderer'],
  },
  test: {
    environment: 'jsdom',
    include: ['src/**/*.test.ts', 'src/**/*.test.tsx'],
    setupFiles: [],
  },
});
