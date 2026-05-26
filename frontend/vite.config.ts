import { fileURLToPath, URL } from 'node:url';

import vue from '@vitejs/plugin-vue';
import { defineConfig, loadEnv } from 'vite';

/**
 * 创建 Vite 配置。
 *
 * @param mode 当前运行模式，用于读取对应环境变量
 * @returns Vite 编译、开发代理和测试配置
 */
export default defineConfig(({ mode }) => {
  // 读取 VITE_API_BASE_URL，避免在代码中硬编码后端生产地址。
  const env = loadEnv(mode, process.cwd(), '');
  const apiBaseUrl = env.VITE_API_BASE_URL || 'http://localhost:8080';

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },
    server: {
      proxy: {
        // /api 代理用于本地联调，后端真实地址来自环境变量。
        '/api': {
          target: apiBaseUrl,
          changeOrigin: true,
          // 本地开发时前端统一请求 /api/*，转发到后端前需移除 /api 前缀。
          rewrite: (path) => path.replace(/^\/api/, ''),
        },
      },
    },
    test: {
      environment: 'jsdom',
      globals: true,
      exclude: ['node_modules/**', 'dist/**', 'e2e/**'],
    },
  };
});
