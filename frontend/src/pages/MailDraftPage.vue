<template>
  <MainLayout>
    <template #title>
      <span>邮件草稿</span>
    </template>

    <template #actions>
      <el-button :icon="Refresh" :loading="loadingLatest" @click="loadLatestPackage">
        刷新附件
      </el-button>
    </template>

    <template #aside>
      <el-descriptions :column="1" size="small" border>
        <el-descriptions-item label="当前项目">
          {{ projectName }}
        </el-descriptions-item>
        <el-descriptions-item label="附件">
          {{ latestPackage?.filename ?? draft?.attachmentFilename ?? '暂无' }}
        </el-descriptions-item>
        <el-descriptions-item label="草稿状态">
          {{ draftStatusText }}
        </el-descriptions-item>
      </el-descriptions>
    </template>

    <section class="mail-layout">
      <el-alert
        :closable="false"
        show-icon
        title="推荐使用 .zip 作为邮件附件格式，兼容性最好。.7z 和 .tar.gz 也可作为附件格式。发送前必须再次确认。"
        type="info"
      />

      <el-card class="panel" shadow="never">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
          <el-form-item label="收件人" prop="recipientsText">
            <el-input
              v-model="form.recipientsText"
              placeholder="多个收件人可用逗号或换行分隔"
              type="textarea"
              :rows="3"
            />
          </el-form-item>

          <el-form-item label="主题" prop="subject">
            <el-input v-model.trim="form.subject" maxlength="120" show-word-limit />
          </el-form-item>

          <el-form-item label="正文" prop="body">
            <el-input v-model="form.body" type="textarea" :rows="10" />
          </el-form-item>

          <el-form-item label="附件格式">
            <el-radio-group v-model="form.attachmentFormat">
              <el-radio-button label=".zip" value="zip">.zip</el-radio-button>
              <el-radio-button label=".7z" value="7z">.7z</el-radio-button>
              <el-radio-button label=".tar.gz" value="tar.gz">.tar.gz</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="绑定附件">
            <el-tag v-if="attachmentText" type="success">{{ attachmentText }}</el-tag>
            <el-tag v-else type="warning">请先生成最终压缩包</el-tag>
          </el-form-item>

          <el-form-item>
            <el-button :icon="DocumentAdd" :loading="creating" type="primary" @click="handleCreateDraft">
              生成草稿
            </el-button>
            <el-button :disabled="!draft" :loading="saving" @click="handleSaveDraft">
              保存修改
            </el-button>
            <el-button :disabled="!draft || draft.status === 'sent'" :loading="sending" type="danger" @click="handleSendDraft">
              确认发送
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </section>
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus';
import { DocumentAdd, Refresh } from '@element-plus/icons-vue';
import { useRoute } from 'vue-router';

import MainLayout from '@/layouts/MainLayout.vue';
import { createMailDraft, sendMailDraft, updateMailDraft } from '@/services/mailApi';
import { getLatestPackage } from '@/services/packageApi';
import { useAuthStore } from '@/stores/auth';
import { useProjectStore } from '@/stores/project';

import type { MailDraft, PackageArtifact, PackageFormat } from '@/types/project';

const route = useRoute();
const authStore = useAuthStore();
const projectStore = useProjectStore();

const formRef = ref<FormInstance>();
const creating = ref(false);
const saving = ref(false);
const sending = ref(false);
const loadingLatest = ref(false);
const draft = ref<MailDraft | null>(null);
const latestPackage = ref<PackageArtifact | null>(null);

const form = reactive({
  recipientsText: '',
  subject: '',
  body: '',
  attachmentFormat: 'zip' as PackageFormat,
});

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

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/; // 收件人必须是标准邮箱格式。

/**
 * 将用户输入拆分为收件人列表。
 *
 * @returns 去重后的收件人列表
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
 *
 * @returns 校验通过时返回收件人列表，否则返回 null
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
 * 查询最近压缩包作为草稿附件参考。
 */
async function loadLatestPackage(): Promise<void> {
  if (!projectId.value) {
    return;
  }

  loadingLatest.value = true;

  try {
    latestPackage.value = await getLatestPackage(projectId.value, { userId: authStore.currentUser?.id });
    form.attachmentFormat = latestPackage.value.format;
  } catch {
    latestPackage.value = null;
  } finally {
    loadingLatest.value = false;
  }
}

/**
 * 创建邮件草稿，后端会绑定项目最近一次压缩包。
 */
async function handleCreateDraft(): Promise<void> {
  if (!projectId.value) {
    ElMessage.warning('请先选择项目');
    return;
  }

  const recipients = await validateForm();

  if (!recipients) {
    return;
  }

  creating.value = true;

  try {
    draft.value = await createMailDraft({
      projectId: projectId.value,
      userId: authStore.currentUser?.id,
      recipients,
      subject: form.subject,
      body: form.body,
    });
    ElMessage.success('草稿已生成');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '生成草稿失败');
  } finally {
    creating.value = false;
  }
}

/**
 * 保存草稿修改。
 */
async function handleSaveDraft(): Promise<void> {
  if (!draft.value) {
    return;
  }

  const recipients = await validateForm();

  if (!recipients) {
    return;
  }

  saving.value = true;

  try {
    draft.value = await updateMailDraft({
      draftId: draft.value.draftId,
      userId: authStore.currentUser?.id,
      recipients,
      subject: form.subject,
      body: form.body,
      packageId: latestPackage.value?.packageId ?? draft.value.packageId,
    });
    ElMessage.success('草稿已保存');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存草稿失败');
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
    });
    draft.value = result;
    ElMessage.success(result.message || '邮件已发送');
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '邮件发送失败');
  } finally {
    sending.value = false;
  }
}

onMounted(loadLatestPackage);
</script>

<style scoped>
.mail-layout {
  display: grid;
  gap: 18px;
  max-width: 920px;
}

.panel {
  border-radius: 8px;
}
</style>
