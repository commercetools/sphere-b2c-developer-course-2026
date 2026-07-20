import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

// Commerce Canvas talks ONLY to the BFF training API and holds no commercetools credentials.
// In dev, /api is proxied to the BFF origin (default :8081) so there is no CORS and the same UI
// works against the Java or the NestJS BFF unchanged.
const BFF_ORIGIN = process.env.VITE_BFF_ORIGIN || 'http://localhost:8081';

const proxy = { '/api': { target: BFF_ORIGIN, changeOrigin: true } };

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: { port: 5173, proxy },
  preview: { port: 5173, proxy },
});
