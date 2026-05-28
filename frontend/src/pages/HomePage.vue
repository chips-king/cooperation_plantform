<template>
  <MainLayout>
    <template #title>
      <span>我的协作空间</span>
    </template>

    <template #actions>
      <el-input
        v-model.trim="searchForm.projectKeyword"
        placeholder="搜索项目、文件或成员"
        :prefix-icon="Search"
        size="default"
        clearable
        style="width: 260px"
        @keyup.enter="runSearch"
      />
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
        <el-skeleton :rows="4" animated />
      </section>

      <template v-else>
        <!-- 状态筛选导航栏 -->
        <div class="home-page__status-nav">
          <div
            v-for="tab in statusTabs"
            :key="tab.key"
            class="home-page__status-tab"
            :class="{ 'home-page__status-tab--active': activeStatus === tab.key }"
            @click="activeStatus = tab.key"
          >
            <span class="home-page__status-tab-label">{{ tab.label }}</span>
            <span class="home-page__status-tab-count">{{ tab.count }}</span>
          </div>
        </div>

        <!-- 项目卡片列表 -->
        <div v-if="filteredProjects.length === 0" class="home-page__empty">
          <el-empty description="暂无项目">
            <template #description>
              <p style="margin: 8px 0; color: var(--cb-text-secondary)">暂无{{ statusTabs.find(t => t.key === activeStatus)?.label }}项目</p>
            </template>
            <el-button type="primary" :icon="Plus" @click="openProjectDialog">创建项目</el-button>
          </el-empty>
        </div>

        <div v-else class="home-page__project-grid">
          <div
            v-for="project in filteredProjects"
            :key="project.id"
            class="home-page__project-card"
          >
            <RouterLink class="home-page__project-card-link" :to="`/projects/${project.id}`"
            >
              <div class="home-page__project-card-header">
                <FolderOpened class="home-page__project-card-icon" />
                <span class="home-page__project-card-name">{{ project.name }}</span>
              </div>
              <div class="home-page__project-card-footer">
                <el-tag :type="project.status === 'active' ? 'success' : 'info'" size="small" effect="plain">
                  {{ project.status === 'active' ? '协作中' : '已结束' }}
                </el-tag>
                <span class="home-page__project-card-group">{{ getGroupName(project.groupId) }}</span>
              </div>
            </RouterLink>
            <button
              class="home-page__project-delete"
              title="删除项目"
              @click.stop="confirmDeleteProject(project)"
            >
              <Delete class="home-page__project-delete-icon" />
            </button>
          </div>
        </div>
      </template>

      <!-- 搜索结果 -->
      <el-card v-if="hasSearchResult" class="home-page__search-result" shadow="never">
        <template #header>
          <span>搜索结果</span>
        </template>

        <el-tabs>
          <el-tab-pane :label="`项目 ${searchResults.projects.length}`">
            <el-table :data="searchResults.projects" empty-text="暂无匹配项目">
              <el-table-column prop="projectName" label="项目名称" />
              <el-table-column prop="groupId" label="小组编号" width="120" />
            </el-table>
          </el-tab-pane>
          <el-tab-pane :label="`文件 ${searchResults.files.length}`">
            <el-table :data="searchResults.files" empty-text="暂无匹配文件">
              <el-table-column prop="fileName" label="文件名" />
              <el-table-column prop="projectId" label="项目编号" width="120" />
            </el-table>
          </el-tab-pane>
          <el-tab-pane :label="`成员 ${searchResults.members.length}`">
            <el-table :data="searchResults.members" empty-text="暂无匹配成员">
              <el-table-column prop="displayName" label="成员" />
              <el-table-column prop="userId" label="用户编号" width="120" />
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </el-card>

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
import { Delete, FolderOpened, Plus, Refresh, Search } from '@element-plus/icons-vue';

import MainLayout from '@/layouts/MainLayout.vue';
import {
  createGroup,
  createProject,
  deleteGroup,
  deleteProject,
  listGroups,
  listProjects,
  searchFiles,
  searchMembers,
  searchProjects,
} from '@/services/groupProjectApi';
import { useAuthStore } from '@/stores/auth';
import { useProjectStore } from '@/stores/project';
import type { FileHit, Group, MemberHit, Project, ProjectHit, SearchResult } from '@/types/project';

type StatusFilter = 'all' | 'active' | 'ended';

interface StatusTab {
  key: StatusFilter;
  label: string;
  count: number;
}

const authStore = useAuthStore();
const projectStore = useProjectStore();
const loading = ref(false);
const searching = ref(false);
const creatingGroup = ref(false);
const creatingProject = ref(false);
const groupDialogVisible = ref(false);
const projectDialogVisible = ref(false);
const errorMessage = ref('');
const activeStatus = ref<StatusFilter>('all');

const searchForm = reactive({
  projectKeyword: '',
});
const groupForm = reactive({ name: '' });
const projectForm = reactive<{
  groupId: number | null;
  name: string;
}>({
  groupId: null,
  name: '',
});
const searchResults = reactive<{
  projects: ProjectHit[];
  files: FileHit[];
  members: MemberHit[];
}>({
  projects: [],
  files: [],
  members: [],
});

const statusTabs = computed<StatusTab[]>(() => {
  const all = projectStore.projects.length;
  const active = projectStore.projects.filter((p) => p.status === 'active').length;
  const ended = all - active;
  return [
    { key: 'all', label: '全部', count: all },
    { key: 'active', label: '协作中', count: active },
    { key: 'ended', label: '已结束', count: ended },
  ];
});

const filteredProjects = computed(() => {
  if (activeStatus.value === 'all') return projectStore.projects;
  return projectStore.projects.filter((p) => p.status === activeStatus.value);
});

const recentProjects = computed(() => projectStore.projects.slice(0, 6));

const hasSearchResult = computed(
  () => searchResults.projects.length > 0 || searchResults.files.length > 0 || searchResults.members.length > 0,
);

function currentUserId(): number | undefined {
  return authStore.currentUser?.id;
}

function getGroupName(groupId: number): string {
  return projectStore.groups.find((g) => g.id === groupId)?.name ?? '未知小组';
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
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '首页数据加载失败';
  } finally {
    loading.value = false;
  }
}

async function runSearch(): Promise<void> {
  searching.value = true;
  errorMessage.value = '';
  projectStore.setFilters({ keyword: searchForm.projectKeyword });
  try {
    const [projects, files, members] = await Promise.all([
      searchForm.projectKeyword
        ? searchProjects({ keyword: searchForm.projectKeyword, userId: currentUserId() })
        : Promise.resolve([] as ProjectHit[]),
      searchForm.projectKeyword
        ? searchFiles({ keyword: searchForm.projectKeyword, userId: currentUserId() })
        : Promise.resolve([] as FileHit[]),
      searchForm.projectKeyword
        ? searchMembers({ keyword: searchForm.projectKeyword, userId: currentUserId() })
        : Promise.resolve([] as MemberHit[]),
    ]);
    searchResults.projects = Array.isArray(projects) ? projects : (projects as SearchResult).projects ?? [];
    searchResults.files = Array.isArray(files) ? files : (files as SearchResult).files ?? [];
    searchResults.members = Array.isArray(members) ? members : (members as SearchResult).members ?? [];
    await loadHomeData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '搜索失败，请稍后重试';
  } finally {
    searching.value = false;
  }
}

async function confirmDeleteProject(project: Project): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要删除项目「${project.name}」吗？删除后不可恢复。`,
      '删除项目',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger',
      },
    );
    await deleteProject(project.id);
    ElMessage.success('项目已删除');
    await loadHomeData();
  } catch (error) {
    if (error === 'cancel' || (error as Error)?.message === 'cancel') return;
    errorMessage.value = error instanceof Error ? error.message : '删除项目失败';
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

/* ---- 状态筛选导航栏 ---- */
.home-page__status-nav {
  display: flex;
  gap: 4px;
  padding: 4px;
  background: var(--cb-bg-card);
  border: 1px solid var(--cb-border);
  border-radius: var(--cb-radius-md);
}

.home-page__status-tab {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
  flex: 1;
  justify-content: center;
}

.home-page__status-tab:hover {
  background: var(--cb-bg-page);
}

.home-page__status-tab--active {
  background: var(--cb-color-primary);
  color: #fff;
}

.home-page__status-tab-label {
  font-size: 14px;
  font-weight: 500;
}

.home-page__status-tab-count {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
  background: rgba(0,0,0,0.06);
}

.home-page__status-tab--active .home-page__status-tab-count {
  background: rgba(255,255,255,0.2);
}

/* ---- 项目卡片网格 ---- */
.home-page__project-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
}

.home-page__project-card {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
  background: var(--cb-bg-card);
  border: 1px solid var(--cb-border);
  border-radius: var(--cb-radius-md);
  transition: border-color 0.15s, box-shadow 0.15s, transform 0.1s;
}

.home-page__project-card:hover {
  border-color: var(--cb-color-primary);
  box-shadow: var(--cb-shadow-elevated);
  transform: translateY(-2px);
}

.home-page__project-card-link {
  display: flex;
  flex-direction: column;
  gap: 16px;
  text-decoration: none;
  color: inherit;
}

.home-page__project-card-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.home-page__project-card-icon {
  width: 20px;
  height: 20px;
  color: #5470c6;
  flex-shrink: 0;
}

.home-page__project-card-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--cb-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.home-page__project-card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.home-page__project-card-group {
  font-size: 12px;
  color: var(--cb-text-muted);
}

.home-page__project-delete {
  position: absolute;
  top: 8px;
  right: 8px;
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #999;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.15s, background 0.15s, color 0.15s;
}

.home-page__project-card:hover .home-page__project-delete {
  opacity: 1;
}

.home-page__project-delete:hover {
  background: #fde2e2;
  color: #f56c6c;
}

.home-page__project-delete-icon {
  width: 16px;
  height: 16px;
}

/* ---- 搜索结果 ---- */
.home-page__search-result {
  margin-top: 8px;
}

@media (max-width: 768px) {
  .home-page__status-nav {
    flex-direction: column;
  }

  .home-page__project-grid {
    grid-template-columns: 1fr;
  }
}
</style>
