import { defineConfig } from 'vitest/config';

export default defineConfig({
  cacheDir: '.vite-vitest',
  test: {
    environment: 'node',
    include: ['src/**/*.test.ts'],
  },
});
