import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

const backend = 'http://localhost:8080'

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      // Forward /api/* to the Spring Boot backend, stripping the /api prefix
      '/api': {
        target: backend,
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
      // GitHub OAuth login start + callback are handled by Spring Security.
      // Going through the proxy keeps the session cookie on the frontend's origin.
      '/oauth2': backend,
      '/login/oauth2': backend,
    },
  },
})
