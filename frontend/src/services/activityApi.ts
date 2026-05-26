import { request } from './http';

import type { Notification, NotificationList, OperationLogList } from '@/types/project';

/**
 * 当前用户请求选项。
 */
export interface CurrentUserRequestOptions {
  userId?: number | string;
}

/**
 * 操作记录查询参数。
 */
export interface OperationLogQuery extends CurrentUserRequestOptions {
  action?: string;
  actorId?: number;
  from?: string;
  to?: string;
}

/**
 * 通知查询参数。
 */
export interface NotificationQuery extends CurrentUserRequestOptions {
  projectId?: string;
  read?: boolean;
}

/**
 * 生成当前用户请求头。
 *
 * @param userId 当前用户标识
 * @returns 请求头对象
 */
function userHeaders(userId?: number | string): Record<string, string> | undefined {
  // 记录和通知接口兼容测试期 X-User-Id，缺省时由后端认证上下文接管。
  if (userId === undefined || userId === null || userId === '') {
    return undefined;
  }

  return { 'X-User-Id': String(userId) };
}

/**
 * 查询项目操作记录。
 *
 * @param projectId 项目标识
 * @param query 操作类型、操作人和时间筛选
 * @returns 操作记录列表
 */
export function listOperationLogs(projectId: string, query: OperationLogQuery = {}): Promise<OperationLogList> {
  return request<OperationLogList>({
    url: `/projects/${projectId}/operation-logs`,
    method: 'GET',
    headers: userHeaders(query.userId),
    params: {
      action: query.action,
      actorId: query.actorId,
      from: query.from,
      to: query.to,
    },
  });
}

/**
 * 查询当前用户通知列表。
 *
 * @param query 项目和已读状态筛选
 * @returns 通知列表
 */
export function listNotifications(query: NotificationQuery = {}): Promise<NotificationList> {
  return request<NotificationList>({
    url: '/notifications',
    method: 'GET',
    headers: userHeaders(query.userId),
    params: {
      projectId: query.projectId,
      read: query.read,
    },
  });
}

/**
 * 标记通知为已读。
 *
 * @param notificationId 通知标识
 * @param options 当前用户请求选项
 * @returns 更新后的通知详情
 */
export function markNotificationRead(
  notificationId: string,
  options: CurrentUserRequestOptions = {},
): Promise<Notification> {
  return request<Notification>({
    url: `/notifications/${notificationId}/read`,
    method: 'POST',
    headers: userHeaders(options.userId),
  });
}
