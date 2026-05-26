import ElementPlus from 'element-plus';
import { flushPromises, mount } from '@vue/test-utils';
import { createPinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import ProjectWorkspacePage from './ProjectWorkspacePage.vue';
import { request } from '@/services/http';
import { getProject } from '@/services/groupProjectApi';
import { listOperationLogs } from '@/services/activityApi';
import { getLatestPackage } from '@/services/packageApi';
import { createDirectory } from '@/services/fileApi';

const routerPush = vi.fn();

vi.mock('vue-router', () => ({
  RouterLink: {
    props: ['to'],
    template: '<a :href="String(to)"><slot /></a>',
  },
  useRoute: () => ({ params: { projectId: '1' } }),
  useRouter: () => ({ push: routerPush }),
}));

vi.mock('@/services/http', () => ({ request: vi.fn() }));
vi.mock('@/services/groupProjectApi', () => ({ getProject: vi.fn() }));
vi.mock('@/services/activityApi', () => ({ listOperationLogs: vi.fn() }));
vi.mock('@/services/packageApi', () => ({ getLatestPackage: vi.fn() }));
vi.mock('@/services/fileApi', () => ({ createDirectory: vi.fn() }));

const mockedRequest = vi.mocked(request);
const mockedGetProject = vi.mocked(getProject);
const mockedListOperationLogs = vi.mocked(listOperationLogs);
const mockedGetLatestPackage = vi.mocked(getLatestPackage);
const mockedCreateDirectory = vi.mocked(createDirectory);

function mountProjectWorkspacePage() {
  return mount(ProjectWorkspacePage, {
    attachTo: document.body,
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

describe('ProjectWorkspacePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    document.body.innerHTML = '';
    routerPush.mockResolvedValue(undefined);
    mockedGetProject.mockResolvedValue({
      id: 1,
      groupId: 1,
      name: '企业开发',
      ownerId: 1,
      status: 'active',
      endedAt: null,
      reopenedAt: null,
    });
    mockedListOperationLogs.mockResolvedValue({ logs: [] });
    mockedGetLatestPackage.mockRejectedValue(new Error('没有压缩包'));
    mockedRequest.mockResolvedValue({
      projectId: '1',
      totalDirectoryCount: 1,
      completedDirectoryCount: 0,
      directories: [{
        directoryId: 'dir-1',
        name: '默认分工目录',
        status: 'in_progress',
        statusDisplayName: '进行中',
        updatedAt: '2026-05-26T09:56:18',
      }],
    });
    mockedCreateDirectory.mockResolvedValue({
      id: 'dir-2',
      parentId: 'dir-1',
      name: '任务一',
      status: 'in_progress',
      files: [],
      children: [],
    });
  });

  it('只有默认目录时提示创建分工目录', async () => {
    const wrapper = mountProjectWorkspacePage();

    await flushPromises();

    expect(wrapper.text()).toContain('建议先创建分工目录');
    expect(wrapper.text()).toContain('创建分工目录');
  });

  it('点击目录进度行后进入文件管理并定位目录', async () => {
    const wrapper = mountProjectWorkspacePage();

    await flushPromises();
    await wrapper.find('tbody tr').trigger('click');

    expect(routerPush).toHaveBeenCalledWith({
      name: 'project-files',
      params: { projectId: '1' },
      query: { directoryId: 'dir-1' },
    });
  });

  it('创建分工目录后进入新目录文件管理页', async () => {
    vi.spyOn(window, 'prompt').mockReturnValue('任务一');
    const wrapper = mountProjectWorkspacePage();

    await flushPromises();
    await wrapper.findAll('button').find((button) => button.text().includes('创建分工目录'))?.trigger('click');
    await flushPromises();

    expect(mockedCreateDirectory).toHaveBeenCalledWith({
      projectId: '1',
      parentDirectoryId: 'dir-1',
      name: '任务一',
    });
    expect(routerPush).toHaveBeenCalledWith({
      name: 'project-files',
      params: { projectId: '1' },
      query: { directoryId: 'dir-2' },
    });
  });
});
