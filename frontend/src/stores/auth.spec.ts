import { createPinia, setActivePinia } from 'pinia';
import { beforeEach, describe, expect, it } from 'vitest';

import { useAuthStore } from './auth';

/**
 * 鉴权 Store 测试，覆盖刷新页面后的会话恢复能力。
 */
describe('auth store', () => {
  beforeEach(() => {
    window.localStorage.clear();
    setActivePinia(createPinia());
  });

  it('写入登录态时同步持久化用户、令牌和权限', () => {
    const store = useAuthStore();

    store.setSession(
      { id: 1, displayName: '管理员', email: 'admin@example.com' },
      'signed-test-token',
      ['project.view'],
    );

    expect(window.localStorage.getItem('access_token')).toBe('signed-test-token');
    expect(window.localStorage.getItem('current_user')).toContain('admin@example.com');
    expect(window.localStorage.getItem('permissions')).toContain('project.view');
  });
});
