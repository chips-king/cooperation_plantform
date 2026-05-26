import { request } from './http';

import type { PageQuery, PageResponse } from '@/types/api';
import type {
  CreateGroupResponse,
  CreateProjectResponse,
  FileHit,
  Group,
  MemberHit,
  Project,
  ProjectHit,
  SearchResult,
} from '@/types/project';

/**
 * 带当前用户标识的请求选项。
 */
export interface CurrentUserRequestOptions {
  userId?: number | string;
}

/**
 * 创建小组请求。
 */
export interface CreateGroupRequest extends CurrentUserRequestOptions {
  name: string;
}

/**
 * 创建项目请求。
 */
export interface CreateProjectRequest extends CurrentUserRequestOptions {
  groupId: number;
  name: string;
}

/**
 * 项目列表查询参数。
 */
export interface ProjectListQuery extends PageQuery {
  groupId?: number;
  keyword?: string;
  status?: string;
}

/**
 * 搜索查询参数。
 */
export interface SearchQuery extends CurrentUserRequestOptions {
  keyword: string;
}

/**
 * 生成需要透传给后端的用户请求头。
 *
 * @param userId 当前用户标识
 * @returns 请求头对象
 */
function userHeaders(userId?: number | string): Record<string, string> | undefined {
  // 只有调用方显式提供用户标识时才补充测试期请求头，避免覆盖正式鉴权链路。
  if (userId === undefined || userId === null || userId === '') {
    return undefined;
  }

  return { 'X-User-Id': String(userId) };
}

/**
 * 查询当前用户的小组分页列表。
 *
 * @param query 分页参数
 * @param options 当前用户请求选项
 * @returns 小组分页结果
 */
export function listGroups(
  query: PageQuery = {},
  options: CurrentUserRequestOptions = {},
): Promise<PageResponse<Group>> {
  return request<PageResponse<Group>>({
    url: '/groups',
    method: 'GET',
    params: query,
    headers: userHeaders(options.userId),
  });
}

/**
 * 创建小组。
 *
 * @param payload 小组名称和当前用户标识
 * @returns 创建后的小组标识
 */
export function createGroup(payload: CreateGroupRequest): Promise<CreateGroupResponse> {
  return request<CreateGroupResponse>({
    url: '/groups',
    method: 'POST',
    headers: userHeaders(payload.userId),
    data: { name: payload.name },
  });
}

/**
 * 查询小组详情。
 *
 * @param groupId 小组标识
 * @param options 当前用户请求选项
 * @returns 小组详情
 */
export function getGroup(groupId: number, options: CurrentUserRequestOptions = {}): Promise<Group> {
  return request<Group>({
    url: `/groups/${groupId}`,
    method: 'GET',
    headers: userHeaders(options.userId),
  });
}

/**
 * 查询项目列表，用于首页最近项目和项目筛选。
 *
 * @param query 项目列表筛选条件
 * @param options 当前用户请求选项
 * @returns 项目分页结果
 */
export function listProjects(
  query: ProjectListQuery = {},
  options: CurrentUserRequestOptions = {},
): Promise<PageResponse<Project>> {
  return request<PageResponse<Project>>({
    url: '/projects',
    method: 'GET',
    params: query,
    headers: userHeaders(options.userId),
  });
}

/**
 * 在指定小组下创建项目。
 *
 * @param payload 小组标识、项目名称和当前用户标识
 * @returns 创建后的项目摘要
 */
export function createProject(payload: CreateProjectRequest): Promise<CreateProjectResponse> {
  return request<CreateProjectResponse>({
    url: `/groups/${payload.groupId}/projects`,
    method: 'POST',
    headers: userHeaders(payload.userId),
    data: { name: payload.name },
  });
}

/**
 * 查询项目详情。
 *
 * @param projectId 项目标识
 * @param options 当前用户请求选项
 * @returns 项目详情
 */
export function getProject(projectId: number, options: CurrentUserRequestOptions = {}): Promise<Project> {
  return request<Project>({
    url: `/projects/${projectId}`,
    method: 'GET',
    headers: userHeaders(options.userId),
  });
}

/**
 * 结束项目。
 *
 * @param projectId 项目标识
 * @param options 当前用户请求选项
 * @returns 结束后的项目详情
 */
export function endProject(projectId: number, options: CurrentUserRequestOptions = {}): Promise<Project> {
  return request<Project>({
    url: `/projects/${projectId}/end`,
    method: 'POST',
    headers: userHeaders(options.userId),
  });
}

/**
 * 重新打开项目。
 *
 * @param projectId 项目标识
 * @param options 当前用户请求选项
 * @returns 重新打开后的项目详情
 */
export function reopenProject(projectId: number, options: CurrentUserRequestOptions = {}): Promise<Project> {
  return request<Project>({
    url: `/projects/${projectId}/reopen`,
    method: 'POST',
    headers: userHeaders(options.userId),
  });
}

/**
 * 搜索当前用户可见项目。
 *
 * @param query 搜索关键字和当前用户标识
 * @returns 项目搜索命中列表
 */
export function searchProjects(query: SearchQuery): Promise<SearchResult | ProjectHit[]> {
  return request<SearchResult | ProjectHit[]>({
    url: '/search/projects',
    method: 'GET',
    params: { keyword: query.keyword },
    headers: userHeaders(query.userId),
  });
}

/**
 * 搜索当前用户可访问文件。
 *
 * @param query 搜索关键字和当前用户标识
 * @returns 文件搜索命中列表
 */
export function searchFiles(query: SearchQuery): Promise<SearchResult | FileHit[]> {
  return request<SearchResult | FileHit[]>({
    url: '/search/files',
    method: 'GET',
    params: { keyword: query.keyword },
    headers: userHeaders(query.userId),
  });
}

/**
 * 搜索共同项目范围内的成员。
 *
 * @param query 搜索关键字和当前用户标识
 * @returns 成员搜索命中列表
 */
export function searchMembers(query: SearchQuery): Promise<SearchResult | MemberHit[]> {
  return request<SearchResult | MemberHit[]>({
    url: '/search/members',
    method: 'GET',
    params: { keyword: query.keyword },
    headers: userHeaders(query.userId),
  });
}
