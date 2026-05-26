<template>
  <MainLayout>
    <template #title>
      <span>{{ project?.name || '项目工作台' }}</span>
    </template>

    <template #actions>
      <el-button :icon="Refresh" :loading="loading" @click="loadWorkspace">刷新</el-button>
    </template>

    <template #aside>
      <section class="workspace-page__aside">
        <h2>快捷入口</h2>
        <RouterLink class="workspace-page__aside-link" :to="`/projects/${projectId}/files`">文件管理</RouterLink>
        <RouterLink class="workspace-page__aside-link" :to="`/projects/${projectId}/progress`">任务进度</RouterLink>
        <RouterLink class="workspace-page__aside-link" :to="`/projects/${projectId}/package/check`">打包检查</RouterLink>
        <RouterLink class="workspace-page__aside-link" :to="`/projects/${projectId}/mail`">邮件草稿</RouterLink>
      </section>
    </template>

    <section class="workspace-page">
      <el-alert
        v-if="project?.status === 'ended'"
        title="项目已结束，成员上传、整理和删除等写操作已锁定。"
        type="info"
        show-icon
        :closable="false"
      />
      <el-alert
        v-if="shouldPromptCreateDirectory"
        title="建议先创建分工目录，方便同学按任务或成员上传源文件。"
        type="info"
        show-icon
        :closable="false"
      >
        <template #default>
          <el-button size="small" type="primary" @click="handleCreateDirectory">创建分工目录</el-button>
        </template>
      </el-alert>
      <el-alert v-if="errorMessage" :title="errorMessage" type="warning" show-icon :closable="false" />

      <section class="workspace-page__overview" aria-label="项目概览">
        <el-card shadow="never">
          <span>项目状态</span>
          <strong>{{ project?.status === 'ended' ? '已结束' : '协作中' }}</strong>
        </el-card>
        <el-card shadow="never">
          <span>目录完成</span>
          <strong>{{ progressSummary }}</strong>
        </el-card>
        <el-card shadow="never">
          <span>最近记录</span>
          <strong>{{ logs.length }}</strong>
        </el-card>
        <el-card shadow="never">
          <span>最近压缩包</span>
          <strong>{{ latestPackage?.filename || '暂无' }}</strong>
        </el-card>
      </section>

      <section class="workspace-page__actions" aria-label="协作操作入口">
        <RouterLink class="workspace-page__action" :to="`/projects/${projectId}/package/check`">
          <DocumentChecked class="workspace-page__action-icon" />
          <span>打包前检查</span>
        </RouterLink>
        <RouterLink class="workspace-page__action" :to="`/projects/${projectId}/package/export`">
          <Box class="workspace-page__action-icon" />
          <span>最终打包</span>
        </RouterLink>
        <RouterLink class="workspace-page__action" :to="`/projects/${projectId}/mail`">
          <Message class="workspace-page__action-icon" />
          <span>邮件草稿</span>
        </RouterLink>
      </section>

      <section class="workspace-page__grid">
        <el-card shadow="never">
          <template #header>
            <div class="workspace-page__card-header">
              <span>目录进度</span>
              <el-button text type="primary" @click="loadProgress">刷新进度</el-button>
            </div>
          </template>

          <el-table
            v-loading="progressLoading"
            :data="progress?.directories || []"
            class="workspace-page__progress-table"
            empty-text="暂无目录进度"
            @row-click="openDirectory"
          >
            <el-table-column prop="name" label="目录" min-width="160" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="directoryStatusType(row.status)">{{ row.statusDisplayName }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="更新时间" min-width="160">
              <template #default="{ row }">{{ formatDate(row.updatedAt) }}</template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card shadow="never">
          <template #header>
            <div class="workspace-page__card-header">
              <span>最近操作</span>
              <RouterLink class="workspace-page__text-link" :to="`/projects/${projectId}/logs`">查看全部</RouterLink>
            </div>
          </template>

          <el-timeline v-if="logs.length > 0" class="workspace-page__timeline">
            <el-timeline-item v-for="log in logs" :key="log.id" :timestamp="formatDate(log.createdAt)">
              <strong>{{ log.action }}</strong>
              <p>{{ log.summary }}</p>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无操作记录" :image-size="96" />
        </el-card>
      </section>
    </section>
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { Box, DocumentChecked, Message, Refresh } from '@element-plus/icons-vue';

import MainLayout from '@/layouts/MainLayout.vue';
import { request } from '@/services/http';
import { getProject } from '@/services/groupProjectApi';
import { listOperationLogs } from '@/services/activityApi';
import { getLatestPackage } from '@/services/packageApi';
import { createDirectory } from '@/services/fileApi';
import { useAuthStore } from '@/stores/auth';
import { useProjectStore } from '@/stores/project';
import type {
  DirectoryStatus,
  OperationLog,
  PackageArtifact,
  Project,
  ProjectProgress,
} from '@/types/project';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const projectStore = useProjectStore();
const project = ref<Project | null>(null);
const progress = ref<ProjectProgress | null>(null);
const logs = ref<OperationLog[]>([]);
const latestPackage = ref<PackageArtifact | null>(null);
const loading = ref(false);
const progressLoading = ref(false);
const errorMessage = ref('');
const projectId = computed(() => String(route.params.projectId || route.params.id || projectStore.currentProject?.id || ''));
const numericProjectId = computed(() => Number(projectId.value));
const progressSummary = computed(() => {
  if (!progress.value || progress.value.totalDirectoryCount === 0) {
    return '暂无';
  }

  return `${progress.value.completedDirectoryCount}/${progress.value.totalDirectoryCount}`;
});
const shouldPromptCreateDirectory = computed(() => {
  const directories = progress.value?.directories ?? [];
  return project.value?.status !== 'ended'
    && directories.length === 1
    && directories[0]?.name === '默认分工目录';
});

/**
 * 读取当前用户标识。
 *
 * @returns 当前用户标识，未登录时返回 undefined
 */
function currentUserId(): number | undefined {
  return authStore.currentUser?.id;
}

/**
 * 格式化时间展示。
 *
 * @param value 日期字符串
 * @returns 本地化后的日期文本
 */
function formatDate(value?: string | null): string {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '暂无';
}

/**
 * 将目录状态映射为 Element Plus 标签样式。
 *
 * @param status 目录状态
 * @returns 标签类型
 */
function directoryStatusType(status: DirectoryStatus): 'success' | 'warning' | 'info' {
  const statusTypeMap: Record<DirectoryStatus, 'success' | 'warning' | 'info'> = {
    completed: 'success',
    in_progress: 'warning',
    not_started: 'info',
  };

  return statusTypeMap[status] ?? 'info';
}

/**
 * 打开文件管理页并定位到指定目录。
 *
 * @param row 目录进度行
 */
function openDirectory(row: { directoryId: string }): void {
  void router.push({
    name: 'project-files',
    params: { projectId: projectId.value },
    query: { directoryId: row.directoryId },
  });
}

/**
 * 在默认目录下创建第一个分工目录。
 */
async function handleCreateDirectory(): Promise<void> {
  const rootDirectoryId = progress.value?.directories[0]?.directoryId;
  if (!rootDirectoryId) {
    errorMessage.value = '请先刷新目录进度后再创建分工目录';
    return;
  }
  const name = window.prompt('请输入分工目录名称，例如：任务一、前端、第四组');
  if (!name?.trim()) {
    return;
  }

  try {
    const directory = await createDirectory({
      projectId: projectId.value,
      parentDirectoryId: rootDirectoryId,
      name: name.trim(),
    });
    await loadProgress();
    await router.push({
      name: 'project-files',
      params: { projectId: projectId.value },
      query: { directoryId: directory.id },
    });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '创建分工目录失败';
  }
}

/**
 * 查询项目目录进度。
 */
async function loadProgress(): Promise<void> {
  if (!projectId.value) {
    return;
  }

  progressLoading.value = true;

  try {
    progress.value = await request<ProjectProgress>({
      url: `/projects/${projectId.value}/progress`,
      method: 'GET',
      headers: currentUserId() ? { 'X-User-Id': String(currentUserId()) } : undefined,
    });
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '目录进度加载失败';
  } finally {
    progressLoading.value = false;
  }
}

/**
 * 加载项目工作台所需的概览、进度、操作记录和压缩包信息。
 */
async function loadWorkspace(): Promise<void> {
  if (!projectId.value || Number.isNaN(numericProjectId.value)) {
    errorMessage.value = '缺少项目标识，无法加载工作台';
    return;
  }

  loading.value = true;
  errorMessage.value = '';

  try {
    const [projectDetail, logList, packageArtifact] = await Promise.all([
      getProject(numericProjectId.value, { userId: currentUserId() }),
      listOperationLogs(projectId.value, { userId: currentUserId() }),
      getLatestPackage(projectId.value, { userId: currentUserId() }).catch(() => null),
    ]);
    project.value = projectDetail;
    projectStore.setCurrentProject(projectDetail);
    logs.value = logList.logs.slice(0, 5);
    latestPackage.value = packageArtifact;
    await loadProgress();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '项目工作台加载失败';
  } finally {
    loading.value = false;
  }
}

onMounted(loadWorkspace);
</script>

<style scoped>
.workspace-page {
  display: grid;
  gap: 18px;
}

.workspace-page__aside h2 {
  margin: 0 0 12px;
  font-size: 14px;
}

.workspace-page__aside-link {
  display: block;
  padding: 10px 12px;
  border-radius: 8px;
  color: #3b4351;
  text-decoration: none;
}

.workspace-page__aside-link:hover {
  color: #173b70;
  background: #eef3f8;
}

.workspace-page__overview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.workspace-page__overview :deep(.el-card__body) {
  display: grid;
  gap: 8px;
}

.workspace-page__overview span {
  color: #687386;
}

.workspace-page__overview strong {
  overflow: hidden;
  font-size: 22px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workspace-page__actions {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.workspace-page__action {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 64px;
  padding: 0 18px;
  border: 1px solid #d9dee7;
  border-radius: 8px;
  color: #20242c;
  background: #ffffff;
  font-weight: 700;
  text-decoration: none;
}

.workspace-page__action:hover {
  border-color: #9ab2d3;
  color: #173b70;
  background: #f3f7fb;
}

.workspace-page__action-icon {
  width: 20px;
  height: 20px;
}

.workspace-page__grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(320px, 0.9fr);
  gap: 18px;
  align-items: start;
}

.workspace-page__progress-table :deep(tbody tr) {
  cursor: pointer;
}

.workspace-page__card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.workspace-page__text-link {
  color: #1d4f91;
  font-weight: 600;
  text-decoration: none;
}

.workspace-page__timeline {
  padding-left: 2px;
}

.workspace-page__timeline p {
  margin: 4px 0 0;
  color: #687386;
}

@media (max-width: 980px) {
  .workspace-page__overview,
  .workspace-page__actions,
  .workspace-page__grid {
    grid-template-columns: 1fr;
  }
}
</style>
