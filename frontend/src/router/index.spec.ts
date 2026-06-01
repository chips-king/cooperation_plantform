import { describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';

vi.mock('@/pages/LoginPage.vue', () => ({ default: { template: '<div />' } }));
vi.mock('@/pages/ProgressHomePage.vue', () => ({ default: { template: '<div />' } }));

import { router } from './index';
import { useAuthStore } from '@/stores/auth';

describe('router', () => {
  it('不再注册任务进度页面路由', () => {
    const routes = router.getRoutes();

    expect(routes.some((route) => route.name === 'project-progress')).toBe(false);
    expect(routes.some((route) => route.path === '/projects/:projectId/progress')).toBe(false);
  });

  it('已有本地令牌时仍允许进入登录页重新登录', async () => {
    setActivePinia(createPinia());
    useAuthStore().setSession(
      { id: 1, displayName: '管理员', email: 'admin@example.com' },
      'old-token',
    );

    await router.push('/login');

    expect(router.currentRoute.value.name).toBe('login');
  });
});
