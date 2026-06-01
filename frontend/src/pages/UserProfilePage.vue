<template>
  <MainLayout>
    <template #title>
      <span>个人中心</span>
    </template>

    <section class="profile-page">
      <!-- 加载骨架 -->
      <div v-if="loadingProfile" class="profile-page__skeleton">
        <el-skeleton :rows="8" animated />
      </div>

      <template v-else>
        <!-- 顶部用户信息卡（全宽） -->
        <div class="profile-header">
          <div class="profile-header__avatar">
            <span class="profile-header__avatar-text">{{ avatarInitial }}</span>
          </div>
          <div class="profile-header__info">
            <div class="profile-header__top">
              <h2 class="profile-header__name">{{ profile?.displayName ?? '-' }}</h2>
              <el-tag
                :type="profile?.status === 'active' ? 'success' : 'info'"
                size="small"
                effect="plain"
              >
                {{ profile?.status === 'active' ? '正常' : profile?.status ?? '-' }}
              </el-tag>
            </div>
            <p class="profile-header__email">{{ profile?.email ?? '-' }}</p>
          </div>
          <div class="profile-header__meta">
            <span class="profile-header__meta-item">
              <span class="profile-header__meta-label">用户 ID</span>
              <span class="profile-header__meta-value">{{ profile?.id ?? '-' }}</span>
            </span>
            <span class="profile-header__meta-item">
              <span class="profile-header__meta-label">账户状态</span>
              <span class="profile-header__meta-value profile-header__meta-value--status">
                {{ profile?.status === 'active' ? '正常' : profile?.status ?? '-' }}
              </span>
            </span>
          </div>
        </div>

        <!-- 下方双列：编辑资料 + 修改密码 -->
        <div class="profile-grid">
          <!-- 编辑资料 -->
          <div class="profile-section">
            <h3 class="profile-section__title">编辑资料</h3>
            <p class="profile-section__desc">修改你的显示名称和邮箱地址。</p>

            <el-form
              ref="editFormRef"
              :model="editForm"
              :rules="editRules"
              label-position="top"
              class="profile-section__form"
            >
              <el-form-item label="昵称" prop="displayName">
                <el-input v-model.trim="editForm.displayName" maxlength="100" show-word-limit placeholder="你的显示名称" />
              </el-form-item>

              <el-form-item label="邮箱" prop="email">
                <el-input v-model.trim="editForm.email" maxlength="255" placeholder="用于接收通知和邮件" />
              </el-form-item>

              <el-form-item>
                <el-button type="primary" :loading="savingProfile" @click="handleSaveProfile">
                  保存修改
                </el-button>
                <el-button @click="resetEditForm">重置</el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- 修改密码 -->
          <div class="profile-section">
            <h3 class="profile-section__title">修改密码</h3>
            <p class="profile-section__desc">定期更新密码可以提高账户安全性。</p>

            <el-form
              ref="passwordFormRef"
              :model="passwordForm"
              :rules="passwordRules"
              label-position="top"
              class="profile-section__form"
            >
              <el-form-item label="当前密码" prop="currentPassword">
                <el-input
                  v-model="passwordForm.currentPassword"
                  type="password"
                  placeholder="请输入当前密码"
                  show-password
                  autocomplete="current-password"
                />
              </el-form-item>

              <el-form-item label="新密码" prop="newPassword">
                <el-input
                  v-model="passwordForm.newPassword"
                  type="password"
                  placeholder="至少 6 位"
                  show-password
                  autocomplete="new-password"
                />
              </el-form-item>

              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input
                  v-model="passwordForm.confirmPassword"
                  type="password"
                  placeholder="再次输入新密码"
                  show-password
                  autocomplete="new-password"
                />
              </el-form-item>

              <el-form-item>
                <el-button type="primary" :loading="changingPassword" @click="handleChangePassword">
                  更新密码
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>

        <!-- 邮件设置（SMTP 配置） -->
        <div class="profile-section smtp-section">
          <div class="smtp-section__header">
            <div>
              <h3 class="profile-section__title">邮件设置</h3>
              <p class="profile-section__desc">配置 SMTP 邮件发送服务，支持多套配置切换。</p>
            </div>
            <el-button type="primary" size="small" @click="openSmtpDialog()">添加配置</el-button>
          </div>

          <div v-if="smtpConfigs.length === 0" class="smtp-section__empty">
            <el-empty description="暂无 SMTP 配置，点击上方按钮添加" :image-size="80" />
          </div>

          <div v-else class="smtp-section__list">
            <div v-for="config in smtpConfigs" :key="config.id" class="smtp-card">
              <div class="smtp-card__info">
                <div class="smtp-card__top">
                  <span class="smtp-card__name">{{ config.name }}</span>
                  <el-tag v-if="config.isDefault" type="success" size="small">默认</el-tag>
                </div>
                <div class="smtp-card__detail">{{ config.host }}:{{ config.port }}</div>
                <div class="smtp-card__detail">{{ config.fromAddress }}</div>
              </div>
              <div class="smtp-card__actions">
                <el-button v-if="!config.isDefault" size="small" text type="primary" @click="handleSetDefault(config.id)">设为默认</el-button>
                <el-button size="small" text @click="openSmtpDialog(config)">编辑</el-button>
                <el-button size="small" text @click="openTestDialog(config)">测试</el-button>
                <el-button size="small" text type="danger" @click="handleDeleteSmtp(config.id)">删除</el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- SMTP 配置对话框 -->
        <el-dialog v-model="smtpDialogVisible" :title="editingSmtp ? '编辑 SMTP 配置' : '添加 SMTP 配置'" width="500px">
          <el-form ref="smtpFormRef" :model="smtpForm" :rules="smtpRules" label-position="top">
            <el-form-item label="配置名称" prop="name">
              <el-input v-model.trim="smtpForm.name" placeholder="如：公司邮箱、QQ邮箱" maxlength="100" />
            </el-form-item>
            <el-row :gutter="12">
              <el-col :span="16">
                <el-form-item label="SMTP 服务器" prop="host">
                  <el-input v-model.trim="smtpForm.host" placeholder="如 smtp.qq.com" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="端口" prop="port">
                  <el-input-number v-model="smtpForm.port" :min="1" :max="65535" style="width:100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="登录账号" prop="username">
              <el-input v-model.trim="smtpForm.username" placeholder="SMTP 登录邮箱或账号" />
            </el-form-item>
            <el-form-item label="密码 / 授权码" prop="password">
              <el-input v-model="smtpForm.password" type="password" placeholder="SMTP 密码或授权码" show-password />
            </el-form-item>
            <el-form-item label="发件人地址" prop="fromAddress">
              <el-input v-model.trim="smtpForm.fromAddress" placeholder="邮件发件人地址" />
            </el-form-item>
            <el-divider content-position="left">IMAP 设置（可选，用于写入已发送记录）</el-divider>
            <el-row :gutter="12">
              <el-col :span="16">
                <el-form-item label="IMAP 服务器">
                  <el-input v-model.trim="smtpForm.imapHost" placeholder="如 imap.qq.com（留空则不写入已发送）" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="IMAP 端口">
                  <el-input-number v-model="smtpForm.imapPort" :min="1" :max="65535" style="width:100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="SSL">
                  <el-switch v-model="smtpForm.sslEnabled" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="STARTTLS">
                  <el-switch v-model="smtpForm.starttlsEnabled" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="设为默认配置">
              <el-switch v-model="smtpForm.isDefault" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="smtpDialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="savingSmtp" @click="handleSaveSmtp">保存</el-button>
          </template>
        </el-dialog>

        <!-- 测试邮件对话框 -->
        <el-dialog v-model="testDialogVisible" title="发送测试邮件" width="420px">
          <el-form>
            <el-form-item label="测试收件人">
              <el-input v-model.trim="testRecipient" placeholder="输入收件人邮箱地址" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="testDialogVisible = false">取消</el-button>
            <el-button type="primary" :loading="testingSmtp" @click="handleSendTest">发送测试</el-button>
          </template>
        </el-dialog>
      </template>
    </section>
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';

import MainLayout from '@/layouts/MainLayout.vue';
import { changePassword, getCurrentUserProfile, updateCurrentUserProfile } from '@/services/userApi';
import {
  listSmtpConfigs, createSmtpConfig, updateSmtpConfig, deleteSmtpConfig,
  setDefaultSmtpConfig, testSmtpConfig,
} from '@/services/smtpConfigApi';
import type { SmtpConfig } from '@/services/smtpConfigApi';
import { useAuthStore } from '@/stores/auth';
import type { UserProfileResponse } from '@/services/userApi';

const authStore = useAuthStore();

const editFormRef = ref<FormInstance>();
const passwordFormRef = ref<FormInstance>();

const loadingProfile = ref(false);
const savingProfile = ref(false);
const changingPassword = ref(false);

/** 当前用户资料 */
const profile = ref<UserProfileResponse | null>(null);

/** 编辑资料表单 */
const editForm = reactive({
  displayName: '',
  email: '',
});

/** 修改密码表单 */
const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
});

/**
 * 根据昵称生成头像首字。
 */
const avatarInitial = computed(() => {
  const name = profile.value?.displayName;
  if (!name) return '?';
  return name.charAt(0).toUpperCase();
});

/** 编辑资料表单校验规则 */
const editRules: FormRules = {
  displayName: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { max: 100, message: '昵称不能超过 100 个字符', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { pattern: /^[^\s@]+@[^\s@]+\.(com|cn|net|org|edu|gov|io|cc|vip|info|top|club|xyz|me|com\.cn|net\.cn|org\.cn|co\.cn)$/i, message: '邮箱格式不正确', trigger: 'blur' },
  ],
};

/**
 * 校验确认密码是否与 newPassword 一致。
 */
function validateConfirmPassword(_rule: unknown, value: string, callback: (error?: Error) => void): void {
  if (!value) {
    callback(new Error('请再次输入新密码'));
  } else if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'));
  } else {
    callback();
  }
}

/** 修改密码表单校验规则 */
const passwordRules: FormRules = {
  currentPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' },
  ],
};

/**
 * 从后端加载当前用户资料。
 */
async function loadProfile(): Promise<void> {
  loadingProfile.value = true;

  try {
    profile.value = await getCurrentUserProfile();
    editForm.displayName = profile.value.displayName;
    editForm.email = profile.value.email;
  } catch {
    if (authStore.currentUser) {
      profile.value = {
        id: authStore.currentUser.id,
        displayName: authStore.currentUser.displayName,
        email: authStore.currentUser.email,
        status: authStore.currentUser.status ?? 'active',
      };
      editForm.displayName = profile.value.displayName;
      editForm.email = profile.value.email;
    }
  } finally {
    loadingProfile.value = false;
  }
}

/**
 * 保存编辑后的用户资料到后端，同时更新本地 store。
 */
async function handleSaveProfile(): Promise<void> {
  const valid = await editFormRef.value?.validate().catch(() => false);
  if (!valid) return;

  savingProfile.value = true;

  try {
    const updated = await updateCurrentUserProfile({
      displayName: editForm.displayName,
      email: editForm.email,
    });
    profile.value = updated;
    if (authStore.currentUser && authStore.accessToken) {
      authStore.setSession(
        { id: updated.id, displayName: updated.displayName, email: updated.email, status: updated.status },
        authStore.accessToken,
        authStore.permissions,
      );
    }
    ElMessage.success('资料已更新');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败');
  } finally {
    savingProfile.value = false;
  }
}

/**
 * 重置编辑表单为当前已保存的资料。
 */
function resetEditForm(): void {
  if (profile.value) {
    editForm.displayName = profile.value.displayName;
    editForm.email = profile.value.email;
  }
  editFormRef.value?.clearValidate();
}

/**
 * 修改密码：校验当前密码后更新为新密码。
 */
async function handleChangePassword(): Promise<void> {
  const valid = await passwordFormRef.value?.validate().catch(() => false);
  if (!valid) return;

  changingPassword.value = true;

  try {
    await changePassword(passwordForm.currentPassword, passwordForm.newPassword);
    ElMessage.success('密码已更新');
    passwordForm.currentPassword = '';
    passwordForm.newPassword = '';
    passwordForm.confirmPassword = '';
    passwordFormRef.value?.clearValidate();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '修改密码失败');
  } finally {
    changingPassword.value = false;
  }
}

onMounted(() => {
  void loadProfile();
  void loadSmtpConfigs();
});

/* ============================== SMTP 配置管理 ============================== */

const smtpConfigs = ref<SmtpConfig[]>([]);
const smtpDialogVisible = ref(false);
const testDialogVisible = ref(false);
const editingSmtp = ref<SmtpConfig | null>(null);
const savingSmtp = ref(false);
const testingSmtp = ref(false);
const testRecipient = ref('');
const testingSmtpId = ref<number | null>(null);
const smtpFormRef = ref<FormInstance>();

const smtpForm = reactive({
  name: '',
  host: '',
  port: 465,
  username: '',
  password: '',
  fromAddress: '',
  imapHost: '',
  imapPort: 993,
  sslEnabled: true,
  starttlsEnabled: false,
  isDefault: false,
});

const smtpRules: FormRules = {
  name: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
  host: [{ required: true, message: '请输入 SMTP 服务器地址', trigger: 'blur' }],
  port: [{ required: true, message: '请输入端口', trigger: 'blur' }],
  username: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  fromAddress: [{ required: true, message: '请输入发件人地址', trigger: 'blur' }],
};

async function loadSmtpConfigs(): Promise<void> {
  try {
    smtpConfigs.value = await listSmtpConfigs();
  } catch {
    smtpConfigs.value = [];
  }
}

function openSmtpDialog(config?: SmtpConfig): void {
  if (config) {
    editingSmtp.value = config;
    smtpForm.name = config.name;
    smtpForm.host = config.host;
    smtpForm.port = config.port;
    smtpForm.username = config.username;
    smtpForm.password = '';
    smtpForm.fromAddress = config.fromAddress;
    smtpForm.imapHost = config.imapHost || '';
    smtpForm.imapPort = config.imapPort || 993;
    smtpForm.sslEnabled = config.sslEnabled;
    smtpForm.starttlsEnabled = config.starttlsEnabled;
    smtpForm.isDefault = config.isDefault;
  } else {
    editingSmtp.value = null;
    smtpForm.name = '';
    smtpForm.host = '';
    smtpForm.port = 465;
    smtpForm.username = '';
    smtpForm.password = '';
    smtpForm.fromAddress = '';
    smtpForm.imapHost = '';
    smtpForm.imapPort = 993;
    smtpForm.sslEnabled = true;
    smtpForm.starttlsEnabled = false;
    smtpForm.isDefault = smtpConfigs.value.length === 0;
  }
  smtpDialogVisible.value = true;
}

async function handleSaveSmtp(): Promise<void> {
  const valid = await smtpFormRef.value?.validate().catch(() => false);
  if (!valid) return;

  savingSmtp.value = true;
  try {
    const data = { ...smtpForm };
    if (editingSmtp.value) {
      await updateSmtpConfig(editingSmtp.value.id, data);
      ElMessage.success('SMTP 配置已更新');
    } else {
      await createSmtpConfig(data);
      ElMessage.success('SMTP 配置已添加');
    }
    smtpDialogVisible.value = false;
    await loadSmtpConfigs();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败');
  } finally {
    savingSmtp.value = false;
  }
}

async function handleDeleteSmtp(id: number): Promise<void> {
  try {
    await ElMessageBox.confirm('确认删除此 SMTP 配置？', '确认删除', { type: 'warning' });
    await deleteSmtpConfig(id);
    ElMessage.success('已删除');
    await loadSmtpConfigs();
  } catch {
    // 用户取消
  }
}

async function handleSetDefault(id: number): Promise<void> {
  try {
    await setDefaultSmtpConfig(id);
    ElMessage.success('已设为默认');
    await loadSmtpConfigs();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '设置失败');
  }
}

function openTestDialog(config: SmtpConfig): void {
  testingSmtpId.value = config.id;
  testRecipient.value = '';
  testDialogVisible.value = true;
}

async function handleSendTest(): Promise<void> {
  if (!testRecipient.value.trim()) {
    ElMessage.warning('请输入测试收件人地址');
    return;
  }
  if (testingSmtpId.value === null) return;

  testingSmtp.value = true;
  try {
    const result = await testSmtpConfig(testingSmtpId.value, testRecipient.value.trim());
    if (result.success) {
      ElMessage.success(result.message);
      testDialogVisible.value = false;
    } else {
      ElMessage.error(result.message);
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '测试发送失败');
  } finally {
    testingSmtp.value = false;
  }
}
</script>

<style scoped>
.profile-page {
  display: grid;
  gap: 16px;
}

.profile-page__skeleton {
  padding: 32px;
  background: var(--cb-bg-card);
  border: 1px solid var(--cb-border);
  border-radius: var(--cb-radius-lg);
}

/* ---- 顶部用户信息卡（全宽） ---- */
.profile-header {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px 28px;
  background: var(--cb-bg-card);
  border: 1px solid var(--cb-border);
  border-radius: var(--cb-radius-lg);
}

.profile-header__avatar {
  display: grid;
  place-items: center;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: var(--cb-color-primary);
  flex-shrink: 0;
}

.profile-header__avatar-text {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
  user-select: none;
}

.profile-header__info {
  flex: 1;
  min-width: 0;
}

.profile-header__top {
  display: flex;
  align-items: center;
  gap: 10px;
}

.profile-header__name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--cb-text-primary);
  line-height: 1.3;
}

.profile-header__email {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--cb-text-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-header__meta {
  flex-shrink: 0;
  display: flex;
  gap: 28px;
}

.profile-header__meta-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.profile-header__meta-label {
  font-size: 11px;
  color: var(--cb-text-muted);
}

.profile-header__meta-value {
  font-size: 15px;
  font-weight: 600;
  color: var(--cb-text-primary);
}

.profile-header__meta-value--status {
  color: #16a34a;
}

/* ---- 下方双列网格 ---- */
.profile-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  align-items: stretch;
}

/* ---- 通用区块卡片 ---- */
.profile-section {
  display: flex;
  flex-direction: column;
  background: var(--cb-bg-card);
  border: 1px solid var(--cb-border);
  border-radius: var(--cb-radius-lg);
  padding: 24px;
}

.profile-section__title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--cb-text-primary);
}

.profile-section__desc {
  margin: 4px 0 20px;
  font-size: 13px;
  color: var(--cb-text-muted);
}

.profile-section__form {
  display: flex;
  flex-direction: column;
  flex: 1;
}

/* 让表单最后一个 el-form-item（按钮行）贴底对齐 */
.profile-section__form :deep(.el-form-item:last-child) {
  margin-top: auto;
}

/* ---- 响应式：小屏回落为单列 ---- */
@media (max-width: 768px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }

  .profile-header {
    flex-wrap: wrap;
  }

  .profile-header__meta {
    width: 100%;
    justify-content: flex-start;
    margin-top: 4px;
  }
}

/* ---- SMTP 邮件设置区块 ---- */
.smtp-section__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 4px;
}

.smtp-section__empty {
  padding: 16px 0;
}

.smtp-section__list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.smtp-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  background: var(--cb-bg-base, #fafafa);
  border: 1px solid var(--cb-border);
  border-radius: 8px;
}

.smtp-card__top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.smtp-card__name {
  font-weight: 600;
  font-size: 14px;
  color: var(--cb-text-primary);
}

.smtp-card__detail {
  font-size: 12px;
  color: var(--cb-text-muted);
  line-height: 1.6;
}

.smtp-card__actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}
</style>
