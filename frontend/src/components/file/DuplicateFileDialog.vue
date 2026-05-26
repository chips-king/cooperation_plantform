<template>
  <el-dialog
    :model-value="modelValue"
    title="同名文件处理"
    width="460px"
    destroy-on-close
    @close="handleCancel"
  >
    <section class="duplicate-dialog">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        title="当前目录已存在同名文件，请选择上传策略。"
      />

      <dl class="duplicate-dialog__meta">
        <div>
          <dt>待上传文件</dt>
          <dd>{{ fileName || '未选择文件' }}</dd>
        </div>
        <div>
          <dt>已存在文件</dt>
          <dd>{{ existingFileName || fileName || '同名文件' }}</dd>
        </div>
      </dl>

      <el-radio-group v-model="selectedPolicy" class="duplicate-dialog__options">
        <el-radio-button label="覆盖原文件" value="overwrite">覆盖原文件</el-radio-button>
        <el-radio-button label="重命名上传" value="rename">重命名上传</el-radio-button>
        <el-radio-button label="保留新版本" value="new_version">保留新版本</el-radio-button>
      </el-radio-group>

      <el-input
        v-if="selectedPolicy === 'rename'"
        v-model="renameValue"
        maxlength="120"
        show-word-limit
        placeholder="填写新的文件名"
      />
    </section>

    <template #footer>
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">确认上传</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';

import type { DuplicateFilePolicy } from '@/types/project';

/**
 * 同名文件弹窗确认结果。
 */
export interface DuplicateFileConfirmPayload {
  policy: DuplicateFilePolicy;
  renamedFileName?: string;
}

const props = withDefaults(defineProps<{
  modelValue: boolean;
  fileName: string;
  existingFileName?: string;
  loading?: boolean;
}>(), {
  existingFileName: '',
  loading: false,
});

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  confirm: [payload: DuplicateFileConfirmPayload];
  cancel: [];
}>();

const selectedPolicy = ref<DuplicateFilePolicy>('overwrite');
const renameValue = ref('');

const defaultRenameValue = computed(() => {
  const dotIndex = props.fileName.lastIndexOf('.');

  // 保留扩展名并追加副本后缀，便于用户直接确认重命名策略。
  if (dotIndex > 0) {
    return `${props.fileName.slice(0, dotIndex)}-副本${props.fileName.slice(dotIndex)}`;
  }

  return props.fileName ? `${props.fileName}-副本` : '';
});

watch(
  () => props.modelValue,
  (visible) => {
    // 每次打开弹窗都重置为默认策略，避免沿用上一次用户选择。
    if (visible) {
      selectedPolicy.value = 'overwrite';
      renameValue.value = defaultRenameValue.value;
    }
  },
);

/**
 * 关闭弹窗并通知父组件取消上传。
 */
function handleCancel(): void {
  emit('update:modelValue', false);
  emit('cancel');
}

/**
 * 校验当前策略并把用户选择返回给上传流程。
 */
function handleConfirm(): void {
  if (selectedPolicy.value === 'rename' && !renameValue.value.trim()) {
    ElMessage.warning('请填写新的文件名');
    return;
  }

  emit('confirm', {
    policy: selectedPolicy.value,
    renamedFileName: selectedPolicy.value === 'rename' ? renameValue.value.trim() : undefined,
  });
}
</script>

<style scoped>
.duplicate-dialog {
  display: grid;
  gap: 18px;
}

.duplicate-dialog__meta {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 14px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  background: #fafbfc;
}

.duplicate-dialog__meta div {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 10px;
}

.duplicate-dialog__meta dt {
  color: #687386;
}

.duplicate-dialog__meta dd {
  min-width: 0;
  margin: 0;
  overflow-wrap: anywhere;
  color: #20242c;
}

.duplicate-dialog__options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
