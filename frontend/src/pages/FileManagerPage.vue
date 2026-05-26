<template>
  <MainLayout>
    <template #title>
      <span>文件管理</span>
    </template>

    <template #actions>
      <el-button :icon="Refresh" :loading="loading" @click="loadDirectoryTree">刷新</el-button>
      <el-button :icon="Delete" @click="trashVisible = true">回收站</el-button>
    </template>

    <template #aside>
      <section class="file-manager__aside">
        <header>
          <strong>目录树</strong>
          <small>{{ selectedDirectoryPath || '请选择目录' }}</small>
        </header>
        <el-tree
          :data="directories"
          :props="treeProps"
          node-key="id"
          default-expand-all
          highlight-current
          empty-text="暂无目录"
          @node-click="handleDirectorySelect"
        />
      </section>
    </template>

    <section class="file-manager">
      <el-alert
        v-if="!projectId"
        type="warning"
        :closable="false"
        show-icon
        title="未识别到当前项目，请从项目工作台进入文件管理。"
      />

      <div
        class="file-manager__toolbar"
        :class="{ 'file-manager__toolbar--dragging': dragActive }"
        @dragenter.prevent="handleDragEnter"
        @dragover.prevent="handleDragEnter"
        @dragleave.prevent="handleDragLeave"
        @drop.prevent="handleFolderDrop"
      >
        <div>
          <div class="file-manager__title-row">
            <el-tooltip content="返回上一级目录" placement="top">
              <el-button
                circle
                :icon="ArrowUp"
                :disabled="!selectedParentDirectory"
                aria-label="返回上一级目录"
                @click="handleBackToParent"
              />
            </el-tooltip>
            <h2>{{ selectedDirectory?.name || '文件列表' }}</h2>
          </div>
          <p>{{ selectedDirectoryPath || '请选择目录' }}</p>
          <p>支持单文件上传、拖拽文件夹、下载、移动和删除。</p>
        </div>

        <div class="file-manager__toolbar-actions">
          <el-select
            v-model="moveTargetDirectoryId"
            placeholder="移动目标目录"
            filterable
            class="file-manager__move-target"
          >
            <el-option
              v-for="directory in flatDirectories"
              :key="directory.id"
              :label="directory.path"
              :value="directory.id"
            />
          </el-select>
          <el-upload :show-file-list="false" :auto-upload="false" :on-change="handleUploadPick">
            <el-button type="primary" :icon="Upload" :disabled="!selectedDirectory || uploading">
              上传文件
            </el-button>
          </el-upload>
          <input
            ref="folderInputRef"
            class="file-manager__folder-input"
            type="file"
            multiple
            webkitdirectory
            @change="handleFolderInputChange"
          >
          <el-button :icon="Upload" :disabled="!selectedDirectory || uploading" @click="openFolderPicker">
            上传文件夹
          </el-button>
          <el-button :icon="FolderAdd" :disabled="!selectedDirectory || uploading" @click="handleCreateDirectory">
            新建目录
          </el-button>
          <el-button
            :icon="Delete"
            :disabled="!canDeleteSelectedDirectory || uploading"
            @click="handleDeleteDirectory"
          >
            删除目录
          </el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="currentFiles" empty-text="当前目录暂无文件">
        <el-table-column prop="name" label="文件名" min-width="220" show-overflow-tooltip />
        <el-table-column label="大小" width="120">
          <template #default="{ row }">
            {{ formatFileSize(row.size) }}
          </template>
        </el-table-column>
        <el-table-column prop="mimeType" label="类型" min-width="150" show-overflow-tooltip />
        <el-table-column label="版本" width="90">
          <template #default="{ row }">v{{ row.versionNo }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'info'">
              {{ fileStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleDownload(row)">下载</el-button>
            <el-button link type="primary" :disabled="!canMoveToTarget(row)" @click="handleMove(row)">移动</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <DuplicateFileDialog
      v-model="duplicateDialogVisible"
      :file-name="pendingUploadFile?.name || ''"
      :existing-file-name="duplicateFileName"
      :loading="uploading"
      @confirm="handleDuplicateConfirm"
      @cancel="pendingUploadFile = null"
    />

    <TrashDrawer
      v-model="trashVisible"
      :project-id="projectId"
      :directories="directories"
      @restored="loadDirectoryTree"
    />
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox, type UploadFile } from 'element-plus';
import { ArrowUp, Delete, FolderAdd, Refresh, Upload } from '@element-plus/icons-vue';

import DuplicateFileDialog, { type DuplicateFileConfirmPayload } from '@/components/file/DuplicateFileDialog.vue';
import TrashDrawer from '@/components/file/TrashDrawer.vue';
import MainLayout from '@/layouts/MainLayout.vue';
import {
  createDirectory,
  deleteDirectory,
  deleteFile,
  downloadFile,
  getDirectoryTree,
  moveFile,
  uploadFile,
} from '@/services/fileApi';
import { useProjectStore } from '@/stores/project';

import type { Directory, DuplicateFilePolicy, FileAsset, FileAssetStatus } from '@/types/project';

interface DirectoryOption {
  id: string;
  path: string;
}

interface FolderUploadItem {
  file: File;
  relativePath: string;
}

const route = useRoute();
const router = useRouter();
const projectStore = useProjectStore();

const loading = ref(false);
const uploading = ref(false);
const dragActive = ref(false);
const directories = ref<Directory[]>([]);
const selectedDirectoryId = ref('');
const moveTargetDirectoryId = ref('');
const pendingUploadFile = ref<File | null>(null);
const folderInputRef = ref<HTMLInputElement | null>(null);
const duplicateDialogVisible = ref(false);
const duplicateFileName = ref('');
const trashVisible = ref(false);

const treeProps = {
  label: 'name',
  children: 'children',
};

const projectId = computed(() => {
  const routeProjectId = route.params.projectId;
  return String(Array.isArray(routeProjectId) ? routeProjectId[0] : routeProjectId || projectStore.currentProject?.id || '');
});

const flatDirectories = computed(() => flattenDirectories(directories.value));
const selectedDirectory = computed(() => findDirectoryById(directories.value, selectedDirectoryId.value));
const selectedParentDirectory = computed(() => findParentDirectory(directories.value, selectedDirectoryId.value));
const selectedDirectoryPath = computed(() => {
  return flatDirectories.value.find((directory) => directory.id === selectedDirectoryId.value)?.path ?? '';
});
const currentFiles = computed(() => selectedDirectory.value?.files ?? []);
const canDeleteSelectedDirectory = computed(() => {
  return Boolean(
    selectedDirectory.value
    && selectedDirectory.value.parentId
    && currentFiles.value.length === 0
    && (selectedDirectory.value.children ?? []).length === 0,
  );
});

watch(
  flatDirectories,
  (nextDirectories) => {
    const routeDirectoryId = currentRouteDirectoryId();

    if (routeDirectoryId && nextDirectories.some((directory) => directory.id === routeDirectoryId)) {
      selectedDirectoryId.value = routeDirectoryId;
    }

    // 首次加载或所选目录被移除时，自动选择第一个目录。
    if (!nextDirectories.some((directory) => directory.id === selectedDirectoryId.value)) {
      selectedDirectoryId.value = nextDirectories[0]?.id ?? '';
    }

    if (!nextDirectories.some((directory) => directory.id === moveTargetDirectoryId.value)) {
      moveTargetDirectoryId.value = nextDirectories[0]?.id ?? '';
    }
  },
  { immediate: true },
);

watch(
  () => route.query.directoryId,
  () => {
    const routeDirectoryId = currentRouteDirectoryId();
    if (routeDirectoryId && flatDirectories.value.some((directory) => directory.id === routeDirectoryId)) {
      selectedDirectoryId.value = routeDirectoryId;
    }
  },
);

onMounted(() => {
  void loadDirectoryTree();
});

/**
 * 查询当前项目目录树。
 */
async function loadDirectoryTree(): Promise<void> {
  if (!projectId.value) {
    directories.value = [];
    return;
  }

  loading.value = true;

  try {
    const response = await getDirectoryTree(projectId.value);
    directories.value = response.directories;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '目录树加载失败');
  } finally {
    loading.value = false;
  }
}

/**
 * 选择文件列表对应目录。
 *
 * @param directory 被点击的目录节点
 */
function handleDirectorySelect(directory: Directory): void {
  selectDirectory(directory.id);
}

/**
 * 返回父级目录。
 */
function handleBackToParent(): void {
  if (selectedParentDirectory.value) {
    selectDirectory(selectedParentDirectory.value.id);
  }
}

/**
 * 在当前目录下创建子目录。
 */
async function handleCreateDirectory(): Promise<void> {
  if (!selectedDirectory.value) {
    ElMessage.warning('请先选择父目录');
    return;
  }

  const name = window.prompt('请输入新目录名称');
  if (!name?.trim()) {
    return;
  }

  try {
    const directory = await createDirectory({
      projectId: projectId.value,
      parentDirectoryId: selectedDirectory.value.id,
      name: name.trim(),
    });
    ElMessage.success('目录已创建');
    await loadDirectoryTree();
    selectDirectory(directory.id);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '目录创建失败');
  }
}

/**
 * 删除当前选中的空目录。
 */
async function handleDeleteDirectory(): Promise<void> {
  if (!selectedDirectory.value || !canDeleteSelectedDirectory.value) {
    ElMessage.warning('只能删除没有文件和子目录的非根目录');
    return;
  }

  try {
    const result = await deleteDirectory({
      projectId: projectId.value,
      directoryId: selectedDirectory.value.id,
    });
    ElMessage.success('目录已删除');
    await loadDirectoryTree();
    selectDirectory(result.parentDirectoryId);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '目录删除失败');
  }
}

/**
 * 处理用户选择的上传文件。
 *
 * @param uploadFile Element Plus 上传文件对象
 */
function handleUploadPick(uploadFile: UploadFile): void {
  if (!selectedDirectory.value || !uploadFile.raw) {
    ElMessage.warning('请先选择上传目录');
    return;
  }

  pendingUploadFile.value = uploadFile.raw;
  const existedFile = currentFiles.value.find((file) => file.name === uploadFile.raw?.name);

  // 前端先基于当前目录列表识别同名文件，最终冲突仍以后端上传校验为准。
  if (existedFile) {
    duplicateFileName.value = existedFile.name;
    duplicateDialogVisible.value = true;
    return;
  }

  void submitUpload();
}

/**
 * 打开浏览器原生文件夹选择器。
 */
function openFolderPicker(): void {
  folderInputRef.value?.click();
}

/**
 * 标记拖拽进入上传区域。
 */
function handleDragEnter(): void {
  if (selectedDirectory.value && !uploading.value) {
    dragActive.value = true;
  }
}

/**
 * 标记拖拽离开上传区域。
 */
function handleDragLeave(): void {
  dragActive.value = false;
}

/**
 * 处理文件夹选择器变更。
 *
 * @param event 原生文件选择事件
 */
async function handleFolderInputChange(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement;
  const files = Array.from(input.files ?? []);
  await uploadFolderItems(files.map((file) => ({
    file,
    relativePath: browserRelativePath(file),
  })));
  input.value = '';
}

/**
 * 处理拖拽文件夹或多文件上传。
 *
 * @param event 拖拽事件
 */
async function handleFolderDrop(event: DragEvent): Promise<void> {
  event.preventDefault();
  dragActive.value = false;
  const items = await collectDroppedFiles(event.dataTransfer);
  await uploadFolderItems(items);
}

/**
 * 从拖拽数据中提取文件与相对路径。
 *
 * @param dataTransfer 浏览器拖拽数据
 * @returns 待上传文件集合
 */
async function collectDroppedFiles(dataTransfer: DataTransfer | null): Promise<FolderUploadItem[]> {
  if (!dataTransfer) {
    return [];
  }

  const entries = Array.from(dataTransfer.items ?? [])
    .map((item) => item.webkitGetAsEntry?.())
    .filter((entry): entry is FileSystemEntry => Boolean(entry));
  if (entries.length > 0) {
    const nestedItems = await Promise.all(entries.map((entry) => readEntryFiles(entry, '')));
    return nestedItems.flat();
  }

  return Array.from(dataTransfer.files ?? []).map((file) => ({
    file,
    relativePath: browserRelativePath(file),
  }));
}

/**
 * 递归读取拖入目录下的所有文件。
 *
 * @param entry 文件系统入口
 * @param parentPath 父级相对路径
 * @returns 待上传文件集合
 */
async function readEntryFiles(entry: FileSystemEntry, parentPath: string): Promise<FolderUploadItem[]> {
  const currentPath = parentPath ? `${parentPath}/${entry.name}` : entry.name;

  if (entry.isFile) {
    const file = await new Promise<File>((resolve, reject) => {
      (entry as FileSystemFileEntry).file(resolve, reject);
    });
    return [{ file, relativePath: currentPath }];
  }

  const reader = (entry as FileSystemDirectoryEntry).createReader();
  const children = await readAllDirectoryEntries(reader);
  const nestedItems = await Promise.all(children.map((child) => readEntryFiles(child, currentPath)));
  return nestedItems.flat();
}

/**
 * 分批读取目录入口，直到浏览器返回空数组。
 *
 * @param reader 目录读取器
 * @returns 子入口集合
 */
async function readAllDirectoryEntries(reader: FileSystemDirectoryReader): Promise<FileSystemEntry[]> {
  const entries: FileSystemEntry[] = [];

  while (true) {
    const batch = await new Promise<FileSystemEntry[]>((resolve, reject) => {
      reader.readEntries(resolve, reject);
    });
    if (batch.length === 0) {
      return entries;
    }
    entries.push(...batch);
  }
}

/**
 * 批量上传文件夹中的文件。
 *
 * @param items 待上传文件和相对路径集合
 */
async function uploadFolderItems(items: FolderUploadItem[]): Promise<void> {
  if (!selectedDirectory.value) {
    ElMessage.warning('请先选择上传目录');
    return;
  }
  if (items.length === 0) {
    ElMessage.warning('未识别到可上传文件');
    return;
  }

  uploading.value = true;
  let successCount = 0;

  try {
    for (const item of items) {
      await uploadFile({
        projectId: projectId.value,
        directoryId: selectedDirectory.value.id,
        file: item.file,
        relativePath: item.relativePath,
        duplicatePolicy: 'new_version',
      });
      successCount += 1;
    }
    ElMessage.success(`文件夹上传完成：${successCount}/${items.length}`);
    await loadDirectoryTree();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : `文件夹上传失败，已成功 ${successCount}/${items.length}`);
  } finally {
    uploading.value = false;
  }
}

/**
 * 读取浏览器提供的文件夹相对路径。
 *
 * @param file 浏览器文件对象
 * @returns 相对路径，普通文件返回文件名
 */
function browserRelativePath(file: File): string {
  return (file as File & { webkitRelativePath?: string }).webkitRelativePath || file.name;
}

/**
 * 根据同名策略继续上传。
 *
 * @param payload 弹窗返回的同名策略
 */
function handleDuplicateConfirm(payload: DuplicateFileConfirmPayload): void {
  void submitUpload(payload.policy);
}

/**
 * 上传当前待处理文件。
 *
 * @param duplicatePolicy 同名文件处理策略
 */
async function submitUpload(duplicatePolicy?: DuplicateFilePolicy): Promise<void> {
  if (!pendingUploadFile.value || !selectedDirectory.value) {
    return;
  }

  uploading.value = true;

  try {
    await uploadFile({
      projectId: projectId.value,
      directoryId: selectedDirectory.value.id,
      file: pendingUploadFile.value,
      duplicatePolicy: duplicatePolicy ?? 'new_version',
      relativePath: pendingUploadFile.value.name,
    });
    ElMessage.success('文件上传成功');
    duplicateDialogVisible.value = false;
    pendingUploadFile.value = null;
    await loadDirectoryTree();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '文件上传失败');
  } finally {
    uploading.value = false;
  }
}

/**
 * 下载指定文件。
 *
 * @param file 文件摘要
 */
async function handleDownload(file: FileAsset): Promise<void> {
  try {
    const blob = await downloadFile(file.fileId);
    const url = window.URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = file.name;
    anchor.click();
    window.URL.revokeObjectURL(url);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '文件下载失败');
  }
}

/**
 * 移动文件到当前选择的目标目录。
 *
 * @param file 文件摘要
 */
async function handleMove(file: FileAsset): Promise<void> {
  if (!moveTargetDirectoryId.value) {
    ElMessage.warning('请先选择移动目标目录');
    return;
  }

  try {
    await moveFile({
      projectId: projectId.value,
      fileId: file.fileId,
      targetDirectoryId: moveTargetDirectoryId.value,
    });
    ElMessage.success('文件已移动');
    await loadDirectoryTree();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '文件移动失败');
  }
}

/**
 * 删除文件并移入回收站。
 *
 * @param file 文件摘要
 */
async function handleDelete(file: FileAsset): Promise<void> {
  try {
    await ElMessageBox.confirm(`确认将“${file.name}”移入回收站吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '移入回收站',
      cancelButtonText: '取消',
    });
    await deleteFile({ projectId: projectId.value, fileId: file.fileId });
    ElMessage.success('文件已移入回收站');
    await loadDirectoryTree();
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '文件删除失败');
    }
  }
}

/**
 * 判断文件是否可以移动到当前目标目录。
 *
 * @param file 文件摘要
 * @returns 可移动时返回 true
 */
function canMoveToTarget(file: FileAsset): boolean {
  return Boolean(file.fileId && moveTargetDirectoryId.value && moveTargetDirectoryId.value !== selectedDirectoryId.value);
}

/**
 * 选中目录并同步地址栏 query。
 *
 * @param directoryId 目录标识
 */
function selectDirectory(directoryId: string): void {
  selectedDirectoryId.value = directoryId;
  void router.push({
    name: 'project-files',
    params: { projectId: projectId.value },
    query: { directoryId },
  });
}

/**
 * 读取路由中的目录标识。
 *
 * @returns 目录标识，不存在时返回空字符串
 */
function currentRouteDirectoryId(): string {
  const queryValue = route.query.directoryId;
  return String(Array.isArray(queryValue) ? queryValue[0] : queryValue || '');
}

/**
 * 将目录树拍平成带路径的列表。
 *
 * @param tree 目录树
 * @param parentPath 父目录路径
 * @returns 拍平后的目录选项
 */
function flattenDirectories(tree: Directory[], parentPath = ''): DirectoryOption[] {
  return tree.flatMap((directory) => {
    const path = parentPath ? `${parentPath} / ${directory.name}` : directory.name;

    return [
      { id: directory.id, path },
      ...flattenDirectories(directory.children ?? [], path),
    ];
  });
}

/**
 * 在目录树中按标识查找目录。
 *
 * @param tree 目录树
 * @param directoryId 目录标识
 * @returns 命中的目录，未命中时返回 null
 */
function findDirectoryById(tree: Directory[], directoryId: string): Directory | null {
  for (const directory of tree) {
    if (directory.id === directoryId) {
      return directory;
    }

    const matchedDirectory = findDirectoryById(directory.children ?? [], directoryId);

    if (matchedDirectory) {
      return matchedDirectory;
    }
  }

  return null;
}

/**
 * 在目录树中查找指定目录的父目录。
 *
 * @param tree 目录树
 * @param directoryId 目录标识
 * @param parent 当前递归父目录
 * @returns 父目录，根目录或未命中时返回 null
 */
function findParentDirectory(tree: Directory[], directoryId: string, parent: Directory | null = null): Directory | null {
  for (const directory of tree) {
    if (directory.id === directoryId) {
      return parent;
    }

    const matchedParent = findParentDirectory(directory.children ?? [], directoryId, directory);

    if (matchedParent) {
      return matchedParent;
    }
  }

  return null;
}

/**
 * 格式化文件大小。
 *
 * @param size 字节数
 * @returns 人类可读大小
 */
function formatFileSize(size: number): string {
  if (size < 1024) {
    return `${size} B`;
  }

  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`;
  }

  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

/**
 * 转换文件状态文案。
 *
 * @param status 文件状态
 * @returns 中文状态文案
 */
function fileStatusText(status: FileAssetStatus): string {
  const statusMap: Record<string, string> = {
    active: '正常',
    trashed: '回收站',
    superseded: '已覆盖',
  };

  return statusMap[status] ?? status;
}
</script>

<style scoped>
.file-manager {
  display: grid;
  gap: 18px;
}

.file-manager__aside {
  display: grid;
  gap: 14px;
}

.file-manager__aside header {
  display: grid;
  gap: 4px;
}

.file-manager__aside small,
.file-manager__toolbar p {
  margin: 0;
  color: #687386;
}

.file-manager__toolbar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #ffffff;
}

.file-manager__toolbar--dragging {
  border-color: #409eff;
  background: #ecf5ff;
}

.file-manager__toolbar h2 {
  margin: 0 0 6px;
  font-size: 20px;
}

.file-manager__title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.file-manager__title-row h2 {
  margin: 0;
}

.file-manager__toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.file-manager__move-target {
  width: 220px;
}

.file-manager__folder-input {
  display: none;
}

@media (max-width: 900px) {
  .file-manager__toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .file-manager__toolbar-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .file-manager__move-target {
    width: 100%;
  }
}
</style>
