import { defineConfig } from "vite";
import react from "@vitejs/plugin-react-swc";
import path from "node:path";

const dashboardApiTarget = process.env.VITE_DASHBOARD_API_TARGET ?? "http://127.0.0.1:21100";

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src")
    }
  },
  build: {
    sourcemap: false,
    emptyOutDir: true
  },
  server: {
    host: "127.0.0.1",
    port: 5173,
    strictPort: true,
    proxy: {
      "/api": {
        target: dashboardApiTarget,
        changeOrigin: false
      }
    }
  }
});
