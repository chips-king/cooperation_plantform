import { expect, test, type Page, type Route } from '@playwright/test';

/**
 * 返回后端统一成功响应。
 *
 * @param data 业务数据
 * @returns 统一响应结构
 */
function ok(data: unknown): Record<string, unknown> {
  return { success: true, code: null, message: 'ok', data, fieldErrors: null };
}

/**
 * 安装协作主流程所需接口 mock。
 *
 * @param page Playwright 页面对象
 */
async function mockCollaborationApis(page: Page): Promise<void> {
  await page.route('**/api/**', async (route: Route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname.replace('/api', '');
    const method = request.method();

    // 使用路径和方法分发 mock，覆盖上传、记录、检查和打包主流程。
    if (path === '/projects/1' && method === 'GET') {
      await route.fulfill({
        json: ok({
          id: 1,
          name: '协作项目',
          groupId: 1,
          ownerId: 7,
          status: 'IN_PROGRESS',
          description: 'e2e 测试项目',
          createdAt: '2026-05-01 09:00:00',
          updatedAt: '2026-05-25 10:00:00',
        }),
      });
      return;
    }

    if (path === '/projects/1/tree') {
      await route.fulfill({
        json: ok({
          projectId: '1',
          directories: [
            {
              id: 'dir-1',
              parentId: null,
              name: '源文件',
              status: 'in_progress',
              files: [{ fileId: 'file-1', name: 'main.java', size: 1280, mimeType: 'text/plain', versionNo: 1, status: 'active', uploadedAt: '2026-05-25T10:00:00' }],
              children: [],
            },
          ],
        }),
      });
      return;
    }

    if (path === '/directories/dir-1/files' && method === 'POST') {
      await route.fulfill({
        json: ok({ fileId: 'file-2', name: 'README.md', size: 20, mimeType: 'text/markdown', versionNo: 1, status: 'active', duplicatePolicy: null, archive: false }),
      });
      return;
    }

    if (path === '/projects/1/operation-logs') {
      await route.fulfill({
        json: ok({ logs: [{ id: 'log-1', projectId: '1', actorId: 7, action: 'FILE_UPLOAD', targetType: 'file', targetId: 'file-2', summary: '上传 README.md', createdAt: '2026-05-25 10:00:00' }] }),
      });
      return;
    }

    if (path === '/projects/1/checks' && method === 'POST') {
      await route.fulfill({
        json: ok({ canContinuePackaging: true, issues: [], cleanupSuggestions: [] }),
      });
      return;
    }

    if (path === '/projects/1/packages/latest' && method === 'GET') {
      await route.fulfill({ status: 404, json: { success: false, message: '暂无压缩包', errorCode: 'NOT_FOUND' } });
      return;
    }

    if (path === '/projects/1/packages' && method === 'POST') {
      await route.fulfill({
        json: ok({ packageId: 'pkg-1', filename: '课程成果.zip', format: 'zip', snapshotCreatedAt: '2026-05-25 10:05:00', size: 4096 }),
      });
      return;
    }

    await route.fulfill({ json: ok(null) });
  });
}

test('协作主流程可上传文件、查看记录、检查并生成压缩包', async ({ page }) => {
  await mockCollaborationApis(page);
  await page.addInitScript(() => window.localStorage.setItem('access_token', 'e2e-token'));

  await page.goto('/projects/1');
  await expect(page.getByText('main.java')).toBeVisible();
  await page.locator('input[type="file"]').setInputFiles({
    name: 'README.md',
    mimeType: 'text/markdown',
    buffer: Buffer.from('# readme'),
  });
  await expect(page.getByText('文件上传完成')).toBeVisible();

  await page.goto('/projects/1/logs');
  await page.getByRole('button', { name: '查询记录' }).click();
  await expect(page.getByText('上传 README.md')).toBeVisible();

  await page.goto('/projects/1/package/check');
  await page.getByRole('button', { name: '执行检查' }).click();
  await expect(page.getByText('允许继续打包')).toBeVisible();

  await page.goto('/projects/1/package/export');
  await page.getByPlaceholder('例如：课程项目成果').fill('课程成果');
  await page.getByRole('button', { name: '生成压缩包' }).click();
  await expect(page.getByRole('main').getByText('课程成果.zip')).toBeVisible();
});
