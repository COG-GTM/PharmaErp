/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

const backend = process.env.VITE_BACKEND_URL ?? 'http://localhost:8090';
const proxied = ['/api', '/login', '/token', '/actuator'];

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: Object.fromEntries(proxied.map((path) => [path, { target: backend, changeOrigin: true }])),
  },
  test: {
    globals: true,
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
    restoreMocks: true,
  },
});
