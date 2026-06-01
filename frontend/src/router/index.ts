import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';

import { useAuthStore } from '@/stores/auth';

const GroupDetailPage = () => import('@/pages/GroupDetailPage.vue');
const JoinInvitationPage = () => import('@/pages/JoinInvitationPage.vue');
const LoginPage = () => import('@/pages/LoginPage.vue');
const MailDraftPage = () => import('@/pages/MailDraftPage.vue');
const NotificationPage = () => import('@/pages/NotificationPage.vue');
const OperationLogPage = () => import('@/pages/OperationLogPage.vue');
const PackageCheckPage = () => import('@/pages/PackageCheckPage.vue');
const PackageExportPage = () => import('@/pages/PackageExportPage.vue');
const ProgressHomePage = () => import('@/pages/ProgressHomePage.vue');
const ProjectWorkspacePage = () => import('@/pages/ProjectWorkspacePage.vue');
const RegisterPage = () => import('@/pages/RegisterPage.vue');
const UserProfilePage = () => import('@/pages/UserProfilePage.vue');

const routes: RouteRecordRaw[] = [
  { path: '/', name: 'home', component: ProgressHomePage, meta: { requiresAuth: true } },
  { path: '/login', name: 'login', component: LoginPage },
  { path: '/register', name: 'register', component: RegisterPage },
  { path: '/join/:code', name: 'join-invitation', component: JoinInvitationPage },
  { path: '/groups/:groupId', name: 'group-detail', component: GroupDetailPage, meta: { requiresAuth: true } },
  { path: '/projects/:projectId', name: 'project-workspace', component: ProjectWorkspacePage, meta: { requiresAuth: true } },
  { path: '/projects/:projectId/package/check', name: 'package-check', component: PackageCheckPage, meta: { requiresAuth: true } },
  { path: '/projects/:projectId/package/export', name: 'package-export', component: PackageExportPage, meta: { requiresAuth: true } },
  { path: '/mail-drafts', name: 'mail-draft-overview', component: MailDraftPage, meta: { requiresAuth: true } },
  { path: '/projects/:projectId/mail', name: 'mail-draft', component: MailDraftPage, meta: { requiresAuth: true } },
  { path: '/projects/:projectId/logs', name: 'operation-logs', component: OperationLogPage, meta: { requiresAuth: true } },
  { path: '/notifications', name: 'notifications', component: NotificationPage, meta: { requiresAuth: true } },
  { path: '/profile', name: 'profile', component: UserProfilePage, meta: { requiresAuth: true } },
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

  return true;
});
