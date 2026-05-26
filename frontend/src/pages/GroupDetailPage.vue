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

      <section class="group-page__summary" aria-label="小组概览">
        <el-card shadow="never">
          <span>小组状态</span>
          <strong>{{ group?.status === 'active' ? '正常' : group?.status || '未知' }}</strong>
        </el-card>
        <el-card shadow="never">
          <span>项目数量</span>
          <strong>{{ projects.length }}</strong>
        </el-card>
        <el-card shadow="never">
          <span>当前成员视图</span>
          <strong>{{ members.length }}</strong>
        </el-card>
      </section>

      <el-card shadow="never">
        <template #header>
          <div class="group-page__card-header">
            <span>小组项目</span>
            <el-tag type="info">{{ projects.length }} 个</el-tag>
          </div>
        </template>

        <el-table v-loading="loading" :data="projects" empty-text="暂无项目" @row-click="selectProject">
          <el-table-column prop="name" label="项目名称" min-width="180" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="row.status === 'active' ? 'success' : 'info'">
                {{ row.status === 'active' ? '协作中' : '已结束' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="结束时间" min-width="160">
            <template #default="{ row }">{{ formatDate(row.endedAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <RouterLink class="group-page__link" :to="`/projects/${row.id}`">进入工作台</RouterLink>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

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
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';

import MainLayout from '@/layouts/MainLayout.vue';
import { getGroup, listProjects } from '@/services/groupProjectApi';
import { createInvitation, getProjectPermissions } from '@/services/memberPermissionApi';
import { useAuthStore } from '@/stores/auth';
import { useProjectStore } from '@/stores/project';
import type { Group, Invitation, InvitationMode, MemberPermission, Project, RoleTemplate } from '@/types/project';

const route = useRoute();
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

/**
 * 读取当前用户标识。
 *
 * @returns 当前用户标识，未登录时返回 undefined
 */
function currentUserId(): number | undefined {
  return authStore.currentUser?.id;
}

/**
 * 格式化日期展示。
 *
 * @param value 日期字符串
 * @returns 本地化后的日期文本
 */
function formatDate(value?: string | null): string {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '暂无';
}

/**
 * 转换角色模板中文名称。
 *
 * @param roleTemplate 角色模板编码
 * @returns 角色中文名称
 */
function roleTemplateName(roleTemplate: RoleTemplate): string {
  const names: Record<RoleTemplate, string> = {
    OWNER: '负责人',
    MEMBER: '成员',
    READ_ONLY: '只读',
  };

  return names[roleTemplate] ?? roleTemplate;
}

/**
 * 加载小组详情、项目列表和默认项目成员。
 */
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

    // 首次进入小组时默认展示第一个项目的成员权限，避免成员区域空置。
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

/**
 * 根据选中的项目加载成员权限列表。
 */
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

/**
 * 选中项目后同步成员和邀请目标。
 *
 * @param project 当前点击的项目
 */
async function selectProject(project: Project): Promise<void> {
  selectedProjectId.value = project.id;
  invitationForm.projectId = project.id;
  await loadMembers();
}

/**
 * 创建邀请链接或邀请码。
 */
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

.group-page__summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.group-page__summary :deep(.el-card__body) {
  display: grid;
  gap: 8px;
}

.group-page__summary span {
  color: #687386;
}

.group-page__summary strong {
  font-size: 24px;
}

.group-page__card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.group-page__member-project {
  width: 220px;
}

.group-page__link {
  color: #1d4f91;
  font-weight: 600;
  text-decoration: none;
}

@media (max-width: 768px) {
  .group-page__summary {
    grid-template-columns: 1fr;
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
