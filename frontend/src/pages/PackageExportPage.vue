<template>
  <MainLayout>
    <template #title>
      <span>打包导出</span>
    </template>

    <template #actions>
      <el-button :icon="Download" :disabled="!latestPackage" :loading="downloading" @click="handleDownload">
        下载最近压缩包
      </el-button>
    </template>

    <template #aside>
      <el-descriptions :column="1" size="small" border>
        <el-descriptions-item label="当前项目">
          {{ projectName }}
        </el-descriptions-item>
        <el-descriptions-item label="最近压缩包">
          {{ latestPackage?.filename ?? '暂无' }}
        </el-descriptions-item>
        <el-descriptions-item label="格式">
          {{ latestPackage?.format ?? '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </template>

    <section class="export-layout">
      <el-card class="panel" shadow="never">
        <template #header>
          <div class="panel__header">
            <span>生成最终压缩包</span>
            <el-tag type="warning">重新打包会替换最近一次压缩包</el-tag>
          </div>
        </template>

        <el-form ref="formRef" :model="form" :rules="rules" label-width="112px">
          <el-form-item label="压缩包文件名" prop="baseName">
            <el-input v-model.trim="form.baseName" maxlength="80" placeholder="例如：课程项目成果" show-word-limit />
          </el-form-item>

          <el-form-item label="导出格式" prop="format">
            <el-radio-group v-model="form.format" class="format-group">
              <el-radio-button label="zip" value="zip">.zip</el-radio-button>
              <el-radio-button label="7z" value="7z">.7z</el-radio-button>
              <el-radio-button label="tar.gz" value="tar.gz">.tar.gz</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="检查风险">
            <el-switch
              v-model="form.continueAfterCheck"
              active-text="确认忽略提醒并继续"
              inactive-text="按后端默认规则处理"
            />
          </el-form-item>

          <el-form-item>
            <el-button :icon="Box" :loading="exporting" type="primary" @click="handleCreatePackage">
              生成压缩包
            </el-button>
            <el-button :icon="Refresh" :loading="loadingLatest" @click="loadLatestPackage">
              刷新最近压缩包
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card class="panel" shadow="never">
        <template #header>
          <span>最近一次导出</span>
        </template>

        <el-empty v-if="!latestPackage" description="暂无最终压缩包" />
        <el-descriptions v-else :column="1" border>
          <el-descriptions-item label="文件名">
            {{ latestPackage.filename }}
          </el-descriptions-item>
          <el-descriptions-item label="格式">
            {{ latestPackage.format }}
          </el-descriptions-item>
          <el-descriptions-item label="大小">
            {{ formatSize(latestPackage.size) }}
          </el-descriptions-item>
          <el-descriptions-item label="快照时间">
            {{ latestPackage.snapshotCreatedAt }}
          </el-descriptions-item>
        </el-descriptions>
      </el-card>
    </section>
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { Box, Download, Refresh } from '@element-plus/icons-vue';
import { useRoute } from 'vue-router';

import MainLayout from '@/layouts/MainLayout.vue';
import { createPackage, downloadLatestPackage, getLatestPackage } from '@/services/packageApi';
import { useAuthStore } from '@/stores/auth';
import { useProjectStore } from '@/stores/project';

import type { PackageArtifact, PackageFormat } from '@/types/project';

const route = useRoute();
const authStore = useAuthStore();
const projectStore = useProjectStore();

const formRef = ref<FormInstance>();
const exporting = ref(false);
const downloading = ref(false);
const loadingLatest = ref(false);
const latestPackage = ref<PackageArtifact | null>(null);

const form = reactive({
  baseName: '',
  format: 'zip' as PackageFormat,
  continueAfterCheck: true,
});

const projectId = computed(() => {
  const routeProjectId = route.params.projectId;
  return String(projectStore.currentProject?.id ?? (Array.isArray(routeProjectId) ? routeProjectId[0] : routeProjectId) ?? '');
});
const projectName = computed(() => projectStore.currentProject?.name ?? (projectId.value ? `项目 ${projectId.value}` : '未选择项目'));

const rules: FormRules = {
  baseName: [
    { required: true, message: '请输入压缩包文件名', trigger: 'blur' },
    {
      pattern: /^[^\\/:*?"<>|\u0000-\u001f]+$/,
      message: '文件名不能包含路径分隔符或非法字符',
      trigger: 'blur',
    },
  ],
  format: [{ required: true, message: '请选择导出格式', trigger: 'change' }],
};

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
 * 查询最近一次压缩包，用于展示覆盖提示和下载入口。
 */
async function loadLatestPackage(): Promise<void> {
  if (!projectId.value) {
    return;
  }

  loadingLatest.value = true;

  try {
    latestPackage.value = await getLatestPackage(projectId.value, { userId: authStore.currentUser?.id });
  } catch {
    latestPackage.value = null;
  } finally {
    loadingLatest.value = false;
  }
}

/**
 * 创建最终压缩包，存在旧包时先要求用户确认覆盖。
 */
async function handleCreatePackage(): Promise<void> {
  if (!projectId.value) {
    ElMessage.warning('请先选择项目');
    return;
  }

  const valid = await formRef.value?.validate().catch(() => false);

  if (!valid) {
    return;
  }

  if (latestPackage.value) {
    try {
      await ElMessageBox.confirm(`将替换最近一次压缩包「${latestPackage.value.filename}」，确认继续？`, '确认覆盖', {
        confirmButtonText: '继续打包',
        cancelButtonText: '取消',
        type: 'warning',
      });
    } catch {
      return;
    }
  }

  exporting.value = true;

  try {
    latestPackage.value = await createPackage({
      projectId: projectId.value,
      userId: authStore.currentUser?.id,
      baseName: form.baseName,
      format: form.format,
      continueAfterCheck: form.continueAfterCheck,
    });
    ElMessage.success('压缩包已生成');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '打包失败');
  } finally {
    exporting.value = false;
  }
}

/**
 * 下载最近一次压缩包并触发浏览器保存。
 */
async function handleDownload(): Promise<void> {
  if (!projectId.value || !latestPackage.value) {
    return;
  }

  downloading.value = true;

  try {
    const blob = await downloadLatestPackage(projectId.value, { userId: authStore.currentUser?.id });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = latestPackage.value.filename;
    link.click();
    window.URL.revokeObjectURL(url);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '下载失败');
  } finally {
    downloading.value = false;
  }
}

onMounted(loadLatestPackage);
</script>

<style scoped>
.export-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 420px);
  gap: 18px;
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

.format-group {
  display: flex;
  flex-wrap: wrap;
}

@media (max-width: 960px) {
  .export-layout {
    grid-template-columns: 1fr;
  }
}
</style>
