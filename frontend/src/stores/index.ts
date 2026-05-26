import { createPinia } from 'pinia';

/**
 * 创建前端全局状态容器。
 *
 * @returns Pinia 实例
 */
export function createApplicationStore() {
  return createPinia();
}
