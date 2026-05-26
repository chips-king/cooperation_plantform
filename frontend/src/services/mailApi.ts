import { request } from './http';

import type { MailDraft, SendMailDraftResponse } from '@/types/project';

/**
 * 当前用户请求选项。
 */
export interface CurrentUserRequestOptions {
  userId?: number | string;
}

/**
 * 创建邮件草稿请求。
 */
export interface CreateMailDraftRequest extends CurrentUserRequestOptions {
  projectId: string;
  recipients: string[];
  subject: string;
  body: string;
}

/**
 * 更新邮件草稿请求。
 */
export interface UpdateMailDraftRequest extends CurrentUserRequestOptions {
  draftId: string;
  recipients: string[];
  subject: string;
  body: string;
  packageId: string;
}

/**
 * 发送邮件草稿请求。
 */
export interface SendMailDraftRequest extends CurrentUserRequestOptions {
  draftId: string;
  confirmed: boolean;
}

/**
 * 生成当前用户请求头。
 *
 * @param userId 当前用户标识
 * @returns 请求头对象
 */
function userHeaders(userId?: number | string): Record<string, string> | undefined {
  // 邮件草稿接口当前通过 X-User-Id 识别操作者，仅在调用方提供时发送。
  if (userId === undefined || userId === null || userId === '') {
    return undefined;
  }

  return { 'X-User-Id': String(userId) };
}

/**
 * 基于项目最近压缩包创建邮件草稿。
 *
 * @param payload 收件人、主题、正文和当前用户标识
 * @returns 邮件草稿详情
 */
export function createMailDraft(payload: CreateMailDraftRequest): Promise<MailDraft> {
  return request<MailDraft>({
    url: `/projects/${payload.projectId}/mail-drafts`,
    method: 'POST',
    headers: userHeaders(payload.userId),
    data: {
      recipients: payload.recipients,
      subject: payload.subject,
      body: payload.body,
    },
  });
}

/**
 * 查询邮件草稿详情。
 *
 * @param draftId 草稿标识
 * @param options 当前用户请求选项
 * @returns 邮件草稿详情
 */
export function getMailDraft(draftId: string, options: CurrentUserRequestOptions = {}): Promise<MailDraft> {
  return request<MailDraft>({
    url: `/mail-drafts/${draftId}`,
    method: 'GET',
    headers: userHeaders(options.userId),
  });
}

/**
 * 更新邮件草稿。
 *
 * @param payload 草稿标识、草稿字段和当前用户标识
 * @returns 更新后的邮件草稿详情
 */
export function updateMailDraft(payload: UpdateMailDraftRequest): Promise<MailDraft> {
  return request<MailDraft>({
    url: `/mail-drafts/${payload.draftId}`,
    method: 'PATCH',
    headers: userHeaders(payload.userId),
    data: {
      recipients: payload.recipients,
      subject: payload.subject,
      body: payload.body,
      packageId: payload.packageId,
    },
  });
}

/**
 * 确认并发送邮件草稿。
 *
 * @param payload 草稿标识、确认标识和当前用户标识
 * @returns 发送后的草稿详情和提示
 */
export function sendMailDraft(payload: SendMailDraftRequest): Promise<SendMailDraftResponse> {
  return request<SendMailDraftResponse>({
    url: `/mail-drafts/${payload.draftId}/send`,
    method: 'POST',
    headers: userHeaders(payload.userId),
    data: { confirmed: payload.confirmed },
  });
}
