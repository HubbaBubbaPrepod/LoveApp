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
        target: 'http://168.222.193.34:3005',
        changeOrigin: true,
      },
      '/uploads': {
        target: 'http://168.222.193.34:3005',
        changeOrigin: true,
      },
    },
  },
})
