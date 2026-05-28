<template>
  <div class="file-list-block">
    <!-- 头部：标题 + 上传按钮 -->
    <div class="file-list-block__header">
      <div class="file-list-block__title">
        <FolderOpened class="file-list-block__title-icon" />
        <span>项目文件</span>
        <span class="file-list-block__count">
          {{ directories.length > 0 ? directories.length + ' 个目录' : '' }}
          {{ directories.length > 0 && (files?.length ?? 0) > 0 ? '，' : '' }}
          {{ (files?.length ?? 0) > 0 ? (files?.length ?? 0) + ' 个文件' : '' }}
        </span>
      </div>
      <div class="file-list-block__actions">
        <el-dropdown trigger="click" @command="handleUploadCommand">
          <el-button type="primary" size="small" :disabled="uploading">
            <Upload style="width: 14px; height: 14px; margin-right: 4px" />上传<el-icon style="margin-left: 4px"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="file">上传文件</el-dropdown-item>
              <el-dropdown-item command="folder">上传文件夹</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <!-- 隐藏的文件和文件夹 input -->
        <input ref="fileInputRef" class="file-list-block__folder-input" type="file" @change="handleFileInputPick">
        <input ref="folderInputRef" class="file-list-block__folder-input" type="file" multiple webkitdirectory @change="handleFolderPick">
      </div>
    </div>

    <!-- 上传进度 -->
    <div v-if="uploading" class="file-list-block__upload-bar">
      <el-progress :percentage="uploadProgress" :stroke-width="4" :show-text="false" style="flex: 1" />
      <span class="file-list-block__upload-text">{{ uploadStatusText }}</span>
    </div>

    <!-- 文件列表 -->
    <div class="file-list-block__table">
      <!-- 表头 -->
      <div class="file-list-block__row file-list-block__row--header">
        <div class="file-list-block__cell file-list-block__cell--name">名称</div>
        <div class="file-list-block__cell file-list-block__cell--size">大小</div>
        <div class="file-list-block__cell file-list-block__cell--time">最后上传</div>
        <div class="file-list-block__cell file-list-block__cell--actions">操作</div>
      </div>

      <!-- 目录行 -->
      <div
        v-for="dir in directories"
        :key="'dir-' + dir.id"
        class="file-list-block__row"
        @click="$emit('open-directory', dir)"
      >
        <div class="file-list-block__cell file-list-block__cell--name">
          <FolderOpened class="file-list-block__icon file-list-block__icon--folder" />
          <span class="file-list-block__name-text">{{ dir.name }}</span>
          <el-tag size="small" effect="plain" class="file-list-block__dir-count">
            {{ dir.files?.length ?? 0 }} 个文件
          </el-tag>
        </div>
        <div class="file-list-block__cell file-list-block__cell--size">-</div>
        <div class="file-list-block__cell file-list-block__cell--time">-</div>
        <div class="file-list-block__cell file-list-block__cell--actions" @click.stop>
          <el-button link size="small" type="danger" @click="$emit('delete-directory', dir)">
            <Delete style="width: 14px; height: 14px" />
          </el-button>
        </div>
      </div>

      <!-- 文件行 -->
      <div
        v-for="file in files"
        :key="'file-' + file.fileId"
        class="file-list-block__row file-list-block__row--file"
        @click="$emit('open-file', file)"
      >
        <div class="file-list-block__cell file-list-block__cell--name">
          <Document class="file-list-block__icon file-list-block__icon--file" />
          <span class="file-list-block__name-text">{{ file.name }}</span>
        </div>
        <div class="file-list-block__cell file-list-block__cell--size">
          {{ formatFileSize(file.size) }}
        </div>
        <div class="file-list-block__cell file-list-block__cell--time">
          {{ formatUploadTime(file.uploadedAt) }}
        </div>
        <div class="file-list-block__cell file-list-block__cell--actions" @click.stop>
          <el-button link size="small" @click="$emit('download-file', file)">
            <Download style="width: 14px; height: 14px" />
          </el-button>
          <el-button link size="small" type="danger" @click="$emit('delete-file', file)">
            <Delete style="width: 14px; height: 14px" />
          </el-button>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="isEmpty && !loading" class="file-list-block__empty">
        <FolderOpened style="width: 48px; height: 48px; color: #dcdfe6" />
        <p style="margin: 8px 0 4px; font-size: 15px; font-weight: 500; color: #606266">此项目还没有文件</p>
        <p style="margin: 0; font-size: 13px; color: #909399">点击「上传」按钮添加文件或文件夹</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { ArrowDown, Delete, Document, Download, FolderOpened, Upload } from '@element-plus/icons-vue';

import type { Directory, FileAsset } from '@/types/project';

const props = defineProps<{
  directories: Directory[];
  /** 根级别文件（直接上传到项目根目录的文件） */
  files?: FileAsset[];
  loading?: boolean;
}>();

const emit = defineEmits<{
  'open-directory': [dir: Directory];
  'open-file': [file: FileAsset];
  'upload-files': [files: File[]];
  'download-file': [file: FileAsset];
  'delete-file': [file: FileAsset];
  'delete-directory': [dir: Directory];
}>();

/** 文件原生 input 引用 */
const fileInputRef = ref<HTMLInputElement | null>(null);
/** 文件夹原生 input 引用 */
const folderInputRef = ref<HTMLInputElement | null>(null);
/** 是否正在上传 */
const uploading = ref(false);
/** 上传进度百分比 */
const uploadProgress = ref(0);
/** 上传状态描述 */
const uploadStatusText = ref('');

/** 列表是否为空（无目录且无文件） */
const isEmpty = computed(() => props.directories.length === 0 && (props.files?.length ?? 0) === 0);

/**
 * 处理上传下拉菜单命令。
 */
function handleUploadCommand(command: string): void {
  if (command === 'file') {
    fileInputRef.value?.click();
  } else if (command === 'folder') {
    folderInputRef.value?.click();
  }
}

/**
 * 处理单文件 input 选择。
 */
function handleFileInputPick(event: Event): void {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = '';
  if (file) {
    emit('upload-files', [file]);
  }
}

/**
 * 处理文件夹选择，收集所有文件后发送给父组件上传。
 */
async function handleFolderPick(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement;
  const fileList = Array.from(input.files ?? []);
  input.value = '';
  if (fileList.length === 0) return;
  emit('upload-files', fileList);
}

/**
 * 批量上传文件，由父组件提供实际上传逻辑。
 * 内部维护进度状态。
 */
async function runUpload(fileList: File[], uploadOne: (file: File, relativePath: string) => Promise<void>): Promise<void> {
  uploading.value = true;
  uploadProgress.value = 0;
  let success = 0;

  for (let i = 0; i < fileList.length; i++) {
    const file = fileList[i];
    const relativePath = (file as File & { webkitRelativePath?: string }).webkitRelativePath || file.name;
    uploadStatusText.value = `上传中: ${file.name} (${i + 1}/${fileList.length})`;
    try {
      await uploadOne(file, relativePath);
      success += 1;
    } catch {
      // 单个失败不中断
    }
    uploadProgress.value = Math.round(((i + 1) / fileList.length) * 100);
  }

  uploadStatusText.value = `上传完成：${success}/${fileList.length} 成功`;
  uploading.value = false;
}

defineExpose({ runUpload });

/**
 * 格式化文件大小为人类可读字符串。
 */
function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return `${(bytes / Math.pow(1024, i)).toFixed(i > 0 ? 1 : 0)} ${units[i]}`;
}

/**
 * 格式化上传时间为相对时间描述。
 *
 * @param iso ISO 格式时间字符串或为空
 * @returns 相对时间字符串，如"3 分钟前"、"2 小时前"、"5 天前"等
 */
function formatUploadTime(iso: string | undefined | null): string {
  if (!iso) return '-';
  const date = new Date(iso);
  if (isNaN(date.getTime())) return '-';
  const diffMs = Date.now() - date.getTime();
  const diffMin = Math.floor(diffMs / 60_000);
  if (diffMin < 1) return '刚刚';
  if (diffMin < 60) return `${diffMin} 分钟前`;
  const diffHour = Math.floor(diffMin / 60);
  if (diffHour < 24) return `${diffHour} 小时前`;
  const diffDay = Math.floor(diffHour / 24);
  if (diffDay < 30) return `${diffDay} 天前`;
  return date.toLocaleDateString('zh-CN');
}
</script>

<style scoped>
.file-list-block {
  background: var(--cb-bg-card);
  border: 1px solid var(--cb-border);
  border-radius: var(--cb-radius-md);
  overflow: hidden;
}

.file-list-block__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 18px;
  background: var(--cb-bg-page);
  border-bottom: 1px solid var(--cb-border-light);
}

.file-list-block__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 14px;
  color: var(--cb-text-primary);
}

.file-list-block__title-icon {
  width: 18px;
  height: 18px;
  color: var(--cb-text-secondary);
}

.file-list-block__count {
  color: var(--cb-text-muted);
  font-weight: 400;
  font-size: 12px;
}

.file-list-block__actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.file-list-block__table {
  min-height: 80px;
}

/* 行通用样式 */
.file-list-block__row {
  display: flex;
  align-items: center;
  padding: 8px 18px;
  border-bottom: 1px solid var(--cb-border-light);
  cursor: pointer;
  transition: background 0.1s;
}

.file-list-block__row:last-child {
  border-bottom: none;
}

.file-list-block__row:hover {
  background: #f6f8fa;
}

.file-list-block__row--header {
  background: var(--cb-bg-page);
  cursor: default;
  font-size: 12px;
  font-weight: 600;
  color: var(--cb-text-secondary);
  padding: 6px 18px;
}

.file-list-block__row--header:hover {
  background: var(--cb-bg-page);
}

/* 单元格 */
.file-list-block__cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.file-list-block__cell--name {
  flex: 1;
  min-width: 0;
}

.file-list-block__cell--size {
  width: 90px;
  flex-shrink: 0;
  font-size: 13px;
  color: var(--cb-text-secondary);
}

.file-list-block__cell--time {
  width: 120px;
  flex-shrink: 0;
  font-size: 13px;
  color: var(--cb-text-secondary);
}

.file-list-block__cell--actions {
  width: 80px;
  flex-shrink: 0;
  justify-content: flex-end;
  gap: 2px;
}

/* 图标 */
.file-list-block__icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

.file-list-block__icon--folder {
  color: #5470c6;
}

.file-list-block__icon--file {
  color: #67c23a;
}

/* 名称文字 */
.file-list-block__name-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--cb-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-list-block__dir-count {
  margin-left: 8px;
  flex-shrink: 0;
}

/* 隐藏的文件夹选择器 */
.file-list-block__folder-input {
  display: none;
}

/* 上传进度条 */
.file-list-block__upload-bar {
  padding: 8px 18px;
  background: var(--cb-bg-page);
  border-bottom: 1px solid var(--cb-border-light);
  display: flex;
  align-items: center;
  gap: 12px;
}

.file-list-block__upload-text {
  font-size: 12px;
  color: var(--cb-text-muted);
  white-space: nowrap;
}

/* 空状态 */
.file-list-block__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 48px 0;
  color: var(--cb-text-muted);
}

@media (max-width: 768px) {
  .file-list-block__cell--size,
  .file-list-block__cell--time,
  .file-list-block__cell--actions {
    display: none;
  }
}
</style>
