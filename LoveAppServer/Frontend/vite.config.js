import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  build: {
    outDir: '../Backend/dist',
    emptyOutDir: true,
  },
  server: {
    port: 5174,
    proxy: {
      '/api': {
        target: 'http://195.2.71.218:3005',
        changeOrigin: true,
      },
      '/uploads': {
        target: 'http://195.2.71.218:3005',
        changeOrigin: true,
      },
    },
  },
})
