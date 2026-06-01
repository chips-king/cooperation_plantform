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
import type { ProjectProgress } from '@/types/project';

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

const progressDataMap: Record<string, ProjectProgress> = {
  '11': {
    projectId: '11',
    totalDirectoryCount: 3,
    completedDirectoryCount: 1,
    directories: [
      { directoryId: 'd1', name: '需求文档', status: 'completed', statusDisplayName: '已完成', fileCount: 5, mailSent: true, updatedAt: '2026-05-24T10:00:00' },
      { directoryId: 'd2', name: '接口设计', status: 'in_progress', statusDisplayName: '进行中', fileCount: 3, mailSent: false, updatedAt: '2026-05-24T10:00:00' },
      { directoryId: 'd3', name: '测试用例', status: 'not_started', statusDisplayName: '未开始', fileCount: 0, mailSent: false, updatedAt: '2026-05-24T10:00:00' },
    ],
  },
  '12': {
    projectId: '12',
    totalDirectoryCount: 1,
    completedDirectoryCount: 1,
    directories: [
      { directoryId: 'd4', name: '交付报告', status: 'completed', statusDisplayName: '已完成', fileCount: 2, mailSent: true, updatedAt: '2026-05-23T18:00:00' },
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
      return Promise.resolve(progressDataMap[projectId] ?? {
        projectId,
        totalDirectoryCount: 0,
        completedDirectoryCount: 0,
        directories: [],
      });
    });
  });

  it('加载数据并展示项目总览', async () => {
    const wrapper = mountHomePage();
    await flushPromises();

    expect(mockedListGroups).toHaveBeenCalled();
    expect(mockedListProjects).toHaveBeenCalled();
    expect(wrapper.text()).toContain('项目总览');
    expect(wrapper.text()).toContain('组件测试建设');
    expect(wrapper.text()).toContain('交付归档项目');
  });

  it('项目卡片显示小组和项目状态', async () => {
    const wrapper = mountHomePage();
    await flushPromises();

    expect(wrapper.text()).toContain('前端组');
    expect(wrapper.text()).toContain('后端组');
    expect(wrapper.text()).toContain('协作中');
    expect(wrapper.text()).toContain('已结束');
  });

  it('根据项目进度展示项目统计', async () => {
    const wrapper = mountHomePage();
    await flushPromises();

    expect(wrapper.text()).toContain('3 个目录');
    expect(wrapper.text()).toContain('8 个文件');
    expect(wrapper.text()).toContain('1 个目录');
    expect(wrapper.text()).toContain('2 个文件');
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

    expect(wrapper.text()).toContain('暂无项目，请先创建小组和项目');
  });
});
