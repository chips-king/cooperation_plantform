<template>
  <Teleport to="body">
    <Transition name="pkg-fade">
      <div v-if="visible" class="pkg-overlay" @click.self="close">
        <div class="pkg-dialog" role="dialog" aria-modal="true">
          <!-- 标题栏 -->
          <div class="pkg-dialog__header">
            <h3 class="pkg-dialog__title">{{ dialogTitle }}</h3>
            <button class="pkg-dialog__close" type="button" aria-label="关闭" @click="close">×</button>
          </div>

          <!-- 内容区 -->
          <div class="pkg-dialog__body">
            <!-- Step 1: 检查中 -->
            <div v-if="step === 'check' && checking" class="pkg-step-center">
              <el-icon class="pkg-spin" :size="36"><Loading /></el-icon>
              <p class="pkg-step-center__text">正在检查项目文件…</p>
            </div>

            <!-- Step 1: 检查完成 -->
            <div v-else-if="step === 'check' && !checking && report" class="pkg-check-result">
              <div class="pkg-check-result__summary">
                <el-icon :size="24" class="pkg-check-result__icon">
                  <CircleCheckFilled v-if="report.canContinuePackaging" />
                  <WarningFilled v-else />
                </el-icon>
                <div class="pkg-check-result__info">
                  <span class="pkg-check-result__label">检查完成</span>
                  <span class="pkg-check-result__detail">
                    风险 {{ report.issues.length }} 项 ·
                    可清理 {{ report.cleanupSuggestions.length }} 项 ·
                    {{ report.canContinuePackaging ? '允许继续打包' : '需确认风险' }}
                  </span>
                </div>
              </div>

              <!-- 风险项列表 -->
              <ul v-if="report.issues.length > 0" class="pkg-check-result__list">
                <li v-for="(issue, idx) in report.issues" :key="idx" class="pkg-check-result__item">
                  <span
                    class="pkg-check-result__badge"
                    :class="issue.blocking ? 'pkg-check-result__badge--block' : 'pkg-check-result__badge--warn'"
                  >
                    {{ issue.blocking ? '阻断' : '风险' }}
                  </span>
                  <span class="pkg-check-result__item-text">{{ formatIssue(issue) }}</span>
                </li>
              </ul>

              <!-- 清理建议 -->
              <ul v-if="report.cleanupSuggestions.length > 0" class="pkg-check-result__list">
                <li v-for="(sug, idx) in report.cleanupSuggestions" :key="idx" class="pkg-check-result__item">
                  <span class="pkg-check-result__badge pkg-check-result__badge--info">建议</span>
                  <span class="pkg-check-result__item-text">{{ formatCleanup(sug) }}</span>
                </li>
              </ul>
            </div>

            <!-- Step 1: 检查失败 -->
            <div v-else-if="step === 'check' && !checking && checkError" class="pkg-step-center">
              <el-icon :size="36" color="#f56c6c"><WarningFilled /></el-icon>
              <p class="pkg-step-center__text pkg-step-center__text--error">{{ checkError }}</p>
              <el-button type="primary" plain @click="handleRunCheck">重试检查</el-button>
            </div>

            <!-- Step 2: 打包配置 -->
            <div v-else-if="step === 'pack'" class="pkg-pack-form">
              <div class="pkg-pack-form__row">
                <label class="pkg-pack-form__label">文件名</label>
                <input
                  ref="packInputRef"
                  v-model="packBaseName"
                  class="pkg-pack-form__input"
                  maxlength="80"
                  placeholder="例如：课程项目成果"
                  @keydown.enter="handleCreatePackage"
                />
              </div>
              <div class="pkg-pack-form__row">
                <label class="pkg-pack-form__label">格式</label>
                <div class="pkg-pack-form__formats">
                  <button
                    v-for="fmt in formats"
                    :key="fmt"
                    class="pkg-pack-form__format-btn"
                    :class="{ 'pkg-pack-form__format-btn--active': packFormat === fmt }"
                    type="button"
                    @click="packFormat = fmt"
                  >
                    .{{ fmt }}
                  </button>
                </div>
              </div>
            </div>

            <!-- Step 3: 打包中 -->
            <div v-else-if="step === 'done' && exporting" class="pkg-step-center">
              <el-icon class="pkg-spin" :size="36"><Loading /></el-icon>
              <p class="pkg-step-center__text">正在打包…</p>
            </div>

            <!-- Step 3: 打包完成 -->
            <div v-else-if="step === 'done' && !exporting && artifact" class="pkg-step-center">
              <el-icon :size="36" color="#67c23a"><CircleCheckFilled /></el-icon>
              <p class="pkg-step-center__text pkg-step-center__text--success">压缩包已生成</p>
              <p class="pkg-step-center__sub">{{ artifact.filename }}</p>
            </div>
          </div>

          <!-- 底部操作区 -->
          <div class="pkg-dialog__footer">
            <!-- 检查结果 -->
            <template v-if="step === 'check' && report">
              <el-button @click="handleRunCheck">重新检查</el-button>
              <el-button type="primary" @click="goToPack">继续打包</el-button>
            </template>

            <!-- 打包配置 -->
            <template v-else-if="step === 'pack'">
              <el-button @click="step = 'check'">返回</el-button>
              <el-button
                type="primary"
                :disabled="!packBaseName.trim()"
                @click="handleCreatePackage"
              >
                确认打包
              </el-button>
            </template>

            <!-- 打包完成 -->
            <template v-else-if="step === 'done' && artifact">
              <el-button @click="close">关闭</el-button>
              <el-button type="primary" @click="goToMail">前往邮件草稿</el-button>
            </template>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import {
  CircleCheckFilled,
  Loading,
  WarningFilled,
} from '@element-plus/icons-vue';

import { useAuthStore } from '@/stores/auth';
import { createPackage, runPackageCheck } from '@/services/packageApi';
import { HttpRequestError } from '@/services/http';

import type { CheckIssue, CheckReport, CleanupSuggestion, PackageArtifact, PackageFormat } from '@/types/project';

interface Props {
  /** 控制面板是否可见 */
  visible: boolean;
  /** 当前项目 ID */
  projectId: string;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  'update:visible': [value: boolean];
  'package-complete': [];
}>();

/** 当前步骤：check（检查） → pack（配置打包） → done（完成） */
type Step = 'check' | 'pack' | 'done';

const authStore = useAuthStore();

const step = ref<Step>('check');
const checking = ref(false);
const exporting = ref(false);
const report = ref<CheckReport | null>(null);
const artifact = ref<PackageArtifact | null>(null);
const checkError = ref('');

const packBaseName = ref('');
const packFormat = ref<PackageFormat>('zip');
const formats: PackageFormat[] = ['zip', '7z', 'tar.gz'];
const packInputRef = ref<HTMLInputElement | null>(null);

/** 根据当前步骤动态生成对话框标题 */
const dialogTitle = computed(() => {
  switch (step.value) {
    case 'check': return '打包检查';
    case 'pack': return '打包配置';
    case 'done': return '打包完成';
  }
});

/**
 * 重置所有内部状态到初始值。
 */
function resetState(): void {
  step.value = 'check';
  checking.value = false;
  exporting.value = false;
  report.value = null;
  artifact.value = null;
  checkError.value = '';
  packBaseName.value = '';
  packFormat.value = 'zip';
}

watch(
  () => props.visible,
  (val) => {
    if (val) {
      // 打开面板时自动执行打包检查
      void handleRunCheck();
    } else {
      resetState();
    }
  },
);

/** 关闭面板 */
function close(): void {
  emit('update:visible', false);
}

/**
 * 将检查问题对象格式化为可读中文描述。
 *
 * @param issue 检查问题对象
 * @returns 可读的中文描述
 */
function formatIssue(issue: CheckIssue): string {
  const typeMap: Record<string, string> = {
    MISSING_README: '缺少 README 文件',
    EMPTY_DIRECTORY: `空目录：${issue.path}`,
    ARCHIVE_FILE: `包含压缩包：${issue.path}`,
    CACHE_DIRECTORY: `包含缓存目录：${issue.path}`,
    TEMPORARY_FILE: `包含临时文件：${issue.path}`,
    LOG_FILE: `包含日志文件：${issue.path}`,
    SYSTEM_FILE: `包含系统文件：${issue.path}`,
    LARGE_FILE: `包含大文件：${issue.path}`,
  };
  return typeMap[issue.type] ?? `${issue.type}：${issue.path}`;
}

/**
 * 将清理建议对象格式化为可读中文描述。
 *
 * @param sug 清理建议对象
 * @returns 可读的中文描述
 */
function formatCleanup(sug: CleanupSuggestion): string {
  const typeMap: Record<string, string> = {
    EMPTY_DIRECTORY: `可删除空目录：${sug.path}`,
    ARCHIVE_FILE: `可清理压缩包：${sug.path}`,
    CACHE_DIRECTORY: `可清理缓存目录：${sug.path}`,
    TEMPORARY_FILE: `可清理临时文件：${sug.path}`,
    LOG_FILE: `可清理日志文件：${sug.path}`,
    SYSTEM_FILE: `可清理系统文件：${sug.path}`,
    LARGE_FILE: `可清理大文件：${sug.path}`,
  };
  return typeMap[sug.type] ?? `可清理：${sug.path}`;
}

/**
 * 执行打包前检查 API 并同步检查结果。
 */
async function handleRunCheck(): Promise<void> {
  if (!props.projectId) {
    checkError.value = '请先选择项目';
    return;
  }

  checking.value = true;
  checkError.value = '';

  try {
    report.value = await runPackageCheck(props.projectId, {
      userId: authStore.currentUser?.id,
    });
  } catch (error) {
    report.value = null;
    checkError.value = error instanceof Error ? error.message : '检查失败';
  } finally {
    checking.value = false;
  }
}

/**
 * 跳转到打包配置步骤，聚焦文件名输入框。
 */
function goToPack(): void {
  step.value = 'pack';
  nextTick(() => {
    packInputRef.value?.focus();
  });
}

/**
 * 调用后端打包 API 并在成功后切换到完成步骤。
 */
async function handleCreatePackage(): Promise<void> {
  if (!props.projectId || !packBaseName.value.trim()) {
    return;
  }

  exporting.value = true;

  try {
    artifact.value = await createPackage({
      projectId: props.projectId,
      userId: authStore.currentUser?.id,
      baseName: packBaseName.value.trim(),
      format: packFormat.value,
      continueAfterCheck: true,
    });
    step.value = 'done';
    exporting.value = false;
    ElMessage.success('压缩包已生成');
  } catch (error) {
    exporting.value = false;
    if (error instanceof HttpRequestError) {
      const statusHint = error.status ? `[${error.status}] ` : '';
      ElMessage.error(`${statusHint}${error.message}`);
      return;
    }
    ElMessage.error(error instanceof Error ? error.message : '打包失败');
  }
}

/** 关闭面板并跳转到邮件草稿页面 */
function goToMail(): void {
  close();
  emit('package-complete');
}
</script>

<style scoped>
/* ===== 遮罩与过渡动画 ===== */

.pkg-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 15vh;
  background: rgba(0, 0, 0, 0.45);
}

.pkg-fade-enter-active,
.pkg-fade-leave-active {
  transition: opacity 0.18s ease;
}

.pkg-fade-enter-from,
.pkg-fade-leave-to {
  opacity: 0;
}

/* ===== 对话框容器 ===== */

.pkg-dialog {
  width: 480px;
  max-width: calc(100vw - 32px);
  background: #fff;
  border-radius: 12px;
  box-shadow:
    0 16px 70px rgba(0, 0, 0, 0.15),
    0 0 0 1px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  animation: pkgSlideUp 0.2s ease;
}

@keyframes pkgSlideUp {
  from { opacity: 0; transform: translateY(12px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ===== 标题栏 ===== */

.pkg-dialog__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px 12px;
  border-bottom: 1px solid #ebeef5;
}

.pkg-dialog__title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.pkg-dialog__close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: transparent;
  font-size: 20px;
  color: #909399;
  cursor: pointer;
  transition: all 0.15s ease;
}

.pkg-dialog__close:hover {
  background: #f5f7fa;
  color: #303133;
}

/* ===== 内容区 ===== */

.pkg-dialog__body {
  min-height: 120px;
  max-height: 400px;
  overflow-y: auto;
  padding: 20px;
}

/* ===== 居中步骤状态 ===== */

.pkg-step-center {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 24px 0;
}

.pkg-step-center__text {
  margin: 0;
  font-size: 15px;
  color: #303133;
}

.pkg-step-center__text--error {
  color: #f56c6c;
}

.pkg-step-center__text--success {
  color: #67c23a;
  font-weight: 600;
}

.pkg-step-center__sub {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

/* ===== 加载旋转 ===== */

.pkg-spin {
  animation: pkgSpin 1s linear infinite;
  color: #409eff;
}

@keyframes pkgSpin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ===== 检查结果 ===== */

.pkg-check-result {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pkg-check-result__summary {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.pkg-check-result__icon {
  flex-shrink: 0;
  margin-top: 2px;
  color: #67c23a;
}

.pkg-check-result__info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.pkg-check-result__label {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.pkg-check-result__detail {
  font-size: 13px;
  color: #606266;
}

.pkg-check-result__list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.pkg-check-result__item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  font-size: 13px;
  color: #606266;
  background: #fafafa;
  border-radius: 6px;
}

.pkg-check-result__badge {
  display: inline-block;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
}

.pkg-check-result__badge--warn {
  background: #fdf6ec;
  color: #e6a23c;
}

.pkg-check-result__badge--block {
  background: #fef0f0;
  color: #f56c6c;
}

.pkg-check-result__badge--info {
  background: #ecf5ff;
  color: #409eff;
}

.pkg-check-result__item-text {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===== 打包表单 ===== */

.pkg-pack-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pkg-pack-form__row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pkg-pack-form__label {
  flex-shrink: 0;
  width: 52px;
  font-size: 14px;
  color: #909399;
}

.pkg-pack-form__input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  font-size: 14px;
  color: #303133;
  outline: none;
  transition: border-color 0.15s ease;
}

.pkg-pack-form__input:focus {
  border-color: #409eff;
}

.pkg-pack-form__input::placeholder {
  color: #c0c4cc;
}

.pkg-pack-form__formats {
  display: flex;
  gap: 8px;
}

.pkg-pack-form__format-btn {
  padding: 6px 16px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  background: #fff;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  transition: all 0.15s ease;
}

.pkg-pack-form__format-btn:hover {
  border-color: #409eff;
  color: #409eff;
}

.pkg-pack-form__format-btn--active {
  border-color: #409eff;
  background: #ecf5ff;
  color: #409eff;
}

/* ===== 底部操作区 ===== */

.pkg-dialog__footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 20px;
  border-top: 1px solid #ebeef5;
}
</style>
