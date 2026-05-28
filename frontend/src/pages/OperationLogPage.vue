<template>
  <MainLayout>
    <template #title>
      <span>操作记录</span>
    </template>

    <template #actions>
      <el-button :icon="Refresh" :loading="loading" type="primary" @click="loadLogs">
        查询记录
      </el-button>
    </template>

    <template #aside>
      <el-alert
        :closable="false"
        show-icon
        title="操作记录对成员透明，只读用户应由路由或后端权限拦截。"
        type="info"
      />
    </template>

    <section class="log-page">
      <el-card class="panel" shadow="never">
        <el-form :model="filters" class="filters" label-width="72px">
          <el-form-item label="操作类型">
            <el-select v-model="filters.action" clearable filterable placeholder="全部类型">
              <el-option v-for="item in actionOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>

          <el-form-item label="操作人">
            <el-input-number v-model="filters.actorId" :min="1" controls-position="right" placeholder="用户 ID" />
          </el-form-item>

          <el-form-item label="时间范围">
            <el-date-picker
              v-model="filters.timeRange"
              end-placeholder="结束时间"
              range-separator="至"
              start-placeholder="开始时间"
              type="datetimerange"
              value-format="YYYY-MM-DDTHH:mm:ss"
            />
          </el-form-item>
        </el-form>
      </el-card>

      <el-card class="panel" shadow="never">
        <el-table v-loading="loading" :data="logs" border empty-text="暂无操作记录">
          <el-table-column label="时间" min-width="170" prop="createdAt" />
          <el-table-column label="操作人" min-width="100" prop="actorId" />
          <el-table-column label="类型" min-width="170">
            <template #default="{ row }: { row: OperationLog }">
              <el-tag>{{ actionText(row.action) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="对象" min-width="170">
            <template #default="{ row }: { row: OperationLog }">
              {{ row.targetType }} / {{ row.targetId }}
            </template>
          </el-table-column>
          <el-table-column label="摘要" min-width="260" prop="summary" show-overflow-tooltip />
        </el-table>
      </el-card>
    </section>
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';
import { useRoute } from 'vue-router';

import MainLayout from '@/layouts/MainLayout.vue';
import { listOperationLogs } from '@/services/activityApi';
import { useAuthStore } from '@/stores/auth';
import { useProjectStore } from '@/stores/project';

import type { OperationLog } from '@/types/project';

const route = useRoute();
const authStore = useAuthStore();
const projectStore = useProjectStore();

const loading = ref(false);
const logs = ref<OperationLog[]>([]);
const filters = reactive<{
  action: string;
  actorId: number | undefined;
  timeRange: [string, string] | [];
}>({
  action: '',
  actorId: undefined,
  timeRange: [],
});

const projectId = computed(() => {
  const routeProjectId = route.params.projectId;
  return String(projectStore.currentProject?.id ?? (Array.isArray(routeProjectId) ? routeProjectId[0] : routeProjectId) ?? '');
});

const actionOptions = [
  { label: '文件上传', value: 'FILE_UPLOAD' },
  { label: '文件下载', value: 'FILE_DOWNLOAD' },
  { label: '文件移动', value: 'FILE_MOVE' },
  { label: '文件重命名', value: 'FILE_RENAME' },
  { label: '文件删除', value: 'FILE_DELETE' },
  { label: '文件恢复', value: 'FILE_RESTORE' },
  { label: '目录状态变更', value: 'DIRECTORY_STATUS_UPDATE' },
  { label: '打包检查', value: 'CHECK_RUN' },
  { label: '清理执行', value: 'CLEANUP_APPLIED' },
  { label: '压缩包生成', value: 'PACKAGE_CREATED' },
  { label: '邮件草稿生成', value: 'MAIL_DRAFT_CREATED' },
  { label: '邮件发送', value: 'MAIL_SENT' },
  { label: '项目结束', value: 'PROJECT_ENDED' },
  { label: '项目重新打开', value: 'PROJECT_REOPENED' },
];

/**
 * 将操作类型转换为中文文案。
 *
 * @param action 操作类型
 * @returns 中文展示文案
 */
function actionText(action: string): string {
  return actionOptions.find((item) => item.value === action)?.label ?? action;
}

/**
 * 按当前筛选条件加载操作记录。
 */
async function loadLogs(): Promise<void> {
  if (!projectId.value) {
    ElMessage.warning('请先选择项目');
    return;
  }

  loading.value = true;

  try {
    const [from, to] = filters.timeRange;
    const result = await listOperationLogs(projectId.value, {
      userId: authStore.currentUser?.id,
      action: filters.action || undefined,
      actorId: filters.actorId,
      from,
      to,
    });
    logs.value = result.logs;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作记录加载失败');
  } finally {
    loading.value = false;
  }
}

onMounted(loadLogs);
</script>

<style scoped>
.log-page {
  display: grid;
  gap: 18px;
}

.panel {
  border-radius: 8px;
}

.filters {
  display: grid;
  grid-template-columns: minmax(180px, 1fr) minmax(160px, 220px) minmax(320px, 1.4fr);
  gap: 12px;
}

.filters :deep(.el-form-item) {
  margin-bottom: 0;
}

@media (max-width: 960px) {
  .filters {
    grid-template-columns: 1fr;
  }
}
</style>
