<template>
  <MainLayout>
    <template #title>
      <span>目录进度</span>
    </template>

    <template #actions>
      <el-button :icon="Refresh" :loading="loading" @click="loadProgress">刷新</el-button>
    </template>

    <section class="progress-page">
      <el-alert
        v-if="!projectId"
        type="warning"
        :closable="false"
        show-icon
        title="未识别到当前项目，请从项目工作台进入进度页面。"
      />

      <div class="progress-page__summary">
        <div>
          <h2>按目录维护协作状态</h2>
          <p>目录状态用于协作提示，不会锁定文件上传或修改。</p>
        </div>
        <el-progress
          type="dashboard"
          :percentage="completedPercentage"
          :width="116"
          :stroke-width="10"
        />
      </div>

      <el-table v-loading="loading" :data="directories" empty-text="暂无目录进度">
        <el-table-column prop="name" label="目录" min-width="180" show-overflow-tooltip />
        <el-table-column label="当前状态" width="130">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">
              {{ row.statusDisplayName || statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="切换状态" min-width="240">
          <template #default="{ row }">
            <el-radio-group
              :model-value="row.status"
              :disabled="updatingDirectoryId === row.directoryId"
              @change="handleStatusRadioChange(row.directoryId, $event)"
            >
              <el-radio-button label="未开始" value="not_started">未开始</el-radio-button>
              <el-radio-button label="进行中" value="in_progress">进行中</el-radio-button>
              <el-radio-button label="已完成" value="completed">已完成</el-radio-button>
            </el-radio-group>
          </template>
        </el-table-column>
        <el-table-column label="最近更新" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.updatedAt) }}
          </template>
        </el-table-column>
      </el-table>
    </section>
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';

import MainLayout from '@/layouts/MainLayout.vue';
import { getProjectProgress, updateDirectoryStatus } from '@/services/fileApi';
import { useProjectStore } from '@/stores/project';

import type { DirectoryProgress, DirectoryStatus } from '@/types/project';

const route = useRoute();
const projectStore = useProjectStore();

const loading = ref(false);
const directories = ref<DirectoryProgress[]>([]);
const totalDirectoryCount = ref(0);
const completedDirectoryCount = ref(0);
const updatingDirectoryId = ref('');

const projectId = computed(() => {
  const routeProjectId = route.params.projectId;
  return String(Array.isArray(routeProjectId) ? routeProjectId[0] : routeProjectId || projectStore.currentProject?.id || '');
});

const completedPercentage = computed(() => {
  if (!totalDirectoryCount.value) {
    return 0;
  }

  return Math.round((completedDirectoryCount.value / totalDirectoryCount.value) * 100);
});

onMounted(() => {
  void loadProgress();
});

/**
 * 查询项目目录进度。
 */
async function loadProgress(): Promise<void> {
  if (!projectId.value) {
    directories.value = [];
    totalDirectoryCount.value = 0;
    completedDirectoryCount.value = 0;
    return;
  }

  loading.value = true;

  try {
    const response = await getProjectProgress(projectId.value);
    directories.value = response.directories;
    totalDirectoryCount.value = response.totalDirectoryCount;
    completedDirectoryCount.value = response.completedDirectoryCount;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '目录进度加载失败');
  } finally {
    loading.value = false;
  }
}

/**
 * 更新目录状态并同步页面行数据。
 *
 * @param directoryId 目录标识
 * @param status 目标状态
 */
async function handleStatusChange(directoryId: string, status: DirectoryStatus): Promise<void> {
  updatingDirectoryId.value = directoryId;

  try {
    const response = await updateDirectoryStatus({
      projectId: projectId.value,
      directoryId,
      status,
    });

    // 局部更新后重新计算完成数量，避免一次状态切换触发整页刷新。
    directories.value = directories.value.map((directory) => {
      if (directory.directoryId !== directoryId) {
        return directory;
      }

      return {
        ...directory,
        status: response.status,
        statusDisplayName: response.statusDisplayName,
        updatedAt: new Date().toISOString(),
      };
    });
    completedDirectoryCount.value = directories.value.filter((directory) => directory.status === 'completed').length;
    totalDirectoryCount.value = directories.value.length;
    ElMessage.success('目录状态已更新');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '目录状态更新失败');
  } finally {
    updatingDirectoryId.value = '';
  }
}

/**
 * 接收 Element Plus 单选组事件并收窄为目录状态。
 *
 * @param directoryId 目录标识
 * @param value 单选组事件值
 */
function handleStatusRadioChange(directoryId: string, value: string | number | boolean | undefined): void {
  if (value === 'not_started' || value === 'in_progress' || value === 'completed') {
    void handleStatusChange(directoryId, value);
  }
}

/**
 * 获取状态标签样式。
 *
 * @param status 目录状态
 * @returns Element Plus 标签类型
 */
function statusTagType(status: DirectoryStatus): 'info' | 'warning' | 'success' {
  const tagTypes: Record<DirectoryStatus, 'info' | 'warning' | 'success'> = {
    not_started: 'info',
    in_progress: 'warning',
    completed: 'success',
  };

  return tagTypes[status];
}

/**
 * 转换目录状态中文文案。
 *
 * @param status 目录状态
 * @returns 中文状态文案
 */
function statusText(status: DirectoryStatus): string {
  const statusMap: Record<DirectoryStatus, string> = {
    not_started: '未开始',
    in_progress: '进行中',
    completed: '已完成',
  };

  return statusMap[status];
}

/**
 * 格式化页面展示时间。
 *
 * @param value ISO 时间字符串
 * @returns 本地化时间文本
 */
function formatDateTime(value: string): string {
  if (!value) {
    return '-';
  }

  return new Date(value).toLocaleString();
}
</script>

<style scoped>
.progress-page {
  display: grid;
  gap: 18px;
}

.progress-page__summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 20px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #ffffff;
}

.progress-page__summary h2 {
  margin: 0 0 8px;
  font-size: 20px;
}

.progress-page__summary p {
  margin: 0;
  color: #687386;
}

@media (max-width: 720px) {
  .progress-page__summary {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
