<template>
  <el-drawer
    :model-value="modelValue"
    title="回收站"
    size="520px"
    destroy-on-close
    @open="loadTrashFiles"
    @close="emit('update:modelValue', false)"
  >
    <section class="trash-drawer">
      <el-alert
        type="info"
        :closable="false"
        show-icon
        title="回收站文件不会进入最终打包，可按权限恢复到目录。"
      />

      <el-form label-position="top" class="trash-drawer__restore-target">
        <el-form-item label="恢复目录">
          <el-select v-model="restoreDirectoryId" placeholder="选择恢复目录" filterable>
            <el-option
              v-for="directory in flatDirectories"
              :key="directory.id"
              :label="directory.path"
              :value="directory.id"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="trashFiles" empty-text="暂无回收站文件" height="calc(100vh - 260px)">
        <el-table-column prop="name" label="文件名" min-width="180" show-overflow-tooltip />
        <el-table-column prop="deletedBy" label="删除人" width="96" show-overflow-tooltip />
        <el-table-column label="删除时间" width="150">
          <template #default="{ row }">
            {{ formatDateTime(row.deletedAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="96" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="canRestore"
              link
              type="primary"
              :loading="restoringFileId === row.fileId"
              @click="handleRestore(row)"
            >
              恢复
            </el-button>
            <el-text v-else type="info" size="small">无权限</el-text>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';

import { listTrashFiles, restoreFile } from '@/services/fileApi';

import type { Directory, TrashFile } from '@/types/project';

interface DirectoryOption {
  id: string;
  path: string;
}

const props = withDefaults(defineProps<{
  modelValue: boolean;
  projectId: string;
  directories: Directory[];
  canRestore?: boolean;
}>(), {
  canRestore: true,
});

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  restored: [];
}>();

const loading = ref(false);
const trashFiles = ref<TrashFile[]>([]);
const restoreDirectoryId = ref('');
const restoringFileId = ref('');

const flatDirectories = computed(() => flattenDirectories(props.directories));

watch(
  flatDirectories,
  (directories) => {
    // 当前恢复目录失效时回退到第一个可选目录。
    if (!directories.some((directory) => directory.id === restoreDirectoryId.value)) {
      restoreDirectoryId.value = directories[0]?.id ?? '';
    }
  },
  { immediate: true },
);

/**
 * 将目录树拍平成下拉框选项。
 *
 * @param directories 目录树节点
 * @param parentPath 父级路径
 * @returns 可选择目录列表
 */
function flattenDirectories(directories: Directory[], parentPath = ''): DirectoryOption[] {
  return directories.flatMap((directory) => {
    const path = parentPath ? `${parentPath} / ${directory.name}` : directory.name;

    return [
      { id: directory.id, path },
      ...flattenDirectories(directory.children ?? [], path),
    ];
  });
}

/**
 * 读取项目回收站文件。
 */
async function loadTrashFiles(): Promise<void> {
  if (!props.projectId) {
    trashFiles.value = [];
    return;
  }

  loading.value = true;

  try {
    trashFiles.value = await listTrashFiles(props.projectId);
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '回收站加载失败');
  } finally {
    loading.value = false;
  }
}

/**
 * 将回收站文件恢复到当前选中的目录。
 *
 * @param file 待恢复文件
 */
async function handleRestore(file: TrashFile): Promise<void> {
  if (!restoreDirectoryId.value) {
    ElMessage.warning('请先选择恢复目录');
    return;
  }

  restoringFileId.value = file.fileId;

  try {
    await restoreFile({
      projectId: props.projectId,
      fileId: file.fileId,
      restoreDirectoryId: restoreDirectoryId.value,
    });
    ElMessage.success('文件已恢复');
    await loadTrashFiles();
    emit('restored');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '文件恢复失败');
  } finally {
    restoringFileId.value = '';
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
.trash-drawer {
  display: grid;
  gap: 16px;
}

.trash-drawer__restore-target {
  padding: 14px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fafbfc;
}
</style>
