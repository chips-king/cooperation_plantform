import { request } from './http';

import type {
  DirectoryStatus,
  DirectoryStatusResponse,
  DirectoryTreeResponse,
  DeleteDirectoryResponse,
  Directory,
  DuplicateFilePolicy,
  FileAsset,
  ProjectProgress,
  TrashFile,
  UploadFileResponse,
} from '@/types/project';

/**
 * 上传文件请求。
 */
export interface UploadFileRequest {
  projectId: string;
  directoryId: string;
  file: File;
  duplicatePolicy?: DuplicateFilePolicy;
  relativePath?: string;
}

/**
 * 移动文件请求。
 */
export interface MoveFileRequest {
  projectId: string;
  fileId: string;
  targetDirectoryId: string;
}

/**
 * 删除文件请求。
 */
export interface DeleteFileRequest {
  projectId: string;
  fileId: string;
}

/**
 * 恢复文件请求。
 */
export interface RestoreFileRequest {
  projectId: string;
  fileId: string;
  restoreDirectoryId: string;
}

/**
 * 更新目录状态请求。
 */
export interface UpdateDirectoryStatusRequest {
  projectId: string;
  directoryId: string;
  status: DirectoryStatus;
}

/**
 * 创建目录请求。
 */
export interface CreateDirectoryRequest {
  projectId: string;
  parentDirectoryId: string;
  name: string;
}

/**
 * 删除目录请求。
 */
export interface DeleteDirectoryRequest {
  projectId: string;
  directoryId: string;
}

/**
 * 查询项目目录树。
 *
 * @param projectId 项目标识
 * @returns 项目目录树
 */
export function getDirectoryTree(projectId: string): Promise<DirectoryTreeResponse> {
  return request<DirectoryTreeResponse>({
    url: `/projects/${projectId}/tree`,
    method: 'GET',
  });
}

/**
 * 创建当前目录下的子目录。
 *
 * @param payload 项目标识、父目录标识和目录名
 * @returns 新建目录节点
 */
export function createDirectory(payload: CreateDirectoryRequest): Promise<Directory> {
  return request<Directory>({
    url: '/directories',
    method: 'POST',
    data: payload,
  });
}

/**
 * 删除当前空目录。
 *
 * @param payload 项目标识和目录标识
 * @returns 被删除目录的父目录定位信息
 */
export function deleteDirectory(payload: DeleteDirectoryRequest): Promise<DeleteDirectoryResponse> {
  return request<DeleteDirectoryResponse>({
    url: `/directories/${payload.directoryId}`,
    method: 'DELETE',
    params: { projectId: payload.projectId },
  });
}

/**
 * 上传文件到指定目录。
 *
 * @param payload 项目、目录、文件和同名策略
 * @returns 上传后的文件信息
 */
export function uploadFile(payload: UploadFileRequest): Promise<UploadFileResponse> {
  const formData = new FormData();
  formData.append('file', payload.file);

  return request<UploadFileResponse>({
    url: `/directories/${payload.directoryId}/files`,
    method: 'POST',
    params: {
      projectId: payload.projectId,
      duplicatePolicy: payload.duplicatePolicy,
      relativePath: payload.relativePath,
    },
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

/**
 * 下载指定文件。
 *
 * @param fileId 文件标识
 * @returns 文件二进制 Blob
 */
export function downloadFile(fileId: string): Promise<Blob> {
  return request<Blob>({
    url: `/files/${fileId}/download`,
    method: 'GET',
    responseType: 'blob',
  });
}

/**
 * 移动文件到目标目录。
 *
 * @param payload 文件标识、项目标识和目标目录标识
 * @returns 移动后的文件摘要
 */
export function moveFile(payload: MoveFileRequest): Promise<FileAsset> {
  return request<FileAsset>({
    url: `/files/${payload.fileId}/move`,
    method: 'POST',
    data: {
      projectId: payload.projectId,
      targetDirectoryId: payload.targetDirectoryId,
    },
  });
}

/**
 * 删除文件并移入回收站。
 *
 * @param payload 文件标识和项目标识
 * @returns 删除后的文件摘要
 */
export function deleteFile(payload: DeleteFileRequest): Promise<FileAsset> {
  return request<FileAsset>({
    url: `/files/${payload.fileId}`,
    method: 'DELETE',
    params: { projectId: payload.projectId },
  });
}

/**
 * 查询项目回收站文件。
 *
 * @param projectId 项目标识
 * @returns 回收站文件列表
 */
export function listTrashFiles(projectId: string): Promise<TrashFile[]> {
  return request<TrashFile[]>({
    url: `/projects/${projectId}/trash`,
    method: 'GET',
  });
}

/**
 * 恢复回收站文件。
 *
 * @param payload 文件标识、项目标识和恢复目录标识
 * @returns 恢复后的文件摘要
 */
export function restoreFile(payload: RestoreFileRequest): Promise<FileAsset> {
  return request<FileAsset>({
    url: `/files/${payload.fileId}/restore`,
    method: 'POST',
    data: {
      projectId: payload.projectId,
      restoreDirectoryId: payload.restoreDirectoryId,
    },
  });
}

/**
 * 查询项目目录进度。
 *
 * @param projectId 项目标识
 * @returns 项目目录进度
 */
export function getProjectProgress(projectId: string): Promise<ProjectProgress> {
  return request<ProjectProgress>({
    url: `/projects/${projectId}/progress`,
    method: 'GET',
  });
}

/**
 * 更新目录进度状态。
 *
 * @param payload 目录标识、项目标识和目标状态
 * @returns 更新后的目录状态
 */
export function updateDirectoryStatus(payload: UpdateDirectoryStatusRequest): Promise<DirectoryStatusResponse> {
  return request<DirectoryStatusResponse>({
    url: `/directories/${payload.directoryId}/status`,
    method: 'PATCH',
    data: {
      projectId: payload.projectId,
      status: payload.status,
    },
  });
}
