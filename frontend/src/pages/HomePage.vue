<template>
  <MainLayout>
    <template #title>
      <span>项目首页</span>
    </template>

    <template #actions>
      <el-button :icon="Plus" @click="openGroupDialog">新建小组</el-button>
      <el-button type="primary" :icon="Plus" @click="openProjectDialog">新建项目</el-button>
      <el-button :icon="Refresh" :loading="loading" @click="loadHomeData">刷新</el-button>
    </template>

    <template #aside>
      <section class="home-page__aside">
        <h2>小组筛选</h2>
        <el-radio-group v-model="selectedGroupId" class="home-page__group-filter" @change="handleGroupChange">
          <el-radio-button :value="null">全部</el-radio-button>
          <el-radio-button v-for="group in projectStore.groups" :key="group.id" :value="group.id">
            {{ group.name }}
          </el-radio-button>
        </el-radio-group>
      </section>
    </template>

    <section class="home-page">
      <el-card class="home-page__toolbar" shadow="never">
        <el-form class="home-page__search" :inline="true" :model="searchForm" @submit.prevent="runSearch">
          <el-form-item label="项目名">
            <el-input v-model.trim="searchForm.projectKeyword" placeholder="搜索项目" clearable />
          </el-form-item>
          <el-form-item label="文件名">
            <el-input v-model.trim="searchForm.fileKeyword" placeholder="搜索文件" clearable />
          </el-form-item>
          <el-form-item label="成员名">
            <el-input v-model.trim="searchForm.memberKeyword" placeholder="搜索成员" clearable />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" :loading="searching" @click="runSearch">搜索</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-alert
        v-if="errorMessage"
        :title="errorMessage"
        type="warning"
        show-icon
        :closable="false"
      />

      <section class="home-page__summary" aria-label="项目概览">
        <el-card class="home-page__metric" shadow="never">
          <span>最近项目</span>
          <strong>{{ projectStore.projects.length }}</strong>
        </el-card>
        <el-card class="home-page__metric" shadow="never">
          <span>协作中</span>
          <strong>{{ activeCount }}</strong>
        </el-card>
        <el-card class="home-page__metric" shadow="never">
          <span>已结束</span>
          <strong>{{ endedCount }}</strong>
        </el-card>
      </section>

      <el-card class="home-page__list" shadow="never">
        <template #header>
          <div class="home-page__card-header">
            <span>最近参与项目</span>
            <el-tag type="info">{{ projectStore.projects.length }} 个</el-tag>
          </div>
        </template>

        <el-table v-loading="loading" :data="projectRows" empty-text="暂无项目">
          <el-table-column prop="name" label="项目名称" min-width="180" />
          <el-table-column prop="groupName" label="所属小组" min-width="160" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="row.status === 'active' ? 'success' : 'info'">
                {{ row.status === 'active' ? '协作中' : '已结束' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="最近更新" min-width="160">
            <template #default="{ row }">
              {{ formatDate(row.updatedAt || row.reopenedAt || row.endedAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <RouterLink class="home-page__link" :to="`/projects/${row.id}`">进入工作台</RouterLink>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

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
import { ElMessage } from 'element-plus';
import { Plus, Refresh, Search } from '@element-plus/icons-vue';

import MainLayout from '@/layouts/MainLayout.vue';
import {
  createGroup,
  createProject,
  listGroups,
  listProjects,
  searchFiles,
  searchMembers,
  searchProjects,
} from '@/services/groupProjectApi';
import { useAuthStore } from '@/stores/auth';
import { useProjectStore } from '@/stores/project';
import type { FileHit, Group, MemberHit, Project, ProjectHit, SearchResult } from '@/types/project';

type ProjectRow = Project & {
  groupName: string;
  updatedAt?: string;
};

const authStore = useAuthStore();
const projectStore = useProjectStore();
const loading = ref(false);
const searching = ref(false);
const creatingGroup = ref(false);
const creatingProject = ref(false);
const groupDialogVisible = ref(false);
const projectDialogVisible = ref(false);
const errorMessage = ref('');
const selectedGroupId = ref<number | null>(projectStore.filters.groupId);
const searchForm = reactive({
  projectKeyword: '',
  fileKeyword: '',
  memberKeyword: '',
});
const groupForm = reactive({
  name: '',
});
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

const activeCount = computed(() => projectStore.projects.filter((project) => project.status === 'active').length);
const endedCount = computed(() => projectStore.projects.filter((project) => project.status === 'ended').length);
const hasSearchResult = computed(
  () => searchResults.projects.length > 0 || searchResults.files.length > 0 || searchResults.members.length > 0,
);
const projectRows = computed<ProjectRow[]>(() => {
  const groupNameMap = new Map<number, string>(projectStore.groups.map((group) => [group.id, group.name]));

  return projectStore.projects.map((project) => ({
    ...project,
    groupName: groupNameMap.get(project.groupId) ?? `小组 ${project.groupId}`,
  }));
});

/**
 * 统一读取当前用户标识，供测试期 X-User-Id 请求头复用。
 *
 * @returns 当前用户标识，未登录时返回 undefined
 */
function currentUserId(): number | undefined {
  return authStore.currentUser?.id;
}

/**
 * 格式化列表中的日期展示。
 *
 * @param value 日期字符串
 * @returns 本地化后的日期文本
 */
function formatDate(value?: string | null): string {
  if (!value) {
    return '暂无记录';
  }

  return new Date(value).toLocaleString('zh-CN', { hour12: false });
}

/**
 * 解析搜索接口可能返回的统一搜索结构或命中数组。
 *
 * @param result 搜索接口响应
 * @param key 需要读取的结果字段
 * @returns 命中项数组
 */
function normalizeSearchResult<T extends ProjectHit | FileHit | MemberHit>(
  result: SearchResult | T[],
  key: keyof SearchResult,
): T[] {
  // 后端可返回统一结构，也可按单类搜索直接返回数组，页面在边界处做兼容。
  if (Array.isArray(result)) {
    return result;
  }

  return result[key] as T[];
}

/**
 * 打开小组创建弹窗并重置表单。
 */
function openGroupDialog(): void {
  groupForm.name = '';
  groupDialogVisible.value = true;
}

/**
 * 打开项目创建弹窗，默认选中当前筛选小组或列表首个小组。
 */
function openProjectDialog(): void {
  if (projectStore.groups.length === 0) {
    ElMessage.warning('请先创建小组，再创建项目');
    return;
  }

  projectForm.groupId = selectedGroupId.value ?? projectStore.groups[0].id;
  projectForm.name = '';
  projectDialogVisible.value = true;
}

/**
 * 提交小组创建请求并刷新首页数据。
 */
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

/**
 * 提交项目创建请求并刷新首页数据。
 */
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
    selectedGroupId.value = projectForm.groupId;
    projectStore.setFilters({ groupId: projectForm.groupId });
    await loadHomeData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '项目创建失败';
  } finally {
    creatingProject.value = false;
  }
}

/**
 * 加载首页小组和最近参与项目。
 */
async function loadHomeData(): Promise<void> {
  loading.value = true;
  errorMessage.value = '';

  try {
    const [groupPage, projectPage] = await Promise.all([
      listGroups({ page: 1, size: 50 }, { userId: currentUserId() }),
      listProjects(
        {
          page: 1,
          size: 20,
          groupId: selectedGroupId.value ?? undefined,
          keyword: projectStore.filters.keyword || undefined,
        },
        { userId: currentUserId() },
      ),
    ]);
    projectStore.setGroups(groupPage.items as Group[]);
    projectStore.setProjects(projectPage.items as Project[]);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '首页数据加载失败';
  } finally {
    loading.value = false;
  }
}

/**
 * 切换小组筛选后刷新项目列表。
 */
async function handleGroupChange(): Promise<void> {
  projectStore.setFilters({ groupId: selectedGroupId.value });
  await loadHomeData();
}

/**
 * 执行项目、文件和成员基础搜索。
 */
async function runSearch(): Promise<void> {
  searching.value = true;
  errorMessage.value = '';
  projectStore.setFilters({ keyword: searchForm.projectKeyword });

  try {
    const [projects, files, members] = await Promise.all([
      searchForm.projectKeyword
        ? searchProjects({ keyword: searchForm.projectKeyword, userId: currentUserId() })
        : Promise.resolve([] as ProjectHit[]),
      searchForm.fileKeyword
        ? searchFiles({ keyword: searchForm.fileKeyword, userId: currentUserId() })
        : Promise.resolve([] as FileHit[]),
      searchForm.memberKeyword
        ? searchMembers({ keyword: searchForm.memberKeyword, userId: currentUserId() })
        : Promise.resolve([] as MemberHit[]),
    ]);
    searchResults.projects = normalizeSearchResult(projects, 'projects');
    searchResults.files = normalizeSearchResult(files, 'files');
    searchResults.members = normalizeSearchResult(members, 'members');
    await loadHomeData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '搜索失败，请稍后重试';
  } finally {
    searching.value = false;
  }
}

onMounted(loadHomeData);
</script>

<style scoped>
.home-page {
  display: grid;
  gap: 18px;
}

.home-page__aside h2 {
  margin: 0 0 12px;
  font-size: 14px;
}

.home-page__group-filter {
  display: grid;
  gap: 8px;
}

.home-page__toolbar,
.home-page__list,
.home-page__search-result,
.home-page__metric {
  border-radius: 8px;
}

.home-page__search {
  display: flex;
  flex-wrap: wrap;
  gap: 2px 12px;
}

.home-page__summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.home-page__metric :deep(.el-card__body) {
  display: grid;
  gap: 8px;
}

.home-page__metric span {
  color: #687386;
}

.home-page__metric strong {
  font-size: 28px;
  line-height: 1;
}

.home-page__card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.home-page__link {
  color: #1d4f91;
  font-weight: 600;
  text-decoration: none;
}

@media (max-width: 768px) {
  .home-page__summary {
    grid-template-columns: 1fr;
  }
}
</style>
