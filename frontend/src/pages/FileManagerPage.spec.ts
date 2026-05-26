import ElementPlus from 'element-plus';
import { flushPromises, mount } from '@vue/test-utils';
import { createPinia } from 'pinia';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import FileManagerPage from './FileManagerPage.vue';
import { createDirectory, deleteDirectory, getDirectoryTree, uploadFile } from '@/services/fileApi';
import type { Directory } from '@/types/project';

const routerPush = vi.fn();
let routeQuery: Record<string, string> = {};

vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { projectId: 'p1' }, query: routeQuery }),
  useRouter: () => ({ push: routerPush }),
}));

vi.mock('@/services/fileApi', () => ({
  createDirectory: vi.fn(),
  deleteFile: vi.fn(),
  deleteDirectory: vi.fn(),
  downloadFile: vi.fn(),
  getDirectoryTree: vi.fn(),
  listTrashFiles: vi.fn(),
  moveFile: vi.fn(),
  restoreFile: vi.fn(),
  uploadFile: vi.fn(),
}));

const mockedGetDirectoryTree = vi.mocked(getDirectoryTree);
const mockedUploadFile = vi.mocked(uploadFile);
const mockedCreateDirectory = vi.mocked(createDirectory);
const mockedDeleteDirectory = vi.mocked(deleteDirectory);

const directories: Directory[] = [
  {
    id: 'dir-1',
    parentId: null,
    name: '需求资料',
    status: 'in_progress',
    files: [
      {
        fileId: 'file-1',
        name: '需求说明.md',
        size: 2048,
        mimeType: 'text/markdown',
        versionNo: 2,
        status: 'active',
      },
    ],
    children: [
      {
        id: 'dir-2',
        parentId: 'dir-1',
        name: '归档',
        status: 'not_started',
        files: [],
        children: [],
      },
    ],
  },
];

/**
 * 挂载文件管理页面并注入必要插件。
 *
 * @returns 文件管理页面测试包装器
 */
function mountFileManagerPage() {
  return mount(FileManagerPage, {
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

describe('FileManagerPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    document.body.innerHTML = '';
    routeQuery = {};
    routerPush.mockResolvedValue(undefined);
    mockedGetDirectoryTree.mockResolvedValue({ projectId: 'p1', directories });
    mockedCreateDirectory.mockResolvedValue({
      id: 'dir-3',
      parentId: 'dir-2',
      name: '任务三',
      status: 'in_progress',
      files: [],
      children: [],
    });
    mockedDeleteDirectory.mockResolvedValue({ parentDirectoryId: 'dir-1' });
    mockedUploadFile.mockResolvedValue({
      fileId: 'uploaded-1',
      name: 'App.vue',
      size: 20,
      mimeType: 'text/x-vue',
      duplicatePolicy: 'new_version',
      versionNo: 1,
      status: 'active',
      archive: false,
    });
  });

  it('展示当前目录文件列表的基本信息', async () => {
    const wrapper = mountFileManagerPage();

    await flushPromises();

    expect(mockedGetDirectoryTree).toHaveBeenCalledWith('p1');
    expect(wrapper.text()).toContain('目录树');
    expect(wrapper.text()).toContain('需求资料');
    expect(wrapper.text()).toContain('归档');
    expect(wrapper.text()).toContain('需求说明.md');
    expect(wrapper.text()).toContain('2.0 KB');
    expect(wrapper.text()).toContain('text/markdown');
    expect(wrapper.text()).toContain('v2');
    expect(wrapper.text()).toContain('正常');
  });

  it('点击返回上一级箭头后选中父目录并同步路由', async () => {
    routeQuery = { directoryId: 'dir-2' };
    const wrapper = mountFileManagerPage();

    await flushPromises();
    await wrapper.find('[aria-label="返回上一级目录"]').trigger('click');
    await flushPromises();

    expect((wrapper.vm as unknown as { selectedDirectoryId: string }).selectedDirectoryId).toBe('dir-1');
    expect(routerPush).toHaveBeenCalledWith({
      name: 'project-files',
      params: { projectId: 'p1' },
      query: { directoryId: 'dir-1' },
    });
  });

  it('新建目录时在当前目录下创建分工目录并刷新定位', async () => {
    routeQuery = { directoryId: 'dir-2' };
    vi.spyOn(window, 'prompt').mockReturnValue('任务三');
    const wrapper = mountFileManagerPage();

    await flushPromises();
    await wrapper.findAll('button').find((button) => button.text().includes('新建目录'))?.trigger('click');
    await flushPromises();

    expect(mockedCreateDirectory).toHaveBeenCalledWith({
      projectId: 'p1',
      parentDirectoryId: 'dir-2',
      name: '任务三',
    });
    expect(routerPush).toHaveBeenCalledWith({
      name: 'project-files',
      params: { projectId: 'p1' },
      query: { directoryId: 'dir-3' },
    });
  });

  it('删除当前空目录后刷新目录树并回到父目录', async () => {
    routeQuery = { directoryId: 'dir-2' };
    const wrapper = mountFileManagerPage();

    await flushPromises();
    await wrapper.findAll('button').find((button) => button.text().includes('删除目录'))?.trigger('click');
    await flushPromises();

    expect(mockedDeleteDirectory).toHaveBeenCalledWith({ projectId: 'p1', directoryId: 'dir-2' });
    expect(routerPush).toHaveBeenCalledWith({
      name: 'project-files',
      params: { projectId: 'p1' },
      query: { directoryId: 'dir-1' },
    });
  });

  it('选择同名文件时打开同名文件处理弹窗', async () => {
    const wrapper = mountFileManagerPage();

    await flushPromises();

    // 直接模拟 Element Plus 上传组件回调，避免测试依赖浏览器文件选择器。
    const viewModel = wrapper.vm as unknown as {
      handleUploadPick: (uploadFile: { raw: File; name: string }) => void;
      duplicateDialogVisible: boolean;
      duplicateFileName: string;
    };
    const sameNameFile = new File(['updated'], '需求说明.md', { type: 'text/markdown' });
    viewModel.handleUploadPick({ raw: sameNameFile, name: sameNameFile.name });
    await flushPromises();

    expect(viewModel.duplicateDialogVisible).toBe(true);
    expect(viewModel.duplicateFileName).toBe('需求说明.md');
    expect(document.body.textContent).toContain('同名文件处理');
    expect(document.body.textContent).toContain('覆盖原文件');
    expect(document.body.textContent).toContain('重命名上传');
    expect(document.body.textContent).toContain('保留新版本');
  });

  it('点击回收站入口后打开回收站抽屉', async () => {
    const wrapper = mountFileManagerPage();

    await flushPromises();
    await wrapper.findAll('button').find((button) => button.text().includes('回收站'))?.trigger('click');
    await flushPromises();

    expect((wrapper.vm as unknown as { trashVisible: boolean }).trashVisible).toBe(true);
    expect(document.body.textContent).toContain('回收站');
  });

  it('拖拽文件夹时按相对路径批量上传文件', async () => {
    const wrapper = mountFileManagerPage();

    await flushPromises();

    const viewModel = wrapper.vm as unknown as {
      handleFolderDrop: (event: DragEvent) => Promise<void>;
    };
    const appFile = new File(['vue'], 'App.vue', { type: 'text/x-vue' });
    Object.defineProperty(appFile, 'webkitRelativePath', { value: 'src/views/App.vue' });
    const readmeFile = new File(['readme'], 'README.md', { type: 'text/markdown' });
    Object.defineProperty(readmeFile, 'webkitRelativePath', { value: 'README.md' });
    const event = {
      preventDefault: vi.fn(),
      dataTransfer: {
        files: [appFile, readmeFile],
        items: [],
      },
    } as unknown as DragEvent;

    await viewModel.handleFolderDrop(event);
    await flushPromises();

    expect(mockedUploadFile).toHaveBeenCalledWith({
      projectId: 'p1',
      directoryId: 'dir-1',
      file: appFile,
      relativePath: 'src/views/App.vue',
      duplicatePolicy: 'new_version',
    });
    expect(mockedUploadFile).toHaveBeenCalledWith({
      projectId: 'p1',
      directoryId: 'dir-1',
      file: readmeFile,
      relativePath: 'README.md',
      duplicatePolicy: 'new_version',
    });
  });
});
