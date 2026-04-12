import { defineConfig } from 'vitest/config';

export default defineConfig({
  cacheDir: '.vite-vitest',
  test: {
    environment: 'jsdom',
    include: ['src/**/*.test.{ts,tsx}'],
    setupFiles: ['./src/test/setup.ts'],
    globals: true,
  },
});
