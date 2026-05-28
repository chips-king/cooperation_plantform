<template>
  <MainLayout>
    <template #title>
      <span>{{ project?.name || '项目工作台' }}</span>
    </template>

    <template #actions>
      <el-dropdown split-button type="primary" @click="loadWorkspace">
        <span :style="{ display: 'inline-flex', alignItems: 'center', gap: '6px' }"
          ><Refresh style="width: 14px; height: 14px" /> 刷新</span
        >
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item :icon="Delete" style="color: #f56c6c" @click="handleDeleteProject"> 删除项目 </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </template>

    <template #aside>
      <section class="workspace-page__aside">
        <h2>快捷入口</h2>
        <RouterLink class="workspace-page__aside-link" :to="`/projects/${projectId}/progress`">任务进度</RouterLink>
        <RouterLink class="workspace-page__aside-link" :to="`/projects/${projectId}/mail`">邮件草稿</RouterLink>
      </section>
    </template>

    <section class="workspace-page">
      <!-- 项目信息头部 -->
      <div class="workspace-page__project-header">
        <div class="workspace-page__project-info">
          <h1 class="workspace-page__project-name">{{ project?.name }}</h1>
          <el-tag :type="project?.status === 'ended' ? 'info' : 'success'" size="small" effect="plain">
            {{ project?.status === 'ended' ? '已结束' : '协作中' }}
          </el-tag>
        </div>
        <div class="workspace-page__project-actions">
          <div class="workspace-page__project-stats">
            <span class="workspace-page__stat">
              <FolderOpened class="workspace-page__stat-icon" />
              {{ visibleDirectories.length }} 个目录
            </span>
            <span class="workspace-page__stat">
              <Document class="workspace-page__stat-icon" />
              {{ totalFiles }} 个文件
            </span>
            <span class="workspace-page__stat">
              <User class="workspace-page__stat-icon" />
              {{ visibleDirectories.length > 0 ? '有目录' : '暂无目录' }}
            </span>
          </div>
          <el-button type="primary" :icon="Box" @click="packageDialogVisible = true">打包</el-button>
        </div>
      </div>

      <!-- 提示信息 -->
      <el-alert
        v-if="project?.status === 'ended'"
        title="项目已结束，成员上传、整理和删除等写操作已锁定。"
        type="info"
        show-icon
        :closable="false"
      />
      <el-alert v-if="errorMessage" :title="errorMessage" type="warning" show-icon :closable="false" />

      <!-- 文件列表 -->
      <FileListBlock
        ref="fileListBlockRef"
        :directories="visibleDirectories"
        :files="rootFiles"
        :loading="treeLoading"
        @open-directory="openDirectory"
        @open-file="handleFilePreview"
        @upload-files="handleUploadFiles"
        @download-file="handleDownloadFile"
        @delete-file="handleDeleteFile"
        @delete-directory="handleDeleteSingleDirectory"
      />

      <!-- 最近操作 -->
      <section class="workspace-page__recent">
        <div class="workspace-page__recent-header">
          <span class="workspace-page__recent-title">最近操作</span>
          <RouterLink class="workspace-page__text-link" :to="`/projects/${projectId}/logs`">查看全部</RouterLink>
        </div>
        <div v-if="logs.length > 0" class="workspace-page__recent-list">
          <div v-for="log in logs.slice(0, 5)" :key="log.id" class="workspace-page__recent-item">
            <div class="workspace-page__recent-dot" />
            <div class="workspace-page__recent-content">
              <span class="workspace-page__recent-action">{{ actionLabel(log.action) }}</span>
              <span class="workspace-page__recent-summary">{{ log.summary }}</span>
              <span class="workspace-page__recent-time">{{ formatDate(log.createdAt) }}</span>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无操作记录" :image-size="64" />
      </section>
    </section>

    <!-- 创建目录弹窗 -->
    <el-dialog v-model="directoryDialogVisible" title="创建分工目录" width="420px">
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="目录名称" required>
          <el-input
            v-model.trim="directoryFormName"
            maxlength="40"
            show-word-limit
            placeholder="例如：任务一、前端、第四组"
            @keyup.enter="handleCreateDirectory"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="directoryDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="creatingDirectory" @click="handleCreateDirectory">创建</el-button>
      </template>
    </el-dialog>

    <!-- 打包命令面板 -->
    <PackageCommandDialog
      :visible="packageDialogVisible"
      :project-id="projectId"
      @update:visible="packageDialogVisible = $event"
      @package-complete="handlePackageComplete"
    />
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Box, Delete, Document, FolderOpened, Refresh } from '@element-plus/icons-vue';

import FileListBlock from '@/components/project/FileListBlock.vue';
import MainLayout from '@/layouts/MainLayout.vue';
import PackageCommandDialog from '@/components/project/PackageCommandDialog.vue';
import { deleteProject, getProject } from '@/services/groupProjectApi';
import { listOperationLogs } from '@/services/activityApi';
import { createDirectory, deleteDirectory, deleteFile, downloadFile, getDirectoryTree, uploadFile } from '@/services/fileApi';
import { useAuthStore } from '@/stores/auth';
import { useProjectStore } from '@/stores/project';
import type {
  Directory,
  DirectoryStatus,
  FileAsset,
  OperationLog,
  Project,
  ProjectProgress,
} from '@/types/project';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const projectStore = useProjectStore();

const project = ref<Project | null>(null);
const directories = ref<Directory[]>([]);
const logs = ref<OperationLog[]>([]);
const loading = ref(false);
const treeLoading = ref(false);
const errorMessage = ref('');
const directoryDialogVisible = ref(false);
const directoryFormName = ref('');
const creatingDirectory = ref(false);
const packageDialogVisible = ref(false);
/** FileListBlock 组件引用，用于调用 runUpload */
const fileListBlockRef = ref<InstanceType<typeof FileListBlock> | null>(null);

const projectId = computed(() => String(route.params.projectId || route.params.id || projectStore.currentProject?.id || ''));
const numericProjectId = computed(() => Number(projectId.value));

/** 根目录标识（parentDirectoryId 为 null 的顶层目录），用于内部操作，对用户不可见。 */
const rootDirectoryId = ref<string | null>(null);

/** 对用户可见的目录列表，排除隐藏的根目录。 */
const visibleDirectories = computed(() => {
  const root = directories.value.find((d) => !d.parentId);
  if (root && root.children && root.children.length > 0) {
    return root.children;
  }
  // 如果根目录存在但没有子目录，返回空数组（项目为空状态）
  if (root) return [];
  return directories.value;
});

/** 根目录下的文件（直接上传到根级别的文件，类似 GitHub 仓库根目录文件）。 */
const rootFiles = computed<FileAsset[]>(() => {
  const root = directories.value.find((d) => !d.parentId);
  return root?.files ?? [];
});

const totalFiles = computed(() => {
  let count = 0;
  for (const dir of directories.value) {
    count += dir.files?.length ?? 0;
    for (const child of dir.children ?? []) {
      count += child.files?.length ?? 0;
    }
  }
  return count;
});

function currentUserId(): number | undefined {
  return authStore.currentUser?.id;
}

const actionMap: Record<string, string> = {
  FILE_UPLOAD: '文件上传',
  FILE_DOWNLOAD: '文件下载',
  FILE_MOVE: '文件移动',
  FILE_RENAME: '文件重命名',
  FILE_DELETE: '文件删除',
  FILE_RESTORE: '文件恢复',
  DIRECTORY_STATUS_UPDATE: '目录状态变更',
  CHECK_RUN: '打包检查',
  CLEANUP_APPLIED: '清理执行',
  PACKAGE_CREATED: '压缩包生成',
  MAIL_DRAFT_CREATED: '邮件草稿生成',
  MAIL_SENT: '邮件发送',
  PROJECT_ENDED: '项目结束',
  PROJECT_REOPENED: '项目重新打开',
  PROJECT_CREATED: '创建项目',
  PROJECT_DELETED: '删除项目',
};

function actionLabel(action: string): string {
  return actionMap[action] ?? action;
}

function formatDate(value?: string | null): string {
  if (!value) return '-';
  const d = new Date(value);
  const now = new Date();
  const diffMs = now.getTime() - d.getTime();
  const diffMin = Math.floor(diffMs / 60000);
  if (diffMin < 1) return '刚刚';
  if (diffMin < 60) return `${diffMin} 分钟前`;
  const diffHour = Math.floor(diffMin / 60);
  if (diffHour < 24) return `${diffHour} 小时前`;
  const diffDay = Math.floor(diffHour / 24);
  if (diffDay < 7) return `${diffDay} 天前`;
  return d.toLocaleDateString('zh-CN');
}

/**
 * 点击目录时，刷新目录树以获取最新文件信息。
 */
function openDirectory(): void {
  void loadDirectoryTree();
}

/**
 * 点击文件时，在新标签页中预览或下载文件。
 */
async function handleFilePreview(file: FileAsset): Promise<void> {
  try {
    const blob = await downloadFile(file.fileId);
    const url = URL.createObjectURL(blob);
    // 尝试在新标签页打开（浏览器可预览的类型会自动预览）
    window.open(url, '_blank');
    // 延迟释放 URL，让浏览器有时间加载
    setTimeout(() => URL.revokeObjectURL(url), 60_000);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '打开文件失败');
  }
}

/**
 * 下载文件到本地。
 */
async function handleDownloadFile(file: FileAsset): Promise<void> {
  try {
    const blob = await downloadFile(file.fileId);
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = file.name;
    link.click();
    URL.revokeObjectURL(url);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '下载失败');
  }
}

/**
 * 删除单个文件。
 */
async function handleDeleteFile(file: FileAsset): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除文件「${file.name}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
    });
  } catch {
    return;
  }

  try {
    await deleteFile({ projectId: projectId.value, fileId: file.fileId });
    ElMessage.success('文件已删除');
    await loadDirectoryTree();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败');
  }
}

/**
 * 删除单个目录（从文件列表行操作触发）。
 */
async function handleDeleteSingleDirectory(dir: Directory): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认删除目录「${dir.name}」吗？目录必须为空才能删除。`, '删除确认', {
      type: 'warning',
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
    });
  } catch {
    return;
  }

  try {
    await deleteDirectory({ projectId: projectId.value, directoryId: dir.id });
    ElMessage.success('目录已删除');
    await loadDirectoryTree();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败');
  }
}

function openDirectoryDialog(): void {
  directoryFormName.value = '';
  directoryDialogVisible.value = true;
}

async function handleCreateDirectory(): Promise<void> {
  if (!directoryFormName.value.trim()) {
    ElMessage.warning('请输入目录名称');
    return;
  }

  creatingDirectory.value = true;
  try {
    let parentId = rootDirectoryId.value;

    // 如果项目没有根目录，先自动创建一个隐藏的根目录
    if (!parentId) {
      const rootDir = await createDirectory({
        projectId: projectId.value,
        parentDirectoryId: '0',
        name: 'root',
      });
      parentId = rootDir.id;
      rootDirectoryId.value = rootDir.id;
    }

    const directory = await createDirectory({
      projectId: projectId.value,
      parentDirectoryId: parentId,
      name: directoryFormName.value.trim(),
    });
    directoryDialogVisible.value = false;
    ElMessage.success('目录已创建');
    await loadDirectoryTree();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '创建分工目录失败';
  } finally {
    creatingDirectory.value = false;
  }
}

/**
 * 处理文件上传：文件直接上传到根目录（类似 GitHub），不自动创建子目录。
 *
 * @param files 用户选择的文件列表
 */
async function handleUploadFiles(files: File[]): Promise<void> {
  if (files.length === 0) return;

  const block = fileListBlockRef.value;
  if (!block) return;

  // 确保根目录存在（内部隐藏目录，不对用户展示）
  let parentId = rootDirectoryId.value;
  if (!parentId) {
    try {
      const rootDir = await createDirectory({
        projectId: projectId.value,
        parentDirectoryId: '0',
        name: 'root',
      });
      parentId = rootDir.id;
      rootDirectoryId.value = rootDir.id;
    } catch (error) {
      ElMessage.error(error instanceof Error ? error.message : '创建根目录失败');
      return;
    }
  }

  const targetDirId = parentId;

  // 调用 FileListBlock 的 runUpload 进行批量上传，文件直接落入根目录
  await block.runUpload(files, async (file: File, relativePath: string) => {
    await uploadFile({
      projectId: projectId.value,
      directoryId: targetDirId,
      file,
      relativePath,
      duplicatePolicy: 'new_version',
    });
  });

  // 上传完成后刷新目录树
  ElMessage.success('文件上传完成');
  await loadDirectoryTree();
}

async function loadDirectoryTree(): Promise<void> {
  if (!projectId.value) return;
  treeLoading.value = true;
  try {
    const response = await getDirectoryTree(projectId.value);
    directories.value = response.directories;
    // 检测并记录根目录标识，用于后续创建子目录时作为父目录
    const root = response.directories.find((d: Directory) => !d.parentId);
    rootDirectoryId.value = root?.id ?? null;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '目录加载失败';
  } finally {
    treeLoading.value = false;
  }
}

async function loadWorkspace(): Promise<void> {
  if (!projectId.value || Number.isNaN(numericProjectId.value)) {
    errorMessage.value = '缺少项目标识，无法加载工作台';
    return;
  }

  loading.value = true;
  errorMessage.value = '';

  try {
    const [projectDetail, logList] = await Promise.all([
      getProject(numericProjectId.value, { userId: currentUserId() }),
      listOperationLogs(projectId.value, { userId: currentUserId() }).catch(() => ({ logs: [] })),
    ]);
    project.value = projectDetail;
    projectStore.setCurrentProject(projectDetail);
    logs.value = logList.logs;
    await loadDirectoryTree();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '项目工作台加载失败';
  } finally {
    loading.value = false;
  }
}

/**
 * 打包完成后跳转到邮件草稿页面。
 */
function handlePackageComplete(): void {
  void loadWorkspace();
  void router.push(`/projects/${projectId.value}/mail`);
}

async function handleDeleteProject(): Promise<void> {
  try {
    await ElMessageBox.confirm('删除项目后不可恢复，确认继续？', '删除项目', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    });
  } catch {
    return;
  }
  try {
    await deleteProject(numericProjectId.value);
    ElMessage.success('项目已删除');
    router.push('/');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败');
  }
}

onMounted(loadWorkspace);
</script>

<style scoped>
.workspace-page { display: grid; gap: 18px; }

/* ---- 项目信息头部 ---- */
.workspace-page__project-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 20px 24px;
  background: var(--cb-bg-card);
  border: 1px solid var(--cb-border);
  border-radius: var(--cb-radius-md);
}

.workspace-page__project-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.workspace-page__project-name {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--cb-text-primary);
}

.workspace-page__project-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}

.workspace-page__project-stats {
  display: flex;
  gap: 20px;
}

.workspace-page__stat {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--cb-text-secondary);
}

.workspace-page__stat-icon {
  width: 14px;
  height: 14px;
  color: var(--cb-text-muted);
}

/* ---- 侧边栏 ---- */
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

/* ---- 最近操作 ---- */
.workspace-page__recent {
  background: var(--cb-bg-card);
  border: 1px solid var(--cb-border);
  border-radius: var(--cb-radius-md);
  overflow: hidden;
}

.workspace-page__recent-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 18px;
  background: var(--cb-bg-page);
  border-bottom: 1px solid var(--cb-border-light);
}

.workspace-page__recent-title {
  font-weight: 600;
  font-size: 14px;
}

.workspace-page__text-link {
  color: #1d4f91;
  font-weight: 600;
  text-decoration: none;
  font-size: 13px;
}

.workspace-page__recent-list {
  padding: 0;
}

.workspace-page__recent-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 18px;
  border-bottom: 1px solid var(--cb-border-light);
}

.workspace-page__recent-item:last-child {
  border-bottom: none;
}

.workspace-page__recent-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--cb-color-primary);
  margin-top: 6px;
  flex-shrink: 0;
}

.workspace-page__recent-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.workspace-page__recent-action {
  font-weight: 600;
  font-size: 13px;
  color: var(--cb-text-primary);
}

.workspace-page__recent-summary {
  font-size: 13px;
  color: var(--cb-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.workspace-page__recent-time {
  font-size: 12px;
  color: var(--cb-text-muted);
}

@media (max-width: 768px) {
  .workspace-page__project-header {
    flex-direction: column;
    gap: 12px;
  }

  .workspace-page__project-stats {
    flex-wrap: wrap;
    gap: 12px;
  }
}
</style>
