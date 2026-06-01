import ElementPlus from 'element-plus';
import { flushPromises, mount } from '@vue/test-utils';
import { createPinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import ProjectWorkspacePage from './ProjectWorkspacePage.vue';
import { request } from '@/services/http';
import { getProject } from '@/services/groupProjectApi';
import { listOperationLogs } from '@/services/activityApi';
import { getLatestPackage } from '@/services/packageApi';
import { createDirectory, getDirectoryTree, uploadFile } from '@/services/fileApi';

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
vi.mock('@/services/fileApi', () => ({ createDirectory: vi.fn(), getDirectoryTree: vi.fn(), uploadFile: vi.fn() }));

const mockedRequest = vi.mocked(request);
const mockedGetProject = vi.mocked(getProject);
const mockedListOperationLogs = vi.mocked(listOperationLogs);
const mockedGetLatestPackage = vi.mocked(getLatestPackage);
const mockedUploadFile = vi.mocked(uploadFile);
const mockedCreateDirectory = vi.mocked(createDirectory);
const mockedGetDirectoryTree = vi.mocked(getDirectoryTree);

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
        PackageCommandDialog: {
          template: '<div />',
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
    // 项目无目录时，目录树返回空列表（不再自动创建默认分工目录）
    mockedGetDirectoryTree.mockResolvedValue({
      projectId: '1',
      directories: [],
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

  it('项目为空时显示空状态提示', async () => {
    const wrapper = mountProjectWorkspacePage();

    await flushPromises();

    expect(wrapper.text()).toContain('此项目还没有文件');
  });

  it('不再展示任务进度快捷入口', async () => {
    const wrapper = mountProjectWorkspacePage();

    await flushPromises();

    expect(wrapper.text()).not.toContain('任务进度');
    expect(wrapper.find('a[href="/projects/1/progress"]').exists()).toBe(false);
  });

  it('项目有目录时展示目录列表', async () => {
    // 模拟有根目录和子目录的项目
    mockedGetDirectoryTree.mockResolvedValue({
      projectId: '1',
      directories: [{
        id: 'dir-1',
        parentId: null,
        name: 'root',
        status: 'in_progress',
        files: [],
        children: [{
          id: 'dir-2',
          parentId: 'dir-1',
          name: '任务一',
          status: 'in_progress',
          files: [],
          children: [],
        }],
      }],
    });
    const wrapper = mountProjectWorkspacePage();

    await flushPromises();

    expect(wrapper.text()).toContain('任务一');
    expect(wrapper.text()).not.toContain('此项目还没有文件');
  });

  it('上传文件时自动创建根目录，文件直接落入根目录', async () => {
    // 创建根目录返回根目录
    mockedCreateDirectory.mockResolvedValueOnce({
      id: 'dir-root',
      parentId: null,
      name: 'root',
      status: 'in_progress',
      files: [],
      children: [],
    });
    mockedUploadFile.mockResolvedValue({
      fileId: 'file-1',
      name: 'test.txt',
      size: 100,
      mimeType: 'text/plain',
      duplicatePolicy: null,
      versionNo: 1,
      status: 'active',
      archive: false,
      uploadedAt: '2026-05-27T10:00:00',
    });

    const wrapper = mountProjectWorkspacePage();
    await flushPromises();

    // 触发上传文件事件（模拟 FileListBlock 发出）
    const fileListBlock = wrapper.findComponent({ name: 'FileListBlock' });
    await fileListBlock.vm.$emit('upload-files', [new File(['hello'], 'test.txt', { type: 'text/plain' })]);
    await flushPromises();

    // 验证只创建了隐藏根目录（不创建额外子目录）
    expect(mockedCreateDirectory).toHaveBeenCalledTimes(1);
    expect(mockedCreateDirectory).toHaveBeenNthCalledWith(1, {
      projectId: '1',
      parentDirectoryId: '0',
      name: 'root',
    });
    // 验证文件直接上传到根目录
    expect(mockedUploadFile).toHaveBeenCalledTimes(1);
    expect(mockedUploadFile).toHaveBeenCalledWith(expect.objectContaining({
      projectId: '1',
      directoryId: 'dir-root',
    }));
  });
});
