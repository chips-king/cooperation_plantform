<template>
  <el-drawer
    :model-value="visible"
    title="文件评论"
    direction="rtl"
    size="400px"
    :close-on-click-modal="true"
    @close="$emit('close')"
  >
    <template #header>
      <div class="comment-drawer__header">
        <span>文件评论</span>
        <el-tag v-if="comments.length > 0" size="small" type="info" effect="plain">
          {{ comments.length }}
        </el-tag>
      </div>
    </template>

    <!-- 文件信息 -->
    <div v-if="file" class="comment-drawer__file-info">
      <Document class="comment-drawer__file-icon" />
      <div class="comment-drawer__file-detail">
        <span class="comment-drawer__file-name">{{ file.name }}</span>
        <span class="comment-drawer__file-size">{{ formatFileSize(file.size) }}</span>
      </div>
    </div>

    <el-divider style="margin: 12px 0" />

    <!-- 评论列表 -->
    <div ref="commentListRef" class="comment-drawer__list">
      <div v-if="loading" class="comment-drawer__loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <div v-else-if="comments.length === 0" class="comment-drawer__empty">
        <ChatDotSquare style="width: 32px; height: 32px; color: #c0c4cc" />
        <p>暂无评论，发表第一条评论吧</p>
      </div>

      <div v-for="comment in comments" :key="comment.id" class="comment-drawer__item">
        <div class="comment-drawer__item-header">
          <div class="comment-drawer__avatar">{{ comment.username.charAt(0) }}</div>
          <span class="comment-drawer__username">{{ comment.username }}</span>
          <span class="comment-drawer__time">{{ formatTime(comment.createdAt) }}</span>
          <el-button
            v-if="comment.userId === currentUserId"
            text
            size="small"
            type="danger"
            class="comment-drawer__delete-btn"
            @click="handleDelete(comment.id)"
          >
            删除
          </el-button>
        </div>
        <div class="comment-drawer__item-content">{{ comment.content }}</div>
      </div>
    </div>

    <!-- 评论输入区 -->
    <div class="comment-drawer__input-area">
      <el-input
        v-model="newComment"
        type="textarea"
        :rows="3"
        maxlength="2000"
        show-word-limit
        placeholder="写下你的评论..."
        @keydown.enter.ctrl="handleSubmit"
      />
      <div class="comment-drawer__input-actions">
        <span class="comment-drawer__hint">Ctrl + Enter 发送</span>
        <el-button
          type="primary"
          size="small"
          :loading="submitting"
          :disabled="!newComment.trim()"
          @click="handleSubmit"
        >
          发送评论
        </el-button>
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue';
import { ChatDotSquare, Document, Loading } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';

import { addFileComment, deleteFileComment, getFileComments } from '@/services/fileApi';
import type { FileComment } from '@/services/fileApi';
import { useAuthStore } from '@/stores/auth';

/** 文件摘要信息。 */
interface FileSummary {
  fileId: string;
  name: string;
  size: number;
}

const props = defineProps<{
  visible: boolean;
  file: FileSummary | null;
}>();

defineEmits<{
  close: [];
}>();

const authStore = useAuthStore();
const currentUserId = authStore.currentUser?.id ?? 0;

const comments = ref<FileComment[]>([]);
const loading = ref(false);
const submitting = ref(false);
const newComment = ref('');
const commentListRef = ref<HTMLElement | null>(null);

/**
 * 监听抽屉打开状态，加载评论列表。
 */
watch(
  () => props.visible,
  async (isVisible) => {
    if (isVisible && props.file) {
      await loadComments();
    } else {
      comments.value = [];
      newComment.value = '';
    }
  },
);

/**
 * 加载文件评论列表。
 */
async function loadComments(): Promise<void> {
  if (!props.file) return;
  loading.value = true;
  try {
    const response = await getFileComments(props.file.fileId);
    comments.value = response.comments;
    await nextTick();
    scrollToBottom();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载评论失败');
  } finally {
    loading.value = false;
  }
}

/**
 * 提交新评论。
 */
async function handleSubmit(): Promise<void> {
  const content = newComment.value.trim();
  if (!content || !props.file) return;

  submitting.value = true;
  try {
    await addFileComment(props.file.fileId, content);
    newComment.value = '';
    await loadComments();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '评论发送失败');
  } finally {
    submitting.value = false;
  }
}

/**
 * 删除评论。
 */
async function handleDelete(commentId: number): Promise<void> {
  try {
    await deleteFileComment(commentId);
    comments.value = comments.value.filter((c) => c.id !== commentId);
    ElMessage.success('评论已删除');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除评论失败');
  }
}

/**
 * 滚动评论列表到底部。
 */
function scrollToBottom(): void {
  if (commentListRef.value) {
    commentListRef.value.scrollTop = commentListRef.value.scrollHeight;
  }
}

/**
 * 格式化文件大小。
 */
function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/**
 * 格式化时间显示。
 */
function formatTime(value: string): string {
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
</script>

<style scoped>
.comment-drawer__header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}

.comment-drawer__file-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: var(--cb-bg-page, #f5f7fa);
  border-radius: 8px;
}

.comment-drawer__file-icon {
  width: 20px;
  height: 20px;
  color: var(--cb-color-primary, #409eff);
  flex-shrink: 0;
}

.comment-drawer__file-detail {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.comment-drawer__file-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--cb-text-primary, #303133);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.comment-drawer__file-size {
  font-size: 12px;
  color: var(--cb-text-muted, #909399);
}

.comment-drawer__list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 0;
  min-height: 200px;
  max-height: calc(100vh - 360px);
}

.comment-drawer__loading,
.comment-drawer__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 40px 0;
  color: var(--cb-text-muted, #909399);
  font-size: 13px;
}

.comment-drawer__loading {
  flex-direction: row;
}

.comment-drawer__item {
  padding: 12px 0;
  border-bottom: 1px solid var(--cb-border-light, #ebeef5);
}

.comment-drawer__item:last-child {
  border-bottom: none;
}

.comment-drawer__item-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.comment-drawer__avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--cb-color-primary, #409eff);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}

.comment-drawer__username {
  font-size: 13px;
  font-weight: 600;
  color: var(--cb-text-primary, #303133);
}

.comment-drawer__time {
  font-size: 12px;
  color: var(--cb-text-muted, #909399);
  margin-left: auto;
}

.comment-drawer__delete-btn {
  padding: 2px 4px;
  font-size: 12px;
  opacity: 0;
  transition: opacity 0.15s;
}

.comment-drawer__item:hover .comment-drawer__delete-btn {
  opacity: 1;
}

.comment-drawer__item-content {
  font-size: 14px;
  line-height: 1.6;
  color: var(--cb-text-primary, #303133);
  padding-left: 36px;
  word-break: break-word;
}

.comment-drawer__input-area {
  padding-top: 12px;
  border-top: 1px solid var(--cb-border, #dcdfe6);
}

.comment-drawer__input-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.comment-drawer__hint {
  font-size: 12px;
  color: var(--cb-text-muted, #909399);
}
</style>
