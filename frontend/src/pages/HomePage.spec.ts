import ElementPlus from 'element-plus';
import { flushPromises, mount } from '@vue/test-utils';
import { createPinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import HomePage from './HomePage.vue';
import {
  createGroup,
  createProject,
  listGroups,
  listProjects,
  searchFiles,
  searchMembers,
  searchProjects,
} from '@/services/groupProjectApi';

vi.mock('@/services/groupProjectApi', () => ({
  createGroup: vi.fn(),
  createProject: vi.fn(),
  listGroups: vi.fn(),
  listProjects: vi.fn(),
  searchFiles: vi.fn(),
  searchMembers: vi.fn(),
  searchProjects: vi.fn(),
}));

const mockedCreateGroup = vi.mocked(createGroup);
const mockedCreateProject = vi.mocked(createProject);
const mockedListGroups = vi.mocked(listGroups);
const mockedListProjects = vi.mocked(listProjects);
const mockedSearchProjects = vi.mocked(searchProjects);
const mockedSearchFiles = vi.mocked(searchFiles);
const mockedSearchMembers = vi.mocked(searchMembers);

const groups = [
  { id: 1, name: '前端组', ownerId: 1, status: 'active' },
  { id: 2, name: '后端组', ownerId: 2, status: 'active' },
];

const projects = [
  {
    id: 11,
    groupId: 1,
    name: '组件测试建设',
    ownerId: 1,
    status: 'active' as const,
    endedAt: null,
    reopenedAt: '2026-05-24T10:00:00',
  },
  {
    id: 12,
    groupId: 2,
    name: '交付归档项目',
    ownerId: 2,
    status: 'ended' as const,
    endedAt: '2026-05-23T18:00:00',
    reopenedAt: null,
  },
];

/**
 * 挂载首页组件并注入 Element Plus 与 Pinia。
 *
 * @returns 首页组件测试包装器
 */
function mountHomePage() {
  return mount(HomePage, {
    global: {
      plugins: [ElementPlus, createPinia()],
      stubs: {
        RouterLink: {
          props: ['to'],
          template: '<a :href="String(to)"><slot /></a>',
        },
      },
    },
  });
}

describe('HomePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedListGroups.mockResolvedValue({ items: groups, total: groups.length, page: 1, size: 50 });
    mockedListProjects.mockResolvedValue({ items: projects, total: projects.length, page: 1, size: 20 });
    mockedCreateGroup.mockResolvedValue({ groupId: 3 });
    mockedCreateProject.mockResolvedValue({ projectId: 13, status: 'active', updatedAt: '2026-05-25T10:00:00' });
    mockedSearchProjects.mockResolvedValue([]);
    mockedSearchFiles.mockResolvedValue([]);
    mockedSearchMembers.mockResolvedValue([]);
  });

  it('展示最近参与项目和小组筛选入口', async () => {
    const wrapper = mountHomePage();

    await flushPromises();

    expect(mockedListGroups).toHaveBeenCalledWith({ page: 1, size: 50 }, { userId: undefined });
    expect(mockedListProjects).toHaveBeenCalledWith(
      { page: 1, size: 20, groupId: undefined, keyword: undefined },
      { userId: undefined },
    );
    expect(wrapper.text()).toContain('小组筛选');
    expect(wrapper.text()).toContain('前端组');
    expect(wrapper.text()).toContain('后端组');
    expect(wrapper.text()).toContain('最近参与项目');
    expect(wrapper.text()).toContain('组件测试建设');
    expect(wrapper.text()).toContain('交付归档项目');
    expect(wrapper.text()).toContain('协作中');
    expect(wrapper.text()).toContain('已结束');
  });

  it('通过首页动作创建小组和项目', async () => {
    const wrapper = mountHomePage();

    await flushPromises();
    const viewModel = wrapper.vm as unknown as {
      groupForm: { name: string };
      projectForm: { groupId: number | null; name: string };
      submitGroup: () => Promise<void>;
      submitProject: () => Promise<void>;
    };
    viewModel.groupForm.name = '资料整理组';
    await viewModel.submitGroup();
    await flushPromises();

    expect(mockedCreateGroup).toHaveBeenCalledWith({ name: '资料整理组', userId: undefined });

    viewModel.projectForm.groupId = 1;
    viewModel.projectForm.name = '最终提交项目';
    await viewModel.submitProject();
    await flushPromises();

    expect(mockedCreateProject).toHaveBeenCalledWith({ groupId: 1, name: '最终提交项目', userId: undefined });
  });

  it('切换小组筛选后按小组重新加载项目', async () => {
    const wrapper = mountHomePage();

    await flushPromises();

    // 直接调用页面暴露给模板的处理函数，验证筛选条件会传入项目列表接口。
    const viewModel = wrapper.vm as unknown as {
      selectedGroupId: number;
      handleGroupChange: () => Promise<void>;
    };
    viewModel.selectedGroupId = 2;
    await viewModel.handleGroupChange();

    expect(mockedListProjects).toHaveBeenLastCalledWith(
      { page: 1, size: 20, groupId: 2, keyword: undefined },
      { userId: undefined },
    );
  });

  it('通过搜索入口展示项目、文件和成员命中结果', async () => {
    mockedSearchProjects.mockResolvedValue([{ projectId: 11, groupId: 1, projectName: '组件测试建设' }]);
    mockedSearchFiles.mockResolvedValue([{ fileId: 'f1', projectId: 11, fileName: '测试计划.md' }]);
    mockedSearchMembers.mockResolvedValue([{ userId: 7, displayName: '张三' }]);
    const wrapper = mountHomePage();

    await flushPromises();
    await wrapper.find('input[placeholder="搜索项目"]').setValue('组件');
    await wrapper.find('input[placeholder="搜索文件"]').setValue('计划');
    await wrapper.find('input[placeholder="搜索成员"]').setValue('张三');
    await wrapper.findAll('button').find((button) => button.text().includes('搜索'))?.trigger('click');
    await flushPromises();

    expect(mockedSearchProjects).toHaveBeenCalledWith({ keyword: '组件', userId: undefined });
    expect(mockedSearchFiles).toHaveBeenCalledWith({ keyword: '计划', userId: undefined });
    expect(mockedSearchMembers).toHaveBeenCalledWith({ keyword: '张三', userId: undefined });
    expect(wrapper.text()).toContain('搜索结果');
    expect(wrapper.text()).toContain('组件测试建设');
    expect(wrapper.text()).toContain('测试计划.md');
    expect(wrapper.text()).toContain('张三');
  });
});
