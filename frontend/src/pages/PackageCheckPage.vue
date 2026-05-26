<template>
  <MainLayout>
    <template #title>
      <span>打包前检查</span>
    </template>

    <template #actions>
      <el-button :icon="Refresh" :loading="checking" type="primary" @click="handleRunCheck">
        执行检查
      </el-button>
    </template>

    <template #aside>
      <el-descriptions :column="1" size="small" border>
        <el-descriptions-item label="当前项目">
          {{ projectName }}
        </el-descriptions-item>
        <el-descriptions-item label="风险数量">
          {{ report?.issues.length ?? 0 }}
        </el-descriptions-item>
        <el-descriptions-item label="可清理项">
          {{ report?.cleanupSuggestions.length ?? 0 }}
        </el-descriptions-item>
      </el-descriptions>
    </template>

    <section class="page-grid">
      <el-alert
        class="page-grid__full"
        :closable="false"
        show-icon
        title="检查结果只做提醒，不会强制阻止后续打包。清理建议会先展示预览对象，确认后再提交清理。"
        type="info"
      />

      <el-card class="panel" shadow="never">
        <template #header>
          <div class="panel__header">
            <span>风险列表</span>
            <el-tag v-if="report" :type="report.canContinuePackaging ? 'success' : 'warning'">
              {{ report.canContinuePackaging ? '允许继续打包' : '需确认风险' }}
            </el-tag>
          </div>
        </template>

        <el-empty v-if="!report" description="尚未执行检查" />
        <el-table v-else :data="report.issues" border height="420">
          <el-table-column label="类型" min-width="150">
            <template #default="{ row }: { row: CheckIssue }">
              <el-tag :type="issueTagType(row.type)">
                {{ issueTypeText(row.type) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="路径" min-width="260" prop="path" show-overflow-tooltip />
          <el-table-column label="级别" min-width="100" prop="level" />
          <el-table-column label="清理建议" min-width="110">
            <template #default="{ row }: { row: CheckIssue }">
              <el-tag :type="row.cleanupCandidate ? 'success' : 'info'">
                {{ row.cleanupCandidate ? '可清理' : '仅提醒' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card class="panel" shadow="never">
        <template #header>
          <div class="panel__header">
            <span>清理建议</span>
            <el-button
              :disabled="selectedSuggestions.length === 0"
              :loading="previewing"
              size="small"
              type="primary"
              @click="handlePreviewCleanup"
            >
              预览清理对象
            </el-button>
          </div>
        </template>

        <el-empty v-if="cleanupSuggestions.length === 0" description="暂无可一键清理项" />
        <el-checkbox-group v-else v-model="selectedSuggestions" class="suggestion-list">
          <el-checkbox
            v-for="item in cleanupSuggestions"
            :key="item.path"
            :label="item.path"
            class="suggestion-list__item"
          >
            <span>{{ item.path }}</span>
            <el-tag size="small" type="info">{{ issueTypeText(item.type) }}</el-tag>
          </el-checkbox>
        </el-checkbox-group>
      </el-card>
    </section>

    <el-dialog v-model="previewDialogVisible" title="确认清理对象" width="680px">
      <el-table :data="cleanupPreview" border max-height="360">
        <el-table-column label="文件名" min-width="180" prop="fileName" show-overflow-tooltip />
        <el-table-column label="路径" min-width="260" prop="path" show-overflow-tooltip />
        <el-table-column label="大小" min-width="110">
          <template #default="{ row }: { row: CleanupPreviewObject }">
            {{ formatSize(row.size) }}
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="previewDialogVisible = false">取消</el-button>
        <el-button :loading="cleaning" type="danger" @click="handleApplyCleanup">
          确认清理
        </el-button>
      </template>
    </el-dialog>
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';
import { useRoute } from 'vue-router';

import MainLayout from '@/layouts/MainLayout.vue';
import { useAuthStore } from '@/stores/auth';
import { useProjectStore } from '@/stores/project';
import { applyCleanup, previewCleanup, runPackageCheck } from '@/services/packageApi';

import type {
  CheckIssue,
  CheckIssueType,
  CheckReport,
  CleanupPreviewObject,
  CleanupSuggestion,
} from '@/types/project';

const route = useRoute();
const authStore = useAuthStore();
const projectStore = useProjectStore();

const checking = ref(false);
const previewing = ref(false);
const cleaning = ref(false);
const report = ref<CheckReport | null>(null);
const selectedSuggestions = ref<string[]>([]);
const cleanupPreview = ref<CleanupPreviewObject[]>([]);
const previewDialogVisible = ref(false);

const projectId = computed(() => {
  const routeProjectId = route.params.projectId;
  return String(projectStore.currentProject?.id ?? (Array.isArray(routeProjectId) ? routeProjectId[0] : routeProjectId) ?? '');
});
const projectName = computed(() => projectStore.currentProject?.name ?? (projectId.value ? `项目 ${projectId.value}` : '未选择项目'));
const cleanupSuggestions = computed(() => report.value?.cleanupSuggestions ?? []);

/**
 * 将检查项类型转换为页面展示文案。
 *
 * @param type 检查项类型
 * @returns 中文展示文案
 */
function issueTypeText(type: CheckIssueType): string {
  const labels: Record<string, string> = {
    EMPTY_DIRECTORY: '空目录',
    ARCHIVE_FILE: '压缩包',
    MISSING_README: '缺少说明',
    CACHE_DIRECTORY: '缓存目录',
    TEMPORARY_FILE: '临时文件',
    LOG_FILE: '日志文件',
    SYSTEM_FILE: '系统文件',
    LARGE_FILE: '大文件',
  };

  return labels[type] ?? type;
}

/**
 * 根据风险类型选择标签色，帮助用户快速区分可清理内容。
 *
 * @param type 检查项类型
 * @returns Element Plus 标签类型
 */
function issueTagType(type: CheckIssueType): 'success' | 'info' | 'warning' | 'danger' {
  if (['CACHE_DIRECTORY', 'TEMPORARY_FILE', 'LOG_FILE', 'SYSTEM_FILE'].includes(type)) {
    return 'success';
  }

  if (type === 'ARCHIVE_FILE' || type === 'LARGE_FILE') {
    return 'warning';
  }

  return 'info';
}

/**
 * 格式化文件大小。
 *
 * @param size 文件字节数
 * @returns 便于阅读的大小文本
 */
function formatSize(size: number): string {
  if (size < 1024) {
    return `${size} B`;
  }

  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`;
  }

  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

/**
 * 从已选路径生成清理请求项。
 *
 * @returns 清理请求项列表
 */
function buildCleanupItems(): CleanupSuggestion[] {
  const selected = new Set(selectedSuggestions.value);

  return cleanupSuggestions.value.filter((item) => selected.has(item.path));
}

/**
 * 执行打包前检查并同步默认勾选可清理项。
 */
async function handleRunCheck(): Promise<void> {
  if (!projectId.value) {
    ElMessage.warning('请先选择项目');
    return;
  }

  checking.value = true;

  try {
    report.value = await runPackageCheck(projectId.value, { userId: authStore.currentUser?.id });
    selectedSuggestions.value = report.value.cleanupSuggestions.map((item) => item.path);
    ElMessage.success('检查完成');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '检查失败');
  } finally {
    checking.value = false;
  }
}

/**
 * 清理前获取后端预览对象，避免直接提交清理动作。
 */
async function handlePreviewCleanup(): Promise<void> {
  const items = buildCleanupItems();

  if (items.length === 0) {
    ElMessage.warning('请先选择需要清理的建议项');
    return;
  }

  previewing.value = true;

  try {
    const preview = await previewCleanup({
      projectId: projectId.value,
      userId: authStore.currentUser?.id,
      items,
    });
    cleanupPreview.value = preview.previewObjects;
    previewDialogVisible.value = true;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '清理预览失败');
  } finally {
    previewing.value = false;
  }
}

/**
 * 用户确认后执行清理建议。
 */
async function handleApplyCleanup(): Promise<void> {
  const items = buildCleanupItems();

  try {
    await ElMessageBox.confirm('清理对象会按后端规则移入回收站或标记为已清理，确认继续？', '确认清理', {
      confirmButtonText: '确认清理',
      cancelButtonText: '取消',
      type: 'warning',
    });
  } catch {
    return;
  }

  cleaning.value = true;

  try {
    const result = await applyCleanup({
      projectId: projectId.value,
      userId: authStore.currentUser?.id,
      items,
    });
    previewDialogVisible.value = false;
    ElMessage.success(`已清理 ${result.cleanedObjectIds.length} 个对象`);
    await handleRunCheck();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '清理失败');
  } finally {
    cleaning.value = false;
  }
}
</script>

<style scoped>
.page-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.8fr);
  gap: 18px;
}

.page-grid__full {
  grid-column: 1 / -1;
}

.panel {
  border-radius: 8px;
}

.panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.suggestion-list {
  display: grid;
  gap: 10px;
}

.suggestion-list__item {
  display: flex;
  align-items: center;
  min-height: 36px;
  padding: 0 10px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
}

.suggestion-list__item :deep(.el-checkbox__label) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
}

@media (max-width: 960px) {
  .page-grid {
    grid-template-columns: 1fr;
  }
}
</style>
