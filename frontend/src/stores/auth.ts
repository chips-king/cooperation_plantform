import { computed, ref } from 'vue';
import { defineStore } from 'pinia';

import type { CurrentUser, PermissionCode } from '@/types/project';

const ACCESS_TOKEN_KEY = 'access_token';
const CURRENT_USER_KEY = 'current_user';
const PERMISSIONS_KEY = 'permissions';

/**
 * 从本地存储读取 JSON 数据。
 *
 * @param key 本地存储键名
 * @param fallback 读取失败时使用的默认值
 * @returns 解析后的数据或默认值
 */
function readJson<T>(key: string, fallback: T): T {
  const rawValue = window.localStorage.getItem(key);
  if (!rawValue) {
    return fallback;
  }

  try {
    return JSON.parse(rawValue) as T;
  } catch {
    return fallback;
  }
}

/**
 * 当前用户鉴权状态 Store。
 *
 * @returns 鉴权状态、权限摘要和状态变更方法
 */
export const useAuthStore = defineStore('auth', () => {
  const currentUser = ref<CurrentUser | null>(readJson<CurrentUser | null>(CURRENT_USER_KEY, null));
  const accessToken = ref<string | null>(window.localStorage.getItem(ACCESS_TOKEN_KEY));
  const permissions = ref<PermissionCode[]>(readJson<PermissionCode[]>(PERMISSIONS_KEY, []));

  const isAuthenticated = computed(() => Boolean(accessToken.value && currentUser.value));
  const permissionSet = computed(() => new Set(permissions.value));

  /**
   * 写入当前登录用户与令牌。
   *
   * @param user 当前用户摘要
   * @param token 访问令牌
   * @param nextPermissions 当前用户权限摘要
   */
  function setSession(user: CurrentUser, token: string, nextPermissions: PermissionCode[] = []): void {
    currentUser.value = user;
    accessToken.value = token;
    permissions.value = nextPermissions;
    window.localStorage.setItem(ACCESS_TOKEN_KEY, token);
    window.localStorage.setItem(CURRENT_USER_KEY, JSON.stringify(user));
    window.localStorage.setItem(PERMISSIONS_KEY, JSON.stringify(nextPermissions));
  }

  /**
   * 更新当前用户权限摘要。
   *
   * @param nextPermissions 最新权限点列表
   */
  function setPermissions(nextPermissions: PermissionCode[]): void {
    permissions.value = nextPermissions;
    window.localStorage.setItem(PERMISSIONS_KEY, JSON.stringify(nextPermissions));
  }

  /**
   * 判断当前用户是否拥有指定权限。
   *
   * @param permission 待检查的权限点
   * @returns 拥有权限时返回 true
   */
  function hasPermission(permission: PermissionCode): boolean {
    return permissionSet.value.has(permission);
  }

  /**
   * 清空当前登录态。
   */
  function clearSession(): void {
    currentUser.value = null;
    accessToken.value = null;
    permissions.value = [];
    window.localStorage.removeItem(ACCESS_TOKEN_KEY);
    window.localStorage.removeItem(CURRENT_USER_KEY);
    window.localStorage.removeItem(PERMISSIONS_KEY);
  }

  return {
    currentUser,
    accessToken,
    permissions,
    isAuthenticated,
    setSession,
    setPermissions,
    hasPermission,
    clearSession,
  };
});
