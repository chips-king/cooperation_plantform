/**
 * 项目运行状态。
 */
export type ProjectStatus = 'active' | 'ended';

/**
 * 小组运行状态。
 */
export type GroupStatus = 'active' | 'disabled' | string;

/**
 * 成员角色模板。
 */
export type RoleTemplate = 'OWNER' | 'MEMBER' | 'READ_ONLY';

/**
 * 邀请加入模式。
 */
export type InvitationMode = 'direct' | 'review' | string;

/**
 * 目录进度状态。
 */
export type DirectoryStatus = 'not_started' | 'in_progress' | 'completed';

/**
 * 文件状态。
 */
export type FileAssetStatus = 'active' | 'trashed' | 'superseded';

/**
 * 同名文件处理策略。
 */
export type DuplicateFilePolicy = 'overwrite' | 'rename' | 'new_version';

/**
 * 最终压缩包格式。
 */
export type PackageFormat = 'zip' | '7z' | 'tar.gz';

/**
 * 邮件草稿状态。
 */
export type MailDraftStatus = 'draft' | 'sent' | 'cancelled' | string;

/**
 * 权限点，值与后端 PermissionCode 的序列化结果保持一致。
 */
export type PermissionCode =
  | 'project.view'
  | 'project.manage'
  | 'member.manage'
  | 'permission.manage'
  | 'file.view'
  | 'file.upload'
  | 'file.download'
  | 'file.move'
  | 'file.rename'
  | 'file.delete'
  | 'file.restore'
  | 'directory.manage'
  | 'directory.status.update'
  | 'check.run'
  | 'cleanup.apply'
  | 'package.create'
  | 'package.download'
  | 'mail.draft.create'
  | 'mail.draft.update'
  | 'mail.send'
  | 'log.view'
  | 'notification.view'
  | 'project.end'
  | 'project.reopen'
  | string;

/**
 * 当前登录用户摘要。
 */
export interface CurrentUser {
  id: number;
  displayName: string;
  email: string;
  status?: string;
}

/**
 * 小组基础信息。
 */
export interface Group {
  id: number;
  name: string;
  ownerId: number;
  status: GroupStatus;
}

/**
 * 创建小组响应。
 */
export interface CreateGroupResponse {
  groupId: number;
}

/**
 * 项目详情信息。
 */
export interface Project {
  id: number;
  groupId: number;
  name: string;
  ownerId: number;
  status: ProjectStatus;
  endedAt: string | null;
  reopenedAt: string | null;
}

/**
 * 创建项目响应。
 */
export interface CreateProjectResponse {
  projectId: number;
  status: ProjectStatus;
  updatedAt: string;
}

/**
 * 成员关系信息。
 */
export interface Membership {
  id: number;
  userId: number;
  groupId: number;
  projectId: number | null;
  roleTemplate: RoleTemplate;
  customPermissions: PermissionCode[];
  status: string;
}

/**
 * 成员权限信息。
 */
export interface MemberPermission {
  membershipId: number;
  userId: number;
  userName: string;
  roleTemplate: RoleTemplate;
  permissions: PermissionCode[];
}

/**
 * 项目权限响应。
 */
export interface ProjectPermissionResponse {
  projectId: number;
  members: MemberPermission[];
}

/**
 * 成员权限更新响应。
 */
export interface UpdateMemberPermissionResponse {
  membershipId: number;
  permissions: PermissionCode[];
}

/**
 * 邀请创建响应。
 */
export interface Invitation {
  invitationId: number;
  groupId: number;
  projectId: number;
  mode: InvitationMode;
  code: string;
  invitationUrl: string;
}

/**
 * 邀请详情响应。
 */
export interface InvitationDetail {
  invitationId: number;
  groupId: number;
  groupName: string;
  projectId: number;
  projectName: string;
  mode: InvitationMode;
  status: string;
}

/**
 * 加入邀请或拒绝审核响应。
 */
export interface JoinInvitationResponse {
  status: string;
  message: string | null;
}

/**
 * 审核通过响应。
 */
export interface ApproveJoinRequestResponse {
  requestId: number;
  userId: number;
  groupId: number;
  projectId: number;
  roleTemplate: RoleTemplate;
  status: string;
}

/**
 * 文件摘要。
 */
export interface FileAsset {
  fileId: string;
  name: string;
  size: number;
  mimeType: string;
  versionNo: number;
  status: FileAssetStatus;
}

/**
 * 目录树节点。
 */
export interface Directory {
  id: string;
  parentId: string | null;
  name: string;
  status: DirectoryStatus;
  files: FileAsset[];
  children: Directory[];
}

/**
 * 目录树响应。
 */
export interface DirectoryTreeResponse {
  projectId: string;
  directories: Directory[];
}

/**
 * 删除目录响应。
 */
export interface DeleteDirectoryResponse {
  parentDirectoryId: string;
}

/**
 * 上传文件响应。
 */
export interface UploadFileResponse extends FileAsset {
  duplicatePolicy: DuplicateFilePolicy | null;
  archive: boolean;
}

/**
 * 回收站文件响应。
 */
export interface TrashFile {
  fileId: string;
  name: string;
  originalDirectoryId: string;
  deletedBy: string;
  deletedAt: string;
}

/**
 * 项目目录进度响应。
 */
export interface ProjectProgress {
  projectId: string;
  totalDirectoryCount: number;
  completedDirectoryCount: number;
  directories: DirectoryProgress[];
}

/**
 * 单个目录进度响应。
 */
export interface DirectoryProgress {
  directoryId: string;
  name: string;
  status: DirectoryStatus;
  statusDisplayName: string;
  updatedAt: string;
}

/**
 * 目录状态更新响应。
 */
export interface DirectoryStatusResponse {
  directoryId: string;
  name: string;
  status: DirectoryStatus;
  statusDisplayName: string;
}

/**
 * 检查项类型。
 */
export type CheckIssueType =
  | 'EMPTY_DIRECTORY'
  | 'ARCHIVE_FILE'
  | 'MISSING_README'
  | 'CACHE_DIRECTORY'
  | 'TEMPORARY_FILE'
  | 'LOG_FILE'
  | 'SYSTEM_FILE'
  | 'LARGE_FILE'
  | string;

/**
 * 打包检查问题。
 */
export interface CheckIssue {
  type: CheckIssueType;
  path: string;
  level: string;
  blocking: boolean;
  cleanupCandidate: boolean;
}

/**
 * 清理建议项。
 */
export interface CleanupSuggestion {
  type: CheckIssueType;
  path: string;
}

/**
 * 打包检查报告。
 */
export interface CheckReport {
  canContinuePackaging: boolean;
  issues: CheckIssue[];
  cleanupSuggestions: CleanupSuggestion[];
}

/**
 * 清理预览对象。
 */
export interface CleanupPreviewObject {
  path: string;
  objectId: string;
  fileName: string;
  size: number;
}

/**
 * 清理预览响应。
 */
export interface CleanupPreview {
  previewObjects: CleanupPreviewObject[];
}

/**
 * 清理执行响应。
 */
export interface CleanupApplyResponse {
  cleanedObjectIds: string[];
}

/**
 * 最终压缩包摘要。
 */
export interface PackageArtifact {
  packageId: string;
  filename: string;
  format: PackageFormat;
  snapshotCreatedAt: string;
  size: number;
}

/**
 * 邮件草稿详情。
 */
export interface MailDraft {
  draftId: string;
  projectId: string;
  recipients: string[];
  subject: string;
  body: string;
  packageId: string;
  attachmentFilename: string | null;
  status: MailDraftStatus;
  createdAt: string | null;
  sentAt: string | null;
}

/**
 * 邮件发送响应。
 */
export interface SendMailDraftResponse extends MailDraft {
  message: string;
}

/**
 * 操作记录。
 */
export interface OperationLog {
  id: string;
  projectId: string;
  actorId: number;
  action: string;
  targetType: string;
  targetId: string;
  summary: string;
  createdAt: string;
}

/**
 * 操作记录列表响应。
 */
export interface OperationLogList {
  logs: OperationLog[];
}

/**
 * 通知详情。
 */
export interface Notification {
  id: string;
  projectId: string;
  recipientId: number;
  type: string;
  title: string;
  content: string;
  readAt: string | null;
  createdAt: string;
}

/**
 * 通知列表响应。
 */
export interface NotificationList {
  notifications: Notification[];
}

/**
 * 全局搜索结果。
 */
export interface SearchResult {
  projects: ProjectHit[];
  files: FileHit[];
  members: MemberHit[];
}

/**
 * 项目搜索命中项。
 */
export interface ProjectHit {
  projectId: number;
  groupId: number;
  projectName: string;
}

/**
 * 文件搜索命中项。
 */
export interface FileHit {
  fileId: string;
  projectId: number;
  fileName: string;
}

/**
 * 成员搜索命中项。
 */
export interface MemberHit {
  userId: number;
  displayName: string;
}
