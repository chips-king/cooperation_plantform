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
 * 安装邮件草稿流程所需接口 mock。
 *
 * @param page Playwright 页面对象
 */
async function mockMailApis(page: Page): Promise<void> {
  await page.route('**/api/**', async (route: Route) => {
    const request = route.request();
    const url = new URL(request.url());
    const path = url.pathname.replace('/api', '');
    const method = request.method();

    if (path === '/projects/1/packages/latest') {
      await route.fulfill({
        json: ok({ packageId: 'pkg-1', filename: '课程成果.zip', format: 'zip', snapshotCreatedAt: '2026-05-25 10:00:00', size: 4096 }),
      });
      return;
    }

    if (path === '/projects/1/mail-drafts' && method === 'POST') {
      await route.fulfill({
        json: ok({ draftId: 'draft-1', projectId: '1', recipients: ['teacher@example.com'], subject: '课程成果', body: '请查收', packageId: 'pkg-1', attachmentFilename: '课程成果.zip', status: 'draft', createdAt: '2026-05-25 10:10:00', sentAt: null }),
      });
      return;
    }

    if (path === '/mail-drafts/draft-1' && method === 'PATCH') {
      await route.fulfill({
        json: ok({ draftId: 'draft-1', projectId: '1', recipients: ['teacher@example.com'], subject: '课程成果已核实', body: '请查收最终版本', packageId: 'pkg-1', attachmentFilename: '课程成果.zip', status: 'draft', createdAt: '2026-05-25 10:10:00', sentAt: null }),
      });
      return;
    }

    if (path === '/mail-drafts/draft-1/send' && method === 'POST') {
      await route.fulfill({
        json: ok({ draftId: 'draft-1', projectId: '1', recipients: ['teacher@example.com'], subject: '课程成果已核实', body: '请查收最终版本', packageId: 'pkg-1', attachmentFilename: '课程成果.zip', status: 'sent', createdAt: '2026-05-25 10:10:00', sentAt: '2026-05-25 10:12:00', message: '邮件已发送' }),
      });
      return;
    }

    await route.fulfill({ json: ok(null) });
  });
}

test('邮件草稿流程可生成、修改并确认发送', async ({ page }) => {
  await mockMailApis(page);
  await page.addInitScript(() => window.localStorage.setItem('access_token', 'e2e-token'));

  await page.goto('/projects/1/mail');
  await expect(page.getByText('推荐使用 .zip')).toBeVisible();
  await page.getByPlaceholder('多个收件人可用逗号或换行分隔').fill('teacher@example.com');
  await page.getByLabel('主题').fill('课程成果');
  await page.getByLabel('正文').fill('请查收');
  await page.getByRole('button', { name: '生成草稿' }).click();
  await expect(page.getByText('草稿已生成')).toBeVisible();

  await page.getByLabel('主题').fill('课程成果已核实');
  await page.getByLabel('正文').fill('请查收最终版本');
  await page.getByRole('button', { name: '保存修改' }).click();
  await expect(page.getByText('草稿已保存')).toBeVisible();

  await page.getByRole('button', { name: '确认发送' }).click();
  await page.getByRole('button', { name: '确认发送' }).last().click();
  await expect(page.getByText('邮件已发送')).toBeVisible();
});
