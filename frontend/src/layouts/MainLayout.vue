<template>
  <SidebarProvider :collapsible="'icon'">
    <Sidebar>
      <SidebarHeader>
        <div class="sidebar-brand">
          <span class="sidebar-brand-mark">协</span>
          <span class="sidebar-brand-name">协作平台</span>
        </div>
      </SidebarHeader>

      <SidebarContent>
        <SidebarGroup>
          <SidebarMenu>
            <SidebarMenuItem>
              <RouterLink v-slot="{ isActive, navigate }" to="/" custom>
                <SidebarMenuButton
                  :is-active="isActive"
                  tooltip="项目总览"
                  @click="navigate"
                >
                  <HomeFilled class="sidebar-icon" />
                  <span>项目总览</span>
                </SidebarMenuButton>
              </RouterLink>
            </SidebarMenuItem>
            <SidebarMenuItem>
              <RouterLink v-slot="{ isActive, navigate }" to="/notifications" custom>
                <SidebarMenuButton
                  :is-active="isActive"
                  tooltip="通知中心"
                  @click="navigate"
                >
                  <Bell class="sidebar-icon" />
                  <span>通知中心</span>
                </SidebarMenuButton>
              </RouterLink>
            </SidebarMenuItem>
            <SidebarMenuItem>
              <RouterLink v-slot="{ isActive, navigate }" to="/mail-drafts" custom>
                <SidebarMenuButton
                  :is-active="isActive || route.name === 'mail-draft'"
                  tooltip="邮件草稿"
                  @click="navigate"
                >
                  <Message class="sidebar-icon" />
                  <span>邮件草稿</span>
                </SidebarMenuButton>
              </RouterLink>
            </SidebarMenuItem>
          </SidebarMenu>
        </SidebarGroup>

        <template v-if="projectNav.length > 0">
          <SidebarSeparator />

          <SidebarGroup>
            <div class="sidebar-group-header">
              <SidebarGroupLabel>项目导航</SidebarGroupLabel>
            </div>
            <SidebarMenu>
              <SidebarMenuItem v-for="item in projectNav" :key="item.to">
                <RouterLink v-slot="{ isActive, navigate }" :to="item.to" custom>
                  <SidebarMenuButton
                    :is-active="isActive"
                    :tooltip="item.label"
                    @click="navigate"
                  >
                    <component :is="item.icon" class="sidebar-icon" />
                    <span>{{ item.label }}</span>
                  </SidebarMenuButton>
                </RouterLink>
              </SidebarMenuItem>
            </SidebarMenu>
          </SidebarGroup>
        </template>
      </SidebarContent>

      <SidebarFooter>
        <slot name="aside" />
      </SidebarFooter>
    </Sidebar>

    <SidebarInset>
      <header class="main-header">
        <div class="main-header__left">
          <SidebarTrigger />
          <el-button
            v-if="canGoBack"
            link
            :icon="ArrowLeft"
            style="margin-right: 8px; font-size: 14px"
            @click="goBack"
          />
          <span class="main-header__title">{{ pageTitle }}</span>
        </div>
        <div class="main-header__right">
          <slot name="actions" />
          <RouterLink class="main-header__notice" to="/notifications">
            <el-badge v-if="unreadCount > 0" :value="unreadCount" :max="99"
              class="main-header__notice-badge">
              <Bell class="main-header__notice-icon" />
            </el-badge>
            <Bell v-else class="main-header__notice-icon" />
          </RouterLink>
        </div>
      </header>

      <main class="main-content">
        <slot />
      </main>
    </SidebarInset>
  </SidebarProvider>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import {
  ArrowLeft,
  Bell,
  HomeFilled,
  Message,
  TrendCharts,
} from '@element-plus/icons-vue';
import { listNotifications } from '@/services/activityApi';
import { useAuthStore } from '@/stores/auth';
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarInset,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarProvider,
  SidebarSeparator,
  SidebarTrigger,
} from '@/components/ui/sidebar';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const unreadCount = ref(0);

async function fetchUnreadCount(): Promise<void> {
  if (!authStore.currentUser?.id) return;
  try {
    const result = await listNotifications({
      userId: authStore.currentUser.id,
      read: false,
    });
    unreadCount.value = result.notifications?.length ?? 0;
  } catch {
    unreadCount.value = 0;
  }
}

watch(() => route.path, () => {
  void fetchUnreadCount();
});

onMounted(() => {
  void fetchUnreadCount();
});

const titleMap: Record<string, string> = {
  home: '项目总览',
  'group-detail': '小组详情',
  'project-workspace': '项目工作台',
  'project-progress': '任务进度',
  'mail-draft': '邮件草稿',
  'mail-draft-overview': '邮件草稿',
  'operation-logs': '操作记录',
  notifications: '通知中心',
};

const pageTitle = computed(() => titleMap[String(route.name ?? '')] || '工作台');

interface NavItem {
  to: string;
  label: string;
  icon: object;
}

const projectNav = computed<NavItem[]>(() => {
  const pid = route.params.projectId as string;
  if (!pid) return [];
  return [
    { to: `/projects/${pid}`, label: '工作台', icon: HomeFilled },
    { to: `/projects/${pid}/progress`, label: '任务进度', icon: TrendCharts },
  ];
});

const backRouteMap: Record<
  string,
  string | ((params: Record<string, string>) => string)
> = {
  'group-detail': '/',
  'project-workspace': '/',
  'project-progress': (p) => `/projects/${p.projectId}`,
  'package-check': (p) => `/projects/${p.projectId}`,
  'package-export': (p) => `/projects/${p.projectId}`,
  'mail-draft': '/mail-drafts',
  'mail-draft-overview': '/',
  'operation-logs': (p) => `/projects/${p.projectId}`,
  notifications: '/',
};

const canGoBack = computed(() => {
  const name = String(route.name ?? '');
  return (
    name !== 'home' &&
    name !== 'login' &&
    name !== '' &&
    backRouteMap[name] !== undefined
  );
});

function goBack(): void {
  const name = String(route.name ?? '');
  const target = backRouteMap[name];
  if (typeof target === 'function') {
    const params: Record<string, string> = {};
    for (const key of Object.keys(route.params)) {
      const val = route.params[key];
      params[key] = Array.isArray(val) ? val[0] : String(val);
    }
    void router.push(target(params));
  } else if (target) {
    void router.push(target);
  } else {
    router.back();
  }
}
</script>

<style>
/* 品牌 */
.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  overflow: hidden;
  white-space: nowrap;
}

.sidebar-brand-mark {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  color: #fff;
  background: linear-gradient(135deg, var(--sidebar-accent), var(--sidebar-accent-hover));
  font-size: 16px;
  font-weight: 700;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(var(--cb-color-primary-rgb), 0.3);
}

.sidebar-brand-name {
  color: var(--sidebar-fg-active);
  font-size: 16px;
  font-weight: 600;
  letter-spacing: -0.02em;
}

/* 图标 */
.sidebar-icon {
  width: 18px;
  height: 18px;
  opacity: 0.7;
  flex-shrink: 0;
}

/* 头部 */
.main-header {
  display: flex;
  align-items: center;
  height: 56px;
  padding: 0 24px;
  background: var(--cb-bg-card);
  border-bottom: 1px solid var(--cb-border);
  flex-shrink: 0;
}

.main-header__left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.main-header__title {
  font-size: 16px;
  font-weight: 600;
  color: var(--cb-text-primary);
}

.main-header__right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
}

.main-header__notice {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 8px;
  color: var(--cb-text-secondary);
  background: transparent;
  transition: all 0.15s ease;
  text-decoration: none;
}

.main-header__notice:hover {
  background: var(--cb-bg-page);
  color: var(--cb-text-primary);
}

.main-header__notice-icon {
  width: 18px;
  height: 18px;
}

/* 内容区 */
.main-content {
  flex: 1;
  padding: 24px 28px;
  overflow: auto;
}
</style>
