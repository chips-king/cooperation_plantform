<template>
  <MainLayout>
    <template #title>
      <span>任务进度</span>
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

      <div class="kanban">
        <div class="kanban-column">
          <div class="kanban-header kanban-header--not-started">
            <span>未开始</span>
            <el-tag type="info" size="small">{{ notStartedDirs.length }}</el-tag>
          </div>
          <div class="kanban-cards">
            <div
              v-for="dir in notStartedDirs"
              :key="dir.directoryId"
              class="kanban-card"
            >
              <div class="kanban-card__name">{{ dir.name }}</div>
              <div class="kanban-card__meta">
                <span class="kanban-card__files">{{ dir.fileCount }} 个文件</span>
                <span class="kanban-card__time">{{ formatDateTime(dir.updatedAt) }}</span>
              </div>
            </div>
            <el-empty v-if="notStartedDirs.length === 0" description="暂无" :image-size="64" />
          </div>
        </div>

        <div class="kanban-column">
          <div class="kanban-header kanban-header--in-progress">
            <span>进行中</span>
            <el-tag type="warning" size="small">{{ inProgressDirs.length }}</el-tag>
          </div>
          <div class="kanban-cards">
            <div
              v-for="dir in inProgressDirs"
              :key="dir.directoryId"
              class="kanban-card"
            >
              <div class="kanban-card__name">{{ dir.name }}</div>
              <div class="kanban-card__meta">
                <span class="kanban-card__files">{{ dir.fileCount }} 个文件</span>
                <span class="kanban-card__time">{{ formatDateTime(dir.updatedAt) }}</span>
              </div>
            </div>
            <el-empty v-if="inProgressDirs.length === 0" description="暂无" :image-size="64" />
          </div>
        </div>

        <div class="kanban-column">
          <div class="kanban-header kanban-header--completed">
            <span>已完成</span>
            <el-tag type="success" size="small">{{ completedDirs.length }}</el-tag>
          </div>
          <div class="kanban-cards">
            <div
              v-for="dir in completedDirs"
              :key="dir.directoryId"
              class="kanban-card kanban-card--completed"
            >
              <div class="kanban-card__name">{{ dir.name }}</div>
              <div class="kanban-card__meta">
                <span class="kanban-card__files">{{ dir.fileCount }} 个文件</span>
                <span class="kanban-card__time">{{ formatDateTime(dir.updatedAt) }}</span>
              </div>
              <el-tag v-if="dir.mailSent" type="success" size="small" class="kanban-card__badge">
                已发邮件
              </el-tag>
            </div>
            <el-empty v-if="completedDirs.length === 0" description="暂无" :image-size="64" />
          </div>
        </div>
      </div>
    </section>
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';

import MainLayout from '@/layouts/MainLayout.vue';
import { getProjectProgress } from '@/services/fileApi';
import { useProjectStore } from '@/stores/project';

import type { DirectoryProgress, DirectoryStatus } from '@/types/project';

const route = useRoute();
const projectStore = useProjectStore();

const loading = ref(false);
const directories = ref<DirectoryProgress[]>([]);

const projectId = computed(() => {
  const routeProjectId = route.params.projectId;
  return String(Array.isArray(routeProjectId) ? routeProjectId[0] : routeProjectId || projectStore.currentProject?.id || '');
});

/**
 * 根据文件数量和邮件发送状态自动判断目录进度状态。
 *
 * @param dir 目录进度数据
 * @returns 自动推断后的状态
 */
function autoStatus(dir: DirectoryProgress): DirectoryStatus {
  if (dir.mailSent) return 'completed';
  if (dir.fileCount === 0) return 'not_started';
  return 'in_progress';
}

const notStartedDirs = computed(() =>
  directories.value.filter((dir) => autoStatus(dir) === 'not_started'),
);

const inProgressDirs = computed(() =>
  directories.value.filter((dir) => autoStatus(dir) === 'in_progress'),
);

const completedDirs = computed(() =>
  directories.value.filter((dir) => autoStatus(dir) === 'completed'),
);

onMounted(() => {
  void loadProgress();
});

/**
 * 查询项目目录进度。
 */
async function loadProgress(): Promise<void> {
  if (!projectId.value) {
    directories.value = [];
    return;
  }

  loading.value = true;

  try {
    const response = await getProjectProgress(projectId.value);
    directories.value = response.directories;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '目录进度加载失败');
  } finally {
    loading.value = false;
  }
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

.kanban {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.kanban-column {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #f5f7fa;
  border-radius: 12px;
  padding: 16px;
  min-height: 300px;
}

.kanban-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 15px;
  font-weight: 600;
  padding-bottom: 8px;
  border-bottom: 2px solid;
}

.kanban-header--not-started {
  color: #606266;
  border-color: #909399;
}

.kanban-header--in-progress {
  color: #e6a23c;
  border-color: #e6a23c;
}

.kanban-header--completed {
  color: #67c23a;
  border-color: #67c23a;
}

.kanban-cards {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
}

.kanban-card {
  background: #ffffff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 14px;
  transition: box-shadow 0.15s ease;
}

.kanban-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.kanban-card--completed {
  border-color: #b3e19d;
  background: #f6fff2;
}

.kanban-card__name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.kanban-card__meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #909399;
}

.kanban-card__files {
  color: #606266;
}

.kanban-card__badge {
  margin-top: 8px;
}

@media (max-width: 960px) {
  .kanban {
    grid-template-columns: 1fr;
  }
}
</style>
