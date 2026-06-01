import { request } from '@/services/http';

/**
 * 用户资料响应结构。
 */
export interface UserProfileResponse {
  id: number;
  displayName: string;
  email: string;
  status: string;
}

/**
 * 更新用户资料请求参数。
 */
export interface UpdateProfileParams {
  displayName: string;
  email: string;
}

/**
 * 获取当前登录用户的资料。
 *
 * @returns 用户资料信息
 */
export async function getCurrentUserProfile(): Promise<UserProfileResponse> {
  return request<UserProfileResponse>({
    url: '/users/me',
    method: 'GET',
  });
}

/**
 * 更新当前登录用户的资料。
 *
 * @param params 要更新的字段（展示名称、邮箱）
 * @returns 更新后的用户资料
 */
export async function updateCurrentUserProfile(params: UpdateProfileParams): Promise<UserProfileResponse> {
  return request<UserProfileResponse>({
    url: '/users/me',
    method: 'PUT',
    data: params,
  });
}

/**
 * 修改当前登录用户的密码。
 *
 * @param currentPassword 当前密码
 * @param newPassword 新密码
 */
export async function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  await request<void>({
    url: '/users/me/password',
    method: 'PUT',
    data: { currentPassword, newPassword },
  });
}
