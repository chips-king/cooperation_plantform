<template>
  <MainLayout>
    <template #title>
      <span>邮件草稿</span>
    </template>

    <template #actions>
      <template v-if="isProjectView">
        <el-button :icon="Refresh" :loading="loadingLatest" @click="loadLatestPackage">
          刷新附件
        </el-button>
      </template>
    </template>

    <template #aside>
      <nav class="mail-aside">
        <div class="mail-aside__label">可发邮件的项目</div>
        <div v-if="summaries.length === 0" class="mail-aside__empty">暂无项目</div>
        <a
          v-for="item in summaries"
          :key="item.projectId"
          class="mail-aside__link"
          href="#"
          @click.prevent="openProjectDrafts(item.projectId, item.projectName)"
        >
          <span class="mail-aside__link-name">{{ item.projectName }}</span>
          <el-badge v-if="item.draftCount > 0" :value="item.draftCount" :max="99" type="primary" />
          <el-tag v-else type="warning" size="small" effect="plain">待生成</el-tag>
        </a>
      </nav>
    </template>

    <!-- 第一层：项目列表概览（含已打包未建草稿的项目） -->
    <section v-if="!isProjectView" class="mail-overview">
      <el-empty v-if="!loadingSummaries && summaries.length === 0">
        <template #description>
          <p>暂无已打包项目或邮件草稿</p>
          <p class="mail-overview__empty-hint">请先在项目工作台完成打包，再在此生成邮件草稿</p>
        </template>
      </el-empty>

      <div v-else class="mail-overview__grid">
        <div
          v-for="item in summaries"
          :key="item.projectId"
          class="mail-overview__card"
          @click="openProjectDrafts(item.projectId, item.projectName)"
        >
          <div class="mail-overview__card-top">
            <span class="mail-overview__card-name">{{ item.projectName }}</span>
            <el-badge v-if="item.draftCount > 0" :value="item.draftCount" :max="99" type="primary" />
            <el-tag v-else type="warning" size="small">待生成草稿</el-tag>
          </div>
          <div class="mail-overview__card-meta">
            <template v-if="item.draftCount > 0">
              <el-icon :size="14" color="#909399"><Document /></el-icon>
              <span>{{ item.draftCount }} 个草稿</span>
            </template>
            <template v-else-if="item.latestPackageFilename">
              <el-icon :size="14" color="#909399"><Box /></el-icon>
              <span class="mail-overview__package-name">{{ item.latestPackageFilename }}</span>
            </template>
          </div>
        </div>
      </div>
    </section>

    <!-- 第二层：项目草稿列表 + 编辑器 -->
    <section v-else class="mail-layout">
      <!-- 顶部返回 + 项目草稿列表 -->
      <div class="mail-project__nav">
        <el-button text @click="goToOverview">
          ← 所有项目
        </el-button>
        <span class="mail-project__name">{{ projectName }}</span>
      </div>

      <el-alert
        v-if="attachmentText"
        :closable="false"
        show-icon
        title="当前项目已有打包文件，填写收件人和主题后即可发送邮件。"
        type="success"
      />

      <!-- 项目状态摘要信息条 -->
      <div class="mail-project__status-bar">
        <span class="mail-project__status-item">
          <span class="mail-project__status-label">当前项目</span>
          <span class="mail-project__status-value">{{ projectName }}</span>
        </span>
        <el-divider direction="vertical" />
        <span class="mail-project__status-item">
          <span class="mail-project__status-label">附件</span>
          <span class="mail-project__status-value">{{ latestPackage?.filename ?? draft?.attachmentFilename ?? '暂无' }}</span>
        </span>
        <el-divider direction="vertical" />
        <span class="mail-project__status-item">
          <span class="mail-project__status-label">草稿状态</span>
          <el-tag :type="draftStatusText === '已发送' ? 'success' : draftStatusText === '草稿' ? 'warning' : 'info'" size="small">
            {{ draftStatusText }}
          </el-tag>
        </span>
      </div>

      <!-- 左右分栏布局 -->
      <div class="mail-project__content">
        <!-- 左侧：草稿列表 -->
        <aside class="mail-project__sidebar">
          <div v-if="projectDrafts.length > 0" class="mail-project__drafts">
            <div
              v-for="item in projectDrafts"
              :key="item.draftId"
              class="mail-project__draft-item"
              :class="{ 'mail-project__draft-item--active': draft?.draftId === item.draftId }"
              @click="loadDraftDetail(item.draftId)"
            >
              <div class="mail-project__draft-info">
                <span class="mail-project__draft-subject">{{ item.subject || '（无主题）' }}</span>
                <el-tag :type="item.status === 'sent' ? 'success' : 'info'" size="small">
                  {{ item.status === 'sent' ? '已发送' : '草稿' }}
                </el-tag>
              </div>
              <div class="mail-project__draft-meta">
                <span class="mail-project__draft-time">{{ formatDate(item.createdAt) }}</span>
                <el-button
                  :icon="Delete"
                  :loading="deletingDraftId === item.draftId"
                  circle
                  size="small"
                  text
                  type="danger"
                  title="删除草稿"
                  @click.stop="handleDeleteDraft(item)"
                />
              </div>
            </div>
          </div>
          <div v-else class="mail-project__empty-hint">
            <p>该项目暂无草稿</p>
            <p class="hint-text">首次填写表单并点击「暂时保存」即可创建草稿</p>
          </div>
        </aside>

        <!-- 右侧：表单编辑器 -->
        <main class="mail-project__editor">
          <el-card class="panel" shadow="never">
            <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
              <el-form-item label="收件人" prop="recipientsText">
                <el-input
                  v-model="form.recipientsText"
                  type="textarea"
                  :rows="3"
                  placeholder="多个收件人可用逗号或换行分隔"
                />
              </el-form-item>

              <el-form-item label="主题" prop="subject">
                <el-input v-model.trim="form.subject" maxlength="120" show-word-limit />
              </el-form-item>

              <el-form-item label="正文" prop="body">
                <el-input v-model="form.body" type="textarea" :rows="8" />
              </el-form-item>

              <el-form-item label="当前附件">
                <div v-if="latestPackage" class="package-info">
                  <el-tag type="success">{{ latestPackage.filename }}</el-tag>
                  <span class="package-hint">（已自动绑定最新压缩包）</span>
                  <span v-if="latestPackage.format === 'zip'" class="package-hint">推荐使用 .zip 作为邮件附件格式</span>
                </div>
                <el-tag v-else type="warning">请先生成最终压缩包</el-tag>
              </el-form-item>

              <el-form-item v-if="smtpConfigs.length > 0" label="发送邮箱">
                <el-select v-model="selectedSmtpConfigId" placeholder="选择 SMTP 配置" style="width: 100%">
                  <el-option
                    v-for="config in smtpConfigs"
                    :key="config.id"
                    :label="`${config.name} (${config.fromAddress})`"
                    :value="config.id"
                  />
                </el-select>
              </el-form-item>

              <el-form-item>
                <el-button :icon="DocumentAdd" :loading="saving" type="primary" @click="handleSaveDraft">
                  {{ draft ? '保存修改' : '生成草稿' }}
                </el-button>
                <el-button :disabled="!draft || draft.status === 'sent'" :loading="sending" type="danger" @click="handleSendDraft">
                  确认发送
                </el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </main>
      </div>
    </section>
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { Box, Delete, Document, DocumentAdd, Refresh } from '@element-plus/icons-vue';
import { useRoute, useRouter } from 'vue-router';

import MainLayout from '@/layouts/MainLayout.vue';
import {
  createMailDraft,
  deleteMailDraft,
  getMailDraft,
  listProjectDrafts,
  listUserDraftSummaries,
  sendMailDraft,
  updateMailDraft,
} from '@/services/mailApi';
import { getLatestPackage } from '@/services/packageApi';
import { listSmtpConfigs } from '@/services/smtpConfigApi';
import { getProject } from '@/services/groupProjectApi';
import { useAuthStore } from '@/stores/auth';
import { useProjectStore } from '@/stores/project';

import type { DraftSummary, MailDraft, PackageArtifact, PackageFormat, Project, ProjectDraftListItem } from '@/types/project';
import type { SmtpConfig } from '@/services/smtpConfigApi';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const projectStore = useProjectStore();

const formRef = ref<FormInstance>();
const saving = ref(false);
const sending = ref(false);
const loadingLatest = ref(false);
const loadingSummaries = ref(false);
const loadingProjectDrafts = ref(false);
const deletingDraftId = ref<string | null>(null);
const smtpConfigs = ref<SmtpConfig[]>([]);
const selectedSmtpConfigId = ref<number | null>(null);
const draft = ref<MailDraft | null>(null);
const latestPackage = ref<PackageArtifact | null>(null);
const summaries = ref<DraftSummary[]>([]);
const projectDrafts = ref<ProjectDraftListItem[]>([]);

const form = reactive({
  recipientsText: '',
  subject: '',
  body: '',
  attachmentFormat: 'zip' as PackageFormat,
});

/** 当前是否处于项目维度视图（第二层）。 */
const isProjectView = computed(() => route.name === 'mail-draft');

const projectId = computed(() => {
  const routeProjectId = route.params.projectId;
  return String(projectStore.currentProject?.id ?? (Array.isArray(routeProjectId) ? routeProjectId[0] : routeProjectId) ?? '');
});
const projectName = computed(() => projectStore.currentProject?.name ?? (projectId.value ? `项目 ${projectId.value}` : '未选择项目'));
const attachmentText = computed(() => latestPackage.value?.filename ?? draft.value?.attachmentFilename ?? '');
const draftStatusText = computed(() => {
  if (!draft.value) {
    return '未生成';
  }

  return draft.value.status === 'sent' ? '已发送' : '草稿';
});

const rules: FormRules = {
  recipientsText: [{ required: true, message: '请输入收件人', trigger: 'blur' }],
  subject: [{ required: true, message: '请输入邮件主题', trigger: 'blur' }],
  body: [{ required: true, message: '请输入邮件正文', trigger: 'blur' }],
};

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * 格式化日期为简短可读形式。
 */
function formatDate(value?: string | null): string {
  if (!value) return '-';
  const d = new Date(value);
  return d.toLocaleDateString('zh-CN');
}

/**
 * 解析用户输入的收件人列表。
 */
function parseRecipients(): string[] {
  const recipients = form.recipientsText
    .split(/[\n,，;；]+/)
    .map((item) => item.trim())
    .filter(Boolean);

  return Array.from(new Set(recipients));
}

/**
 * 校验表单并返回收件人列表。
 */
async function validateForm(): Promise<string[] | null> {
  const valid = await formRef.value?.validate().catch(() => false);

  if (!valid) {
    return null;
  }

  const recipients = parseRecipients();

  if (recipients.length === 0) {
    ElMessage.warning('请填写至少一个收件人');
    return null;
  }

  if (recipients.some((recipient) => !emailPattern.test(recipient))) {
    ElMessage.warning('邮箱信息不正确');
    return null;
  }

  return recipients;
}

/**
 * 加载用户草稿概览（第一层）。
 */
async function loadSummaries(): Promise<void> {
  loadingSummaries.value = true;

  try {
    summaries.value = await listUserDraftSummaries({ userId: authStore.currentUser?.id });
  } catch {
    summaries.value = [];
  } finally {
    loadingSummaries.value = false;
  }
}

/**
 * 从概览进入某个项目的草稿列表（第二层）。
 */
function openProjectDrafts(pid: string, name?: string): void {
  projectStore.setCurrentProject({
    id: Number(pid),
    name: name ?? '',
    groupId: 0,
    ownerId: 0,
    status: 'active',
    endedAt: null,
    reopenedAt: null,
  });
  void router.push(`/projects/${pid}/mail`);
}

/**
 * 从项目草稿视图返回概览视图（第一层）。
 */
function goToOverview(): void {
  void router.push('/mail-drafts');
}

/**
 * 加载项目草稿列表（第二层侧栏）。
 */
async function loadProjectDrafts(): Promise<void> {
  if (!projectId.value) return;

  loadingProjectDrafts.value = true;

  try {
    projectDrafts.value = await listProjectDrafts(projectId.value, { userId: authStore.currentUser?.id });
  } catch {
    projectDrafts.value = [];
  } finally {
    loadingProjectDrafts.value = false;
  }
}

/**
 * 点击草稿列表项时加载草稿详情到编辑器。
 */
async function loadDraftDetail(draftId: string): Promise<void> {
  try {
    draft.value = await getMailDraft(draftId, { userId: authStore.currentUser?.id });
    form.recipientsText = draft.value.recipients.join(', ');
    form.subject = draft.value.subject;
    form.body = draft.value.body;
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载草稿失败');
  }
}

/**
 * 查询最近压缩包作为草稿附件参考。
 */
async function loadLatestPackage(): Promise<void> {
  if (!projectId.value) {
    return;
  }

  loadingLatest.value = true;

  try {
    latestPackage.value = await getLatestPackage(projectId.value, { userId: authStore.currentUser?.id });
  } catch (error) {
    latestPackage.value = null;
  } finally {
    loadingLatest.value = false;
  }
}

/**
 * 保存草稿（创建或更新）。
 */
async function handleSaveDraft(): Promise<void> {
  if (!projectId.value) {
    ElMessage.warning('请先选择项目');
    return;
  }

  const recipients = await validateForm();

  if (!recipients) {
    return;
  }

  // 检查是否有附件
  if (!latestPackage.value && !draft.value?.packageId) {
    ElMessage.error('当前项目没有可用的压缩包，请先在项目工作台完成打包');
    return;
  }

  saving.value = true;

  try {
    if (draft.value) {
      // 更新已有草稿
      draft.value = await updateMailDraft({
        draftId: draft.value.draftId,
        userId: authStore.currentUser?.id,
        recipients,
        subject: form.subject,
        body: form.body,
        packageId: latestPackage.value?.packageId ?? draft.value.packageId,
      });
      ElMessage.success('草稿已保存');
    } else {
      // 创建新草稿（后端会自动绑定最新压缩包）
      draft.value = await createMailDraft({
        projectId: projectId.value,
        userId: authStore.currentUser?.id,
        recipients,
        subject: form.subject,
        body: form.body,
      });
      ElMessage.success('草稿已创建，已自动绑定最新压缩包');
    }
    await loadProjectDrafts();
  } catch (error) {
    const errorMsg = error instanceof Error ? error.message : '保存草稿失败';
    ElMessage.error(errorMsg);
    // 如果是"项目没有可用的最近压缩包"错误，给出更明确的提示
    if (errorMsg.includes('没有可用的最近压缩包')) {
      ElMessage.info('请前往项目工作台检查打包状态');
    }
  } finally {
    saving.value = false;
  }
}

/**
 * 发送草稿前要求用户主动确认。
 */
async function handleSendDraft(): Promise<void> {
  if (!draft.value) {
    return;
  }

  try {
    await ElMessageBox.confirm('请确认收件人、正文和附件均已核实。发送后会写入操作记录，是否继续？', '发送确认', {
      confirmButtonText: '确认发送',
      cancelButtonText: '取消',
      type: 'warning',
    });
  } catch {
    return;
  }

  sending.value = true;

  try {
    const result = await sendMailDraft({
      draftId: draft.value.draftId,
      userId: authStore.currentUser?.id,
      confirmed: true,
      smtpConfigId: selectedSmtpConfigId.value ?? undefined,
    });
    draft.value = result;
    ElMessage.success(result.message || '邮件已发送');
    await loadProjectDrafts();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '邮件发送失败');
  } finally {
    sending.value = false;
  }
}

/**
 * 删除草稿前要求用户确认；删除成功后刷新列表并清空当前编辑器。
 */
async function handleDeleteDraft(item: ProjectDraftListItem): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要删除草稿「${item.subject || '（无主题）'}」吗？删除后不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    );
  } catch {
    return;
  }

  deletingDraftId.value = item.draftId;

  try {
    await deleteMailDraft(item.draftId, { userId: authStore.currentUser?.id });
    ElMessage.success('草稿已删除');
    // 若删除的是当前正在编辑的草稿，清空编辑器
    if (draft.value?.draftId === item.draftId) {
      draft.value = null;
      form.recipientsText = '';
      form.subject = '';
      form.body = '';
    }
    await loadProjectDrafts();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除草稿失败');
  } finally {
    deletingDraftId.value = null;
  }
}

/**
 * 加载当前项目基本信息，用于侧边栏和标题展示。
 */
async function loadProjectDetail(pid: string): Promise<void> {
  try {
    const project = await getProject(Number(pid), { userId: authStore.currentUser?.id });
    projectStore.setCurrentProject(project);
  } catch {
    // 加载失败时保持现有状态或回退到空
  }
}

/**
 * 根据路由切换加载对应层级的数据。
 */
function handleRouteChange(): void {
  draft.value = null;
  form.recipientsText = '';
  form.subject = '';
  form.body = '';

  if (route.name === 'mail-draft-overview') {
    void loadSummaries();
  } else if (route.name === 'mail-draft' && projectId.value) {
    if (!projectStore.currentProject || String(projectStore.currentProject.id) !== projectId.value) {
      void loadProjectDetail(projectId.value);
    }
    void loadProjectDrafts();
    void loadLatestPackage();
  }
}

watch(() => route.name, handleRouteChange);
watch(() => route.params.projectId, handleRouteChange);

onMounted(() => {
  handleRouteChange();
  void loadSmtpConfigs();
});

async function loadSmtpConfigs(): Promise<void> {
  try {
    smtpConfigs.value = await listSmtpConfigs();
    const defaultConfig = smtpConfigs.value.find(c => c.isDefault);
    if (defaultConfig) {
      selectedSmtpConfigId.value = defaultConfig.id;
    } else if (smtpConfigs.value.length > 0) {
      selectedSmtpConfigId.value = smtpConfigs.value[0].id;
    }
  } catch {
    smtpConfigs.value = [];
  }
}
</script>

<style scoped>
/* ---- 侧边栏 ---- */
.mail-aside__label {
  padding: 0 0 8px;
  color: #6b6b8a;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.8px;
}

.mail-aside__empty {
  padding: 12px 0;
  color: #9e9eb8;
  font-size: 13px;
}

.mail-aside__link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 10px;
  margin-bottom: 2px;
  border-radius: 6px;
  color: #9e9eb8;
  font-size: 13px;
  text-decoration: none;
  transition: background 0.1s, color 0.1s;
}

.mail-aside__link:hover {
  color: #e8e8f0;
  background: rgba(255, 255, 255, 0.06);
}

.mail-aside__link-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ---- 概览视图（第一层） ---- */
.mail-overview {
  display: grid;
  gap: 16px;
}

.mail-overview__empty-hint {
  margin-top: 4px;
  font-size: 13px;
  color: var(--cb-text-muted);
}

.mail-overview__package-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mail-overview__grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.mail-overview__card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 18px;
  background: var(--cb-bg-card);
  border: 1px solid var(--cb-border);
  border-radius: var(--cb-radius-lg);
  cursor: pointer;
  transition: all 0.15s ease;
  min-height: 120px;
}

.mail-overview__card:hover {
  border-color: var(--cb-color-primary);
  box-shadow: var(--cb-shadow-hover);
  transform: translateY(-2px);
}

.mail-overview__card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.mail-overview__card-name {
  font-weight: 600;
  font-size: 16px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mail-overview__card-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--cb-text-muted);
}

/* ---- 项目视图（第二层） ---- */
.mail-layout {
  display: grid;
  gap: 18px;
  max-width: 1200px;
}

/* 项目状态摘要信息条 */
.mail-project__status-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 16px;
  background: var(--cb-bg-card);
  border: 1px solid var(--cb-border);
  border-radius: var(--cb-radius-md);
  font-size: 13px;
  color: var(--cb-text-secondary);
}

.mail-project__status-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.mail-project__status-label {
  color: var(--cb-text-muted);
}

.mail-project__status-value {
  color: var(--cb-text-primary);
  font-weight: 500;
}

.mail-project__nav {
  display: flex;
  align-items: center;
  gap: 12px;
}

.mail-project__name {
  font-weight: 600;
  font-size: 16px;
}

/* 左右分栏布局 */
.mail-project__content {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 16px;
  align-items: start;
}

.mail-project__sidebar {
  position: sticky;
  top: 0;
  max-height: calc(100vh - 200px);
  overflow-y: auto;
}

.mail-project__empty-hint {
  padding: 40px 20px;
  text-align: center;
  color: var(--cb-text-muted);
}

.mail-project__empty-hint .hint-text {
  margin-top: 8px;
  font-size: 12px;
}

.mail-project__drafts {
  display: grid;
  gap: 6px;
  padding: 12px;
  background: var(--cb-bg-card);
  border: 1px solid var(--cb-border);
  border-radius: var(--cb-radius-lg);
  max-height: none;
}

.mail-project__draft-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.12s ease;
}

.mail-project__draft-item:hover {
  background: var(--cb-bg-page);
}

.mail-project__draft-item--active {
  background: var(--cb-color-primary-light);
}

.mail-project__draft-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mail-project__draft-subject {
  font-size: 14px;
  color: var(--cb-text-primary);
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mail-project__draft-time {
  font-size: 12px;
  color: var(--cb-text-muted);
  flex-shrink: 0;
}

.mail-project__draft-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.mail-project__editor {
  min-width: 0;
}

.package-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.package-hint {
  font-size: 12px;
  color: var(--cb-text-muted);
}

.panel {
  border-radius: var(--cb-radius-md);
}

/* 响应式适配 */
@media (max-width: 900px) {
  .mail-project__content {
    grid-template-columns: 1fr;
  }

  .mail-project__sidebar {
    position: static;
    max-height: 200px;
  }
}
</style>
