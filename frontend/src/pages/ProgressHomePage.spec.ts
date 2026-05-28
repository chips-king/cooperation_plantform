import ElementPlus from 'element-plus';
import { flushPromises, mount } from '@vue/test-utils';
import { createPinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import ProgressHomePage from './ProgressHomePage.vue';
import {
  createGroup,
  createProject,
  listGroups,
  listProjects,
} from '@/services/groupProjectApi';
import { getProjectProgress } from '@/services/fileApi';

vi.mock('@/services/groupProjectApi', () => ({
  createGroup: vi.fn(),
  createProject: vi.fn(),
  listGroups: vi.fn(),
  listProjects: vi.fn(),
}));

vi.mock('@/services/fileApi', () => ({
  getProjectProgress: vi.fn(),
}));

vi.mock('vue-router', () => ({
  RouterLink: {
    props: ['to'],
    template: '<a :href="String(to)"><slot /></a>',
  },
  useRoute: () => ({ params: {}, name: 'home' }),
  useRouter: () => ({ push: vi.fn() }),
}));

const mockedCreateGroup = vi.mocked(createGroup);
const mockedCreateProject = vi.mocked(createProject);
const mockedListGroups = vi.mocked(listGroups);
const mockedListProjects = vi.mocked(listProjects);
const mockedGetProjectProgress = vi.mocked(getProjectProgress);

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

const progressDataMap: Record<string, { directories: Array<{ directoryId: string; name: string; fileCount: number; mailSent: boolean; updatedAt: string }> }> = {
  '11': {
    directories: [
      { directoryId: 'd1', name: '需求文档', fileCount: 5, mailSent: true, updatedAt: '2026-05-24T10:00:00' },
      { directoryId: 'd2', name: '接口设计', fileCount: 3, mailSent: false, updatedAt: '2026-05-24T10:00:00' },
      { directoryId: 'd3', name: '测试用例', fileCount: 0, mailSent: false, updatedAt: '2026-05-24T10:00:00' },
    ],
  },
  '12': {
    directories: [
      { directoryId: 'd4', name: '交付报告', fileCount: 2, mailSent: true, updatedAt: '2026-05-23T18:00:00' },
    ],
  },
};

function mountHomePage() {
  return mount(ProgressHomePage, {
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

describe('ProgressHomePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedListGroups.mockResolvedValue({ items: groups, total: groups.length, page: 1, size: 50 });
    mockedListProjects.mockResolvedValue({ items: projects, total: projects.length, page: 1, size: 20 });
    mockedCreateGroup.mockResolvedValue({ groupId: 3 });
    mockedCreateProject.mockResolvedValue({ projectId: 13, status: 'active', updatedAt: '2026-05-25T10:00:00' });
    mockedGetProjectProgress.mockImplementation((projectId: string) => {
      return Promise.resolve(progressDataMap[projectId] ?? { directories: [] });
    });
  });

  it('加载数据并展示三列看板', async () => {
    const wrapper = mountHomePage();
    await flushPromises();

    expect(mockedListGroups).toHaveBeenCalled();
    expect(mockedListProjects).toHaveBeenCalled();
    expect(wrapper.text()).toContain('未开始');
    expect(wrapper.text()).toContain('进行中');
    expect(wrapper.text()).toContain('已完成');
  });

  it('目录卡片显示项目名称', async () => {
    const wrapper = mountHomePage();
    await flushPromises();

    expect(wrapper.text()).toContain('组件测试建设');
    expect(wrapper.text()).toContain('交付归档项目');
  });

  it('按状态正确分类目录', async () => {
    const wrapper = mountHomePage();
    await flushPromises();

    // 未开始：fileCount=0 且 mailSent=false → 测试用例
    expect(wrapper.text()).toContain('测试用例');
    // 进行中：fileCount>0 且 mailSent=false → 接口设计
    expect(wrapper.text()).toContain('接口设计');
    // 已完成：mailSent=true → 需求文档、交付报告
    expect(wrapper.text()).toContain('需求文档');
    expect(wrapper.text()).toContain('交付报告');
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

  it('空项目时显示空态提示', async () => {
    mockedListProjects.mockResolvedValue({ items: [], total: 0, page: 1, size: 20 });
    const wrapper = mountHomePage();
    await flushPromises();

    expect(wrapper.text()).toContain('暂无目录数据');
  });
});
