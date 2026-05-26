import { defineConfig, devices } from '@playwright/test';

/**
 * Playwright 端到端测试配置。
 */
export default defineConfig({
  // testDir 指定端到端测试文件目录。
  testDir: './e2e',
  // timeout 限制单个测试最长 30 秒，避免流程异常时长时间挂起。
  timeout: 30_000,
  // fullyParallel 允许互不依赖的 E2E 测试并行执行。
  fullyParallel: true,
  // retries 本地默认不重试，失败时直接暴露问题。
  retries: 0,
  // reporter 使用列表输出，方便在终端快速定位失败用例。
  reporter: 'list',
  // use 配置浏览器上下文默认行为。
  use: {
    // baseURL 指向本地 Vite 服务，端口需与 webServer 保持一致。
    baseURL: 'http://127.0.0.1:5173',
    // channel 使用本机 Chrome，避免依赖 Playwright 内置浏览器下载。
    channel: 'chrome',
    // trace 失败时保留追踪信息，便于定位交互问题。
    trace: 'retain-on-failure',
  },
  // projects 定义桌面浏览器视口，后续可扩展移动端。
  projects: [
    {
      // name 标识当前桌面 Chrome 项目。
      name: 'chromium-desktop',
      // use 继承 Playwright 桌面 Chrome 设备参数。
      use: { ...devices['Desktop Chrome'] },
    },
  ],
  // webServer 在测试前启动 Vite，本地已有服务时复用。
  webServer: {
    // command 启动前端开发服务器。
    command: 'npm run dev -- --host 127.0.0.1',
    // url 用于等待服务可访问。
    url: 'http://127.0.0.1:5173',
    // reuseExistingServer 本地开发时复用已启动的服务。
    reuseExistingServer: true,
    // timeout 等待服务启动的最长时间。
    timeout: 60_000,
  },
});
