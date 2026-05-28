<template>
  <MainLayout>
    <template #title>
      <span>{{ group?.name || '小组详情' }}</span>
    </template>

    <template #actions>
      <el-button :icon="Refresh" :loading="loading" @click="loadGroupDetail">刷新</el-button>
    </template>

    <template #aside>
      <section class="group-page__aside">
        <h2>邀请设置</h2>
        <el-form label-position="top" :model="invitationForm">
          <el-form-item label="目标项目">
            <el-select v-model="invitationForm.projectId" placeholder="选择项目">
              <el-option v-for="project in projects" :key="project.id" :label="project.name" :value="project.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="加入模式">
            <el-segmented v-model="invitationForm.mode" :options="invitationModeOptions" />
          </el-form-item>
          <el-form-item label="默认角色">
            <el-select v-model="invitationForm.roleTemplate">
              <el-option label="成员" value="MEMBER" />
              <el-option label="只读" value="READ_ONLY" />
            </el-select>
          </el-form-item>
          <el-button type="primary" :loading="creatingInvitation" @click="handleCreateInvitation">
            生成邀请
          </el-button>
        </el-form>

        <el-alert
          v-if="invitation"
          class="group-page__invite-result"
          type="success"
          show-icon
          :closable="false"
        >
          <p>邀请码：{{ invitation.code }}</p>
          <p class="group-page__invite-url">{{ invitation.invitationUrl }}</p>
        </el-alert>
      </section>
    </template>

    <section class="group-page">
      <el-alert v-if="errorMessage" :title="errorMessage" type="warning" show-icon :closable="false" />

      <!-- 小组头部 -->
      <section class="group-page__header" aria-label="小组信息">
        <div class="group-page__header-main">
          <h1 class="group-page__header-name">{{ group?.name }}</h1>
          <el-tag :type="group?.status === 'active' ? 'success' : 'info'" size="small" effect="plain">
            {{ group?.status === 'active' ? '正常' : '停用' }}
          </el-tag>
        </div>
        <div class="group-page__header-meta">
          <span>{{ projects.length }} 个项目</span>
          <span>{{ members.length }} 位成员</span>
        </div>
      </section>

      <!-- 项目列表：GitHub 仓库列表风格 -->
      <section class="group-page__repos" aria-label="项目列表">
        <div class="group-page__repos-header">
          <span>项目</span>
          <el-tag type="info" size="small">{{ projects.length }} 个</el-tag>
        </div>

        <div v-if="projects.length === 0" class="group-page__repos-empty">
          <el-empty description="暂无项目">
            <el-button type="primary" size="small" @click="router.push('/')">去创建项目</el-button>
          </el-empty>
        </div>

        <div v-else class="group-page__repo-list">
          <div
            v-for="project in projects"
            :key="project.id"
            class="repo-row"
            @click="router.push(`/projects/${project.id}`)"
          >
            <div class="repo-row__icon">
              <FolderOpened />
            </div>
            <div class="repo-row__body">
              <div class="repo-row__top">
                <RouterLink
                  :to="`/projects/${project.id}`"
                  class="repo-row__name"
                  @click.stop
                >
                  {{ project.name }}
                </RouterLink>
                <el-tag :type="project.status === 'active' ? 'success' : 'info'" size="small" effect="plain">
                  {{ project.status === 'active' ? '协作中' : '已结束' }}
                </el-tag>
              </div>
              <div class="repo-row__meta">
                <span>项目编号 {{ project.id }}</span>
                <span v-if="project.endedAt">结束于 {{ formatDate(project.endedAt) }}</span>
                <span v-else>创建后未结束</span>
              </div>
            </div>
            <div class="repo-row__action">
              <el-button size="small" @click.stop="router.push(`/projects/${project.id}`)">进入工作台</el-button>
            </div>
          </div>
        </div>
      </section>

      <!-- 成员列表 -->
      <el-card shadow="never">
        <template #header>
          <div class="group-page__card-header">
            <span>成员列表</span>
            <el-select v-model="selectedProjectId" class="group-page__member-project" @change="loadMembers">
              <el-option v-for="project in projects" :key="project.id" :label="project.name" :value="project.id" />
            </el-select>
          </div>
        </template>

        <el-table v-loading="membersLoading" :data="members" empty-text="请选择项目查看成员权限">
          <el-table-column prop="userName" label="成员" min-width="160" />
          <el-table-column prop="userId" label="用户编号" width="110" />
          <el-table-column label="角色" width="120">
            <template #default="{ row }">{{ roleTemplateName(row.roleTemplate) }}</template>
          </el-table-column>
          <el-table-column label="权限数量" width="120">
            <template #default="{ row }">{{ row.permissions.length }}</template>
          </el-table-column>
        </el-table>
      </el-card>
    </section>
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { FolderOpened, Refresh } from '@element-plus/icons-vue';

import MainLayout from '@/layouts/MainLayout.vue';
import { getGroup, listProjects } from '@/services/groupProjectApi';
import { createInvitation, getProjectPermissions } from '@/services/memberPermissionApi';
import { useAuthStore } from '@/stores/auth';
import { useProjectStore } from '@/stores/project';
import type { Group, Invitation, InvitationMode, MemberPermission, Project, RoleTemplate } from '@/types/project';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const projectStore = useProjectStore();
const group = ref<Group | null>(null);
const projects = ref<Project[]>([]);
const members = ref<MemberPermission[]>([]);
const invitation = ref<Invitation | null>(null);
const loading = ref(false);
const membersLoading = ref(false);
const creatingInvitation = ref(false);
const errorMessage = ref('');
const selectedProjectId = ref<number | null>(null);
const invitationForm = reactive<{
  projectId: number | null;
  mode: InvitationMode;
  roleTemplate: RoleTemplate;
}>({
  projectId: null,
  mode: 'direct',
  roleTemplate: 'MEMBER',
});

const invitationModeOptions = [
  { label: '直接加入', value: 'direct' },
  { label: '需要审核', value: 'review' },
];
const groupId = computed(() => Number(route.params.groupId || route.params.id || projectStore.currentGroup?.id || 0));

function currentUserId(): number | undefined {
  return authStore.currentUser?.id;
}

function formatDate(value?: string | null): string {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '暂无';
}

function roleTemplateName(roleTemplate: RoleTemplate): string {
  const names: Record<RoleTemplate, string> = {
    OWNER: '负责人',
    MEMBER: '成员',
    READ_ONLY: '只读',
  };

  return names[roleTemplate] ?? roleTemplate;
}

async function loadGroupDetail(): Promise<void> {
  if (!groupId.value) {
    errorMessage.value = '缺少小组标识，无法加载小组详情';
    return;
  }

  loading.value = true;
  errorMessage.value = '';

  try {
    const [groupDetail, projectPage] = await Promise.all([
      getGroup(groupId.value, { userId: currentUserId() }),
      listProjects({ page: 1, size: 50, groupId: groupId.value }, { userId: currentUserId() }),
    ]);
    group.value = groupDetail;
    projects.value = projectPage.items;
    projectStore.setCurrentGroup(groupDetail);
    projectStore.setProjects(projectPage.items);

    if (!selectedProjectId.value && projects.value.length > 0) {
      selectedProjectId.value = projects.value[0].id;
      invitationForm.projectId = projects.value[0].id;
    }

    await loadMembers();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '小组详情加载失败';
  } finally {
    loading.value = false;
  }
}

async function loadMembers(): Promise<void> {
  if (!selectedProjectId.value) {
    members.value = [];
    return;
  }

  membersLoading.value = true;

  try {
    const response = await getProjectPermissions(selectedProjectId.value, { userId: currentUserId() });
    members.value = response.members;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '成员列表加载失败';
  } finally {
    membersLoading.value = false;
  }
}

async function handleCreateInvitation(): Promise<void> {
  if (!groupId.value || !invitationForm.projectId) {
    errorMessage.value = '请先选择小组项目';
    return;
  }

  creatingInvitation.value = true;
  errorMessage.value = '';

  try {
    invitation.value = await createInvitation({
      groupId: groupId.value,
      projectId: invitationForm.projectId,
      mode: invitationForm.mode,
      roleTemplate: invitationForm.roleTemplate,
      userId: currentUserId(),
    });
    ElMessage.success('邀请已生成');
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '邀请生成失败';
  } finally {
    creatingInvitation.value = false;
  }
}

onMounted(loadGroupDetail);
</script>

<style scoped>
.group-page {
  display: grid;
  gap: 18px;
}

.group-page__aside h2 {
  margin: 0 0 12px;
  font-size: 14px;
}

.group-page__invite-result {
  margin-top: 14px;
}

.group-page__invite-result p {
  margin: 0;
}

.group-page__invite-url {
  word-break: break-all;
}

/* ---- 小组头部 ---- */
.group-page__header {
  padding: 18px 20px;
  background: var(--cb-bg-card);
  border: 1px solid var(--cb-border);
  border-radius: var(--cb-radius-md);
}

.group-page__header-main {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.group-page__header-name {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--cb-text-primary);
}

.group-page__header-meta {
  display: flex;
  gap: 16px;
  color: var(--cb-text-secondary);
  font-size: 13px;
}

/* ---- 项目列表（GitHub repo list 风格） ---- */
.group-page__repos {
  background: var(--cb-bg-card);
  border: 1px solid var(--cb-border);
  border-radius: var(--cb-radius-md);
  overflow: hidden;
}

.group-page__repos-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  background: var(--cb-bg-page);
  border-bottom: 1px solid var(--cb-border-light);
  font-weight: 600;
  font-size: 14px;
}

.group-page__repos-empty {
  padding: 40px 0;
}

.group-page__repo-list {
  display: grid;
}

.repo-row {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  border-bottom: 1px solid var(--cb-border-light);
  cursor: pointer;
  transition: background 0.1s;
}

.repo-row:last-child {
  border-bottom: none;
}

.repo-row:hover {
  background: var(--cb-bg-page);
}

.repo-row__icon {
  color: #9e9eb8;
  width: 22px;
  height: 22px;
  flex-shrink: 0;
}

.repo-row__body {
  flex: 1;
  min-width: 0;
}

.repo-row__top {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}

.repo-row__name {
  color: var(--cb-color-primary);
  font-size: 15px;
  font-weight: 600;
  text-decoration: none;
}

.repo-row__name:hover {
  text-decoration: underline;
}

.repo-row__meta {
  display: flex;
  gap: 14px;
  font-size: 12px;
  color: var(--cb-text-muted);
}

.repo-row__action {
  flex-shrink: 0;
}

/* ---- 成员卡片 ---- */
.group-page__card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.group-page__member-project {
  width: 220px;
}

@media (max-width: 768px) {
  .group-page__header-meta {
    flex-direction: column;
    gap: 4px;
  }

  .repo-row {
    flex-wrap: wrap;
    gap: 10px;
  }

  .repo-row__action {
    width: 100%;
    text-align: right;
  }

  .group-page__card-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .group-page__member-project {
    width: 100%;
  }
}
</style>
