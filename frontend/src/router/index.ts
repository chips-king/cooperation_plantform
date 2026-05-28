import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';

import { useAuthStore } from '@/stores/auth';

const GroupDetailPage = () => import('@/pages/GroupDetailPage.vue');
const LoginPage = () => import('@/pages/LoginPage.vue');
const MailDraftPage = () => import('@/pages/MailDraftPage.vue');
const NotificationPage = () => import('@/pages/NotificationPage.vue');
const OperationLogPage = () => import('@/pages/OperationLogPage.vue');
const PackageCheckPage = () => import('@/pages/PackageCheckPage.vue');
const PackageExportPage = () => import('@/pages/PackageExportPage.vue');
const ProgressHomePage = () => import('@/pages/ProgressHomePage.vue');
const ProgressPage = () => import('@/pages/ProgressPage.vue');
const ProjectWorkspacePage = () => import('@/pages/ProjectWorkspacePage.vue');

const routes: RouteRecordRaw[] = [
  { path: '/', name: 'home', component: ProgressHomePage, meta: { requiresAuth: true } },
  { path: '/login', name: 'login', component: LoginPage },
  { path: '/groups/:groupId', name: 'group-detail', component: GroupDetailPage, meta: { requiresAuth: true } },
  { path: '/projects/:projectId', name: 'project-workspace', component: ProjectWorkspacePage, meta: { requiresAuth: true } },
  { path: '/projects/:projectId/progress', name: 'project-progress', component: ProgressPage, meta: { requiresAuth: true } },
  { path: '/projects/:projectId/package/check', name: 'package-check', component: PackageCheckPage, meta: { requiresAuth: true } },
  { path: '/projects/:projectId/package/export', name: 'package-export', component: PackageExportPage, meta: { requiresAuth: true } },
  { path: '/mail-drafts', name: 'mail-draft-overview', component: MailDraftPage, meta: { requiresAuth: true } },
  { path: '/projects/:projectId/mail', name: 'mail-draft', component: MailDraftPage, meta: { requiresAuth: true } },
  { path: '/projects/:projectId/logs', name: 'operation-logs', component: OperationLogPage, meta: { requiresAuth: true } },
  { path: '/notifications', name: 'notifications', component: NotificationPage, meta: { requiresAuth: true } },
];

export const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to) => {
  const authStore = useAuthStore();

  if (to.meta.requiresAuth && !authStore.accessToken) {
    return { name: 'login', query: { redirect: to.fullPath } };
  }

  if (to.name === 'login' && authStore.accessToken) {
    return { name: 'home' };
  }

  return true;
});
