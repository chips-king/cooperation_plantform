<template>
  <MainLayout>
    <template #title>
      <span>项目总览</span>
    </template>

    <template #actions>
      <el-button :icon="Plus" @click="openGroupDialog">新建小组</el-button>
      <el-button type="primary" :icon="Plus" @click="openProjectDialog">新建项目</el-button>
      <el-button :icon="Refresh" :loading="loading" @click="loadHomeData">刷新</el-button>
    </template>

    <template #aside>
      <nav class="home-page__aside">
        <div class="home-page__aside-label">最近项目</div>
        <div v-if="recentProjects.length === 0" class="home-page__aside-empty">暂无项目</div>
        <RouterLink
          v-for="project in recentProjects"
          :key="project.id"
          class="home-page__project-link"
          :to="`/projects/${project.id}`"
        >
          <span class="home-page__project-link-name">{{ project.name }}</span>
          <el-tag
            :type="project.status === 'active' ? 'success' : 'info'"
            size="small"
            effect="plain"
          >
            {{ project.status === 'active' ? '协作中' : '已结束' }}
          </el-tag>
        </RouterLink>
      </nav>
    </template>

    <section class="home-page">
      <el-alert v-if="errorMessage" :title="errorMessage" type="warning" show-icon :closable="false" />

      <section v-if="loading" class="home-page__loading">
        <el-skeleton :rows="6" animated />
      </section>

      <template v-else>
        <div v-if="projectStore.projects.length === 0" class="home-page__empty">
          <el-empty description="暂无项目">
            <template #description>
              <p style="margin: 8px 0; color: var(--cb-text-secondary)">暂无项目，请先创建小组和项目</p>
            </template>
            <el-button type="primary" :icon="Plus" @click="openProjectDialog">创建项目</el-button>
          </el-empty>
        </div>

        <div v-else class="project-grid">
          <div
            v-for="project in projectStore.projects"
            :key="project.id"
            class="project-card"
          >
            <div class="project-card__header">
              <RouterLink class="project-card__name-link" :to="`/projects/${project.id}`">
                <h3 class="project-card__name">{{ project.name }}</h3>
              </RouterLink>
              <div class="project-card__header-actions">
                <el-tag
                  :type="project.status === 'active' ? 'success' : 'info'"
                  size="small"
                  effect="plain"
                >
                  {{ project.status === 'active' ? '协作中' : '已结束' }}
                </el-tag>
                <el-button
                  :icon="Delete"
                  size="small"
                  circle
                  type="danger"
                  plain
                  @click.stop="handleDeleteProject(project)"
                />
              </div>
            </div>
            <div class="project-card__group">
              {{ getGroupName(project.groupId) }}
            </div>
            <div class="project-card__stats">
              <span class="project-card__stat">
                <FolderOpened class="project-card__stat-icon" />
                {{ getProjectDirCount(project.id) }} 个目录
              </span>
              <span class="project-card__stat">
                <Document class="project-card__stat-icon" />
                {{ getProjectFileCount(project.id) }} 个文件
              </span>
            </div>
            <div class="project-card__footer">
              <span class="project-card__time">{{ formatDateTime(getProjectUpdatedAt(project.id)) }}</span>
              <RouterLink class="project-card__enter-btn" :to="`/projects/${project.id}`">
                进入项目
              </RouterLink>
            </div>
          </div>
        </div>
      </template>

      <!-- 新建小组弹窗 -->
      <el-dialog v-model="groupDialogVisible" title="新建小组" width="420px">
        <el-form label-position="top" @submit.prevent>
          <el-form-item label="小组名称" required>
            <el-input v-model.trim="groupForm.name" maxlength="40" show-word-limit placeholder="输入小组名称" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="groupDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="creatingGroup" @click="submitGroup">创建</el-button>
        </template>
      </el-dialog>

      <!-- 新建项目弹窗 -->
      <el-dialog v-model="projectDialogVisible" title="新建项目" width="460px">
        <el-form label-position="top" @submit.prevent>
          <el-form-item label="所属小组" required>
            <el-select v-model="projectForm.groupId" placeholder="选择小组" filterable>
              <el-option
                v-for="group in projectStore.groups"
                :key="group.id"
                :label="group.name"
                :value="group.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="项目名称" required>
            <el-input v-model.trim="projectForm.name" maxlength="60" show-word-limit placeholder="输入项目名称" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="projectDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="creatingProject" @click="submitProject">创建</el-button>
        </template>
      </el-dialog>
    </section>
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Delete, Document, FolderOpened, Plus, Refresh } from '@element-plus/icons-vue';

import MainLayout from '@/layouts/MainLayout.vue';
import {
  createGroup,
  createProject,
  deleteProject,
  listGroups,
  listProjects,
} from '@/services/groupProjectApi';
import { getProjectProgress } from '@/services/fileApi';
import { useAuthStore } from '@/stores/auth';
import { useProjectStore } from '@/stores/project';
import type { Group, Project, ProjectProgress } from '@/types/project';

const authStore = useAuthStore();
const projectStore = useProjectStore();
const loading = ref(false);
const creatingGroup = ref(false);
const creatingProject = ref(false);
const groupDialogVisible = ref(false);
const projectDialogVisible = ref(false);
const errorMessage = ref('');

const groupForm = reactive({ name: '' });
const projectForm = reactive<{ groupId: number | null; name: string }>({
  groupId: null,
  name: '',
});

/** 项目进度数据，key 为 projectId */
const projectProgressMap = ref<Record<number, ProjectProgress>>({});

const recentProjects = computed(() => projectStore.projects.slice(0, 6));

/** 获取小组名称 */
function getGroupName(groupId: number): string {
  const group = projectStore.groups.find((g) => g.id === groupId);
  return group?.name ?? '未知小组';
}

/** 获取项目的目录数量 */
function getProjectDirCount(projectId: number): number {
  const progress = projectProgressMap.value[projectId];
  return progress?.totalDirectoryCount ?? 0;
}

/** 获取项目的文件数量 */
function getProjectFileCount(projectId: number): number {
  const progress = projectProgressMap.value[projectId];
  if (!progress?.directories) return 0;
  return progress.directories.reduce((sum, dir) => sum + dir.fileCount, 0);
}

/** 获取项目最近更新时间 */
function getProjectUpdatedAt(projectId: number): string {
  const progress = projectProgressMap.value[projectId];
  if (!progress?.directories?.length) return '';
  const timestamps = progress.directories
    .map((d) => d.updatedAt)
    .filter(Boolean)
    .sort()
    .reverse();
  return timestamps[0] ?? '';
}

function currentUserId(): number | undefined {
  return authStore.currentUser?.id;
}

function formatDateTime(value: string): string {
  if (!value) return '-';
  return new Date(value).toLocaleString();
}

function openGroupDialog(): void {
  groupForm.name = '';
  groupDialogVisible.value = true;
}

function openProjectDialog(): void {
  if (projectStore.groups.length === 0) {
    ElMessage.warning('请先创建小组，再创建项目');
    return;
  }
  projectForm.groupId = projectStore.groups[0]?.id ?? null;
  projectForm.name = '';
  projectDialogVisible.value = true;
}

async function submitGroup(): Promise<void> {
  if (!groupForm.name) {
    ElMessage.warning('请输入小组名称');
    return;
  }
  creatingGroup.value = true;
  errorMessage.value = '';
  try {
    await createGroup({ name: groupForm.name, userId: currentUserId() });
    ElMessage.success('小组创建成功');
    groupDialogVisible.value = false;
    await loadHomeData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '小组创建失败';
  } finally {
    creatingGroup.value = false;
  }
}

async function handleDeleteProject(project: Project): Promise<void> {
  try {
    await ElMessageBox.confirm(`删除项目「${project.name}」后不可恢复，确认继续？`, '删除项目', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    });
  } catch {
    return;
  }

  try {
    await deleteProject(project.id);
    ElMessage.success('项目已删除');
    await loadHomeData();
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败');
  }
}

async function submitProject(): Promise<void> {
  if (!projectForm.groupId) {
    ElMessage.warning('请选择所属小组');
    return;
  }
  if (!projectForm.name) {
    ElMessage.warning('请输入项目名称');
    return;
  }
  creatingProject.value = true;
  errorMessage.value = '';
  try {
    await createProject({ groupId: projectForm.groupId, name: projectForm.name, userId: currentUserId() });
    ElMessage.success('项目创建成功');
    projectDialogVisible.value = false;
    await loadHomeData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '项目创建失败';
  } finally {
    creatingProject.value = false;
  }
}

async function loadAllProgress(projects: Project[]): Promise<void> {
  const results = await Promise.allSettled(
    projects.map((p) => getProjectProgress(String(p.id))),
  );

  const map: Record<number, ProjectProgress> = {};
  results.forEach((result, index) => {
    if (result.status === 'fulfilled') {
      const project = projects[index];
      map[project.id] = result.value;
    }
  });

  projectProgressMap.value = map;
}

async function loadHomeData(): Promise<void> {
  loading.value = true;
  errorMessage.value = '';
  try {
    const [groupPage, projectPage] = await Promise.all([
      listGroups({ page: 1, size: 50 }, { userId: currentUserId() }),
      listProjects({ page: 1, size: 20 }, { userId: currentUserId() }),
    ]);
    projectStore.setGroups(groupPage.items as Group[]);
    projectStore.setProjects(projectPage.items as Project[]);

    await loadAllProgress(projectPage.items as Project[]);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '首页数据加载失败';
  } finally {
    loading.value = false;
  }
}

onMounted(loadHomeData);
</script>

<style scoped>
.home-page { display: grid; gap: 20px; }

/* ---- 侧边栏 ---- */
.home-page__aside-label {
  padding: 0 0 8px;
  color: #6b6b8a; font-size: 11px; font-weight: 600;
  text-transform: uppercase; letter-spacing: 0.8px;
}

.home-page__aside-empty {
  padding: 12px 0;
  color: #9e9eb8;
  font-size: 13px;
}

.home-page__project-link {
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

.home-page__project-link:hover {
  color: #e8e8f0;
  background: rgba(255,255,255,0.06);
}

.home-page__project-link-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ---- 加载和空态 ---- */
.home-page__loading { padding: 40px 0; }
.home-page__empty { padding: 60px 0; text-align: center; color: var(--cb-text-muted); }

/* ---- 项目卡片网格 ---- */
.project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.project-card {
  display: flex;
  flex-direction: column;
  padding: 20px;
  background: #ffffff;
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  text-decoration: none;
  color: inherit;
  transition: box-shadow 0.2s ease, border-color 0.2s ease;
}

.project-card:hover {
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.08);
  border-color: #c0c4cc;
}

.project-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.project-card__name-link {
  text-decoration: none;
  color: inherit;
  min-width: 0;
}

.project-card__name {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-card__name-link:hover .project-card__name {
  color: #409eff;
}

.project-card__header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.project-card__group {
  font-size: 13px;
  color: #909399;
  margin-bottom: 16px;
}

.project-card__stats {
  display: flex;
  gap: 20px;
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.project-card__stat {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
}

.project-card__stat-icon {
  width: 14px;
  height: 14px;
  color: #909399;
}

.project-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
}

.project-card__time {
  font-size: 12px;
  color: #c0c4cc;
}

.project-card__enter-btn {
  font-size: 13px;
  color: #409eff;
  text-decoration: none;
  font-weight: 500;
}

.project-card__enter-btn:hover {
  color: #66b1ff;
}

@media (max-width: 768px) {
  .project-grid {
    grid-template-columns: 1fr;
  }
}
</style>
