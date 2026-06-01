import { request } from './http';

import type {
  ApproveJoinRequestResponse,
  Invitation,
  InvitationDetail,
  InvitationMode,
  JoinInvitationResponse,
  PermissionCode,
  ProjectPermissionResponse,
  RoleTemplate,
  UpdateMemberPermissionResponse,
} from '@/types/project';

/**
 * 当前用户请求选项。
 */
export interface CurrentUserRequestOptions {
  userId?: number | string;
}

/**
 * 创建邀请请求。
 */
export interface CreateInvitationRequest extends CurrentUserRequestOptions {
  groupId: number;
  projectId: number;
  mode: InvitationMode;
  roleTemplate: RoleTemplate;
}

/**
 * 加入邀请请求。
 */
export interface JoinInvitationRequest extends CurrentUserRequestOptions {
  code: string;
}

/**
 * 审核加入申请请求。
 */
export interface ApproveJoinRequest extends CurrentUserRequestOptions {
  requestId: number;
  roleTemplate: RoleTemplate;
}

/**
 * 拒绝加入申请请求。
 */
export interface RejectJoinRequest extends CurrentUserRequestOptions {
  requestId: number;
  reason: string;
}

/**
 * 更新成员权限请求。
 */
export interface UpdateMemberPermissionRequest extends CurrentUserRequestOptions {
  membershipId: number;
  permissions: PermissionCode[];
}

/**
 * 生成当前用户请求头。
 *
 * @param userId 当前用户标识
 * @returns 请求头对象
 */
function userHeaders(userId?: number | string): Record<string, string> | undefined {
  // 后端部分接口仍支持 X-User-Id，前端仅在调用方提供时透传。
  if (userId === undefined || userId === null || userId === '') {
    return undefined;
  }

  return { 'X-User-Id': String(userId) };
}

/**
 * 创建小组邀请链接。
 *
 * @param payload 邀请目标、模式、角色模板和当前用户标识
 * @returns 邀请链接信息
 */
export function createInvitation(payload: CreateInvitationRequest): Promise<Invitation> {
  return request<Invitation>({
    url: `/groups/${payload.groupId}/invitations`,
    method: 'POST',
    headers: userHeaders(payload.userId),
    data: {
      projectId: payload.projectId,
      mode: payload.mode,
      roleTemplate: payload.roleTemplate,
    },
  });
}

/**
 * 查询邀请详情。
 *
 * @param code 邀请码
 * @returns 邀请详情
 */
export function getInvitation(code: string): Promise<InvitationDetail> {
  return request<InvitationDetail>({
    url: `/invitations/${code}`,
    method: 'GET',
  });
}

/**
 * 通过邀请码加入项目，支持直接加入和待审核两种后端结果。
 *
 * @param payload 邀请码和当前用户标识
 * @returns 加入处理结果
 */
export function joinInvitation(payload: JoinInvitationRequest): Promise<JoinInvitationResponse> {
  return request<JoinInvitationResponse>({
    url: `/invitations/${payload.code}/join`,
    method: 'POST',
    headers: userHeaders(payload.userId),
    data: { userId: payload.userId },
  });
}

/**
 * 审核通过加入申请。
 *
 * @param payload 申请标识、角色模板和当前用户标识
 * @returns 审核通过结果
 */
export function approveJoinRequest(payload: ApproveJoinRequest): Promise<ApproveJoinRequestResponse> {
  return request<ApproveJoinRequestResponse>({
    url: `/join-requests/${payload.requestId}/approve`,
    method: 'POST',
    headers: userHeaders(payload.userId),
    data: {
      operatorId: payload.userId,
      roleTemplate: payload.roleTemplate,
    },
  });
}

/**
 * 拒绝加入申请。
 *
 * @param payload 申请标识、拒绝原因和当前用户标识
 * @returns 审核拒绝结果
 */
export function rejectJoinRequest(payload: RejectJoinRequest): Promise<JoinInvitationResponse> {
  return request<JoinInvitationResponse>({
    url: `/join-requests/${payload.requestId}/reject`,
    method: 'POST',
    headers: userHeaders(payload.userId),
    data: {
      operatorId: payload.userId,
      reason: payload.reason,
    },
  });
}

/**
 * 移除项目成员。
 *
 * @param membershipId 成员关系标识
 */
export function removeMember(membershipId: number): Promise<void> {
  return request<void>({
    url: `/memberships/${membershipId}`,
    method: 'DELETE',
  });
}

/**
 * 查询项目成员权限列表。
 *
 * @param projectId 项目标识
 * @param options 当前用户请求选项
 * @returns 项目权限列表
 */
export function getProjectPermissions(
  projectId: number,
  options: CurrentUserRequestOptions = {},
): Promise<ProjectPermissionResponse> {
  return request<ProjectPermissionResponse>({
    url: `/projects/${projectId}/permissions`,
    method: 'GET',
    headers: userHeaders(options.userId),
  });
}

/**
 * 更新成员权限。
 *
 * @param payload 成员关系标识、新权限集合和当前用户标识
 * @returns 更新后的成员权限
 */
export function updateMemberPermissions(
  payload: UpdateMemberPermissionRequest,
): Promise<UpdateMemberPermissionResponse> {
  return request<UpdateMemberPermissionResponse>({
    url: `/memberships/${payload.membershipId}/permissions`,
    method: 'PATCH',
    headers: userHeaders(payload.userId),
    data: {
      operatorId: payload.userId,
      permissions: payload.permissions,
    },
  });
}
