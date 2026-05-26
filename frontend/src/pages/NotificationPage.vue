<template>
  <MainLayout>
    <template #title>
      <span>通知中心</span>
    </template>

    <template #actions>
      <el-button :icon="Refresh" :loading="loading" type="primary" @click="loadNotifications">
        刷新通知
      </el-button>
    </template>

    <template #aside>
      <el-descriptions :column="1" size="small" border>
        <el-descriptions-item label="未读">
          {{ unreadCount }}
        </el-descriptions-item>
        <el-descriptions-item label="总数">
          {{ notifications.length }}
        </el-descriptions-item>
      </el-descriptions>
    </template>

    <section class="notification-page">
      <el-card class="panel" shadow="never">
        <el-form :model="filters" class="filters" label-width="72px">
          <el-form-item label="项目">
            <el-input v-model.trim="filters.projectId" clearable placeholder="项目 ID" />
          </el-form-item>

          <el-form-item label="状态">
            <el-segmented v-model="filters.readState" :options="readOptions" />
          </el-form-item>
        </el-form>
      </el-card>

      <el-card class="panel" shadow="never">
        <el-table v-loading="loading" :data="notifications" border>
          <el-table-column label="状态" min-width="90">
            <template #default="{ row }: { row: Notification }">
              <el-tag :type="row.readAt ? 'info' : 'danger'">
                {{ row.readAt ? '已读' : '未读' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="项目" min-width="100" prop="projectId" />
          <el-table-column label="标题" min-width="180" prop="title" show-overflow-tooltip />
          <el-table-column label="内容" min-width="280" prop="content" show-overflow-tooltip />
          <el-table-column label="时间" min-width="170" prop="createdAt" />
          <el-table-column label="操作" fixed="right" min-width="120">
            <template #default="{ row }: { row: Notification }">
              <el-button
                :disabled="Boolean(row.readAt)"
                :loading="markingId === row.id"
                size="small"
                type="primary"
                @click="handleMarkRead(row)"
              >
                标记已读
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </section>
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';

import MainLayout from '@/layouts/MainLayout.vue';
import { listNotifications, markNotificationRead } from '@/services/activityApi';
import { useAuthStore } from '@/stores/auth';

import type { Notification } from '@/types/project';

const authStore = useAuthStore();

const loading = ref(false);
const markingId = ref('');
const notifications = ref<Notification[]>([]);
const filters = reactive({
  projectId: '',
  readState: 'all' as 'all' | 'unread' | 'read',
});

const readOptions = [
  { label: '全部', value: 'all' },
  { label: '未读', value: 'unread' },
  { label: '已读', value: 'read' },
];

const unreadCount = computed(() => notifications.value.filter((item) => !item.readAt).length);

/**
 * 根据页面状态转换为后端查询参数。
 *
 * @returns 已读筛选值，全部时返回 undefined
 */
function readQueryValue(): boolean | undefined {
  if (filters.readState === 'read') {
    return true;
  }

  if (filters.readState === 'unread') {
    return false;
  }

  return undefined;
}

/**
 * 按当前筛选条件加载通知列表。
 */
async function loadNotifications(): Promise<void> {
  loading.value = true;

  try {
    const result = await listNotifications({
      userId: authStore.currentUser?.id,
      projectId: filters.projectId || undefined,
      read: readQueryValue(),
    });
    notifications.value = result.notifications;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '通知加载失败');
  } finally {
    loading.value = false;
  }
}

/**
 * 标记单条通知为已读，并在当前列表中局部更新。
 *
 * @param notification 待标记通知
 */
async function handleMarkRead(notification: Notification): Promise<void> {
  markingId.value = notification.id;

  try {
    const updated = await markNotificationRead(notification.id, { userId: authStore.currentUser?.id });
    notifications.value = notifications.value.map((item) => (item.id === updated.id ? updated : item));
    ElMessage.success('已标记为已读');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '标记已读失败');
  } finally {
    markingId.value = '';
  }
}

watch(
  () => [filters.projectId, filters.readState],
  () => {
    void loadNotifications();
  },
);

onMounted(loadNotifications);
</script>

<style scoped>
.notification-page {
  display: grid;
  gap: 18px;
}

.panel {
  border-radius: 8px;
}

.filters {
  display: grid;
  grid-template-columns: minmax(220px, 320px) minmax(220px, 1fr);
  gap: 12px;
}

.filters :deep(.el-form-item) {
  margin-bottom: 0;
}

@media (max-width: 768px) {
  .filters {
    grid-template-columns: 1fr;
  }
}
</style>
