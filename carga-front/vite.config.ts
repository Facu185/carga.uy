import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// En desarrollo, /carga/* se redirige al WildFly local: mismo origen, sin CORS.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/carga': 'http://localhost:8080',
    },
  },
})
