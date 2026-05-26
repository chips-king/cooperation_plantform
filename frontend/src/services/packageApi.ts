import { request } from './http';

import type {
  CheckIssueType,
  CheckReport,
  CleanupApplyResponse,
  CleanupPreview,
  PackageArtifact,
  PackageFormat,
} from '@/types/project';

/**
 * 当前用户请求选项。
 */
export interface CurrentUserRequestOptions {
  userId?: number | string;
}

/**
 * 清理建议请求项。
 */
export interface CleanupItemRequest {
  path: string;
  reason?: string;
  type?: CheckIssueType;
}

/**
 * 清理建议请求。
 */
export interface CleanupRequest extends CurrentUserRequestOptions {
  projectId: string;
  items: CleanupItemRequest[];
}

/**
 * 创建压缩包请求。
 */
export interface CreatePackageRequest extends CurrentUserRequestOptions {
  projectId: string;
  baseName: string;
  format: PackageFormat;
  continueAfterCheck?: boolean;
}

/**
 * 生成当前用户请求头。
 *
 * @param userId 当前用户标识
 * @returns 请求头对象
 */
function userHeaders(userId?: number | string): Record<string, string> | undefined {
  // 打包相关后端接口当前要求 X-User-Id，未提供时交给后端鉴权处理。
  if (userId === undefined || userId === null || userId === '') {
    return undefined;
  }

  return { 'X-User-Id': String(userId) };
}

/**
 * 执行项目打包前检查。
 *
 * @param projectId 项目标识
 * @param options 当前用户请求选项
 * @returns 检查报告和清理建议
 */
export function runPackageCheck(
  projectId: string,
  options: CurrentUserRequestOptions = {},
): Promise<CheckReport> {
  return request<CheckReport>({
    url: `/projects/${projectId}/checks`,
    method: 'POST',
    headers: userHeaders(options.userId),
  });
}

/**
 * 预览清理建议将处理的对象。
 *
 * @param payload 清理建议项和当前用户标识
 * @returns 清理预览对象列表
 */
export function previewCleanup(payload: CleanupRequest): Promise<CleanupPreview> {
  return request<CleanupPreview>({
    url: `/projects/${payload.projectId}/cleanup-preview`,
    method: 'POST',
    headers: userHeaders(payload.userId),
    data: { items: payload.items },
  });
}

/**
 * 执行清理建议。
 *
 * @param payload 清理建议项和当前用户标识
 * @returns 已清理对象标识列表
 */
export function applyCleanup(payload: CleanupRequest): Promise<CleanupApplyResponse> {
  return request<CleanupApplyResponse>({
    url: `/projects/${payload.projectId}/cleanup`,
    method: 'POST',
    headers: userHeaders(payload.userId),
    data: { items: payload.items },
  });
}

/**
 * 创建项目最终压缩包。
 *
 * @param payload 文件名、格式、确认继续打包标识和当前用户标识
 * @returns 压缩包摘要
 */
export function createPackage(payload: CreatePackageRequest): Promise<PackageArtifact> {
  return request<PackageArtifact>({
    url: `/projects/${payload.projectId}/packages`,
    method: 'POST',
    headers: userHeaders(payload.userId),
    data: {
      baseName: payload.baseName,
      format: payload.format,
      continueAfterCheck: payload.continueAfterCheck,
    },
  });
}

/**
 * 查询最近一次最终压缩包。
 *
 * @param projectId 项目标识
 * @param options 当前用户请求选项
 * @returns 最近压缩包摘要
 */
export function getLatestPackage(
  projectId: string,
  options: CurrentUserRequestOptions = {},
): Promise<PackageArtifact> {
  return request<PackageArtifact>({
    url: `/projects/${projectId}/packages/latest`,
    method: 'GET',
    headers: userHeaders(options.userId),
  });
}

/**
 * 下载最近一次最终压缩包。
 *
 * @param projectId 项目标识
 * @param options 当前用户请求选项
 * @returns 压缩包二进制 Blob
 */
export function downloadLatestPackage(
  projectId: string,
  options: CurrentUserRequestOptions = {},
): Promise<Blob> {
  return request<Blob>({
    url: `/projects/${projectId}/packages/latest/download`,
    method: 'GET',
    headers: userHeaders(options.userId),
    responseType: 'blob',
  });
}
