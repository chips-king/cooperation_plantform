package com.cooperation.infrastructure.runtime;

import com.cooperation.application.directory.DirectoryLookupPort;
import com.cooperation.application.directory.DirectoryRepository;
import com.cooperation.application.directory.ListProjectProgressUseCase;
import com.cooperation.application.directory.UpdateDirectoryStatusUseCase;
import com.cooperation.application.file.DeleteFileUseCase;
import com.cooperation.application.file.DownloadFileUseCase;
import com.cooperation.application.file.FileStoragePort;
import com.cooperation.application.file.ListDirectoryTreeUseCase;
import com.cooperation.application.file.ListTrashFilesUseCase;
import com.cooperation.application.file.MoveFileUseCase;
import com.cooperation.application.file.RestoreFileUseCase;
import com.cooperation.application.file.UploadFileUseCase;
import com.cooperation.application.group.CreateGroupUseCase;
import com.cooperation.application.group.GetGroupDetailUseCase;
import com.cooperation.application.group.Group;
import com.cooperation.application.group.GroupRepository;
import com.cooperation.application.group.ListGroupsUseCase;
import com.cooperation.application.invitation.Invitation;
import com.cooperation.application.invitation.InvitationRepository;
import com.cooperation.application.log.ListOperationLogsUseCase;
import com.cooperation.application.log.OperationLogWriter;
import com.cooperation.application.log.QueryOperationLogUseCase;
import com.cooperation.application.mail.CreateMailDraftUseCase;
import com.cooperation.application.mail.QueryMailDraftUseCase;
import com.cooperation.application.mail.SendMailDraftUseCase;
import com.cooperation.application.mail.UpdateMailDraftUseCase;
import com.cooperation.application.member.JoinByInvitationUseCase;
import com.cooperation.application.member.JoinRequest;
import com.cooperation.application.member.JoinRequestRepository;
import com.cooperation.application.member.Membership;
import com.cooperation.application.member.MembershipRepository;
import com.cooperation.application.notification.ListNotificationsUseCase;
import com.cooperation.application.notification.NotificationPublisher;
import com.cooperation.application.packageartifact.ApplyCleanupSuggestionUseCase;
import com.cooperation.application.packageartifact.CleanupPermissionChecker;
import com.cooperation.application.packageartifact.CleanupTargetRepository;
import com.cooperation.application.packageartifact.CreatePackageUseCase;
import com.cooperation.application.packageartifact.DownloadLatestPackageUseCase;
import com.cooperation.application.packageartifact.PackageArchivePort;
import com.cooperation.application.packageartifact.PackageArtifact;
import com.cooperation.application.packageartifact.PackageArtifactRepository;
import com.cooperation.application.packageartifact.PackageSnapshotRepository;
import com.cooperation.application.packageartifact.ProjectPackageSnapshotRepository;
import com.cooperation.application.packageartifact.QueryLatestPackageUseCase;
import com.cooperation.application.packageartifact.RunPackageCheckUseCase;
import com.cooperation.application.permission.PermissionChecker;
import com.cooperation.application.permission.UpdateMemberPermissionUseCase;
import com.cooperation.application.project.CreateProjectUseCase;
import com.cooperation.application.project.EndProjectUseCase;
import com.cooperation.application.project.GetProjectDetailUseCase;
import com.cooperation.application.project.ReopenProjectUseCase;
import com.cooperation.application.search.SearchUseCase;
import com.cooperation.domain.check.ProjectFileTree;
import com.cooperation.domain.directory.DirectoryNode;
import com.cooperation.domain.directory.DirectoryStatus;
import com.cooperation.domain.file.FileAsset;
import com.cooperation.domain.file.FileAssetRepository;
import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.log.OperationLog;
import com.cooperation.domain.log.OperationLogRepository;
import com.cooperation.domain.mail.MailDraft;
import com.cooperation.domain.notification.NotificationEventType;
import com.cooperation.domain.packageartifact.PackageFormat;
import com.cooperation.domain.permission.PermissionCode;
import com.cooperation.domain.permission.RoleTemplate;
import com.cooperation.domain.project.Project;
import com.cooperation.domain.project.ProjectRepository;
import com.cooperation.web.file.FileDto;
import com.cooperation.web.group.GroupDto;
import com.cooperation.web.progress.ProgressDto;
import com.cooperation.web.project.ProjectDto;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 应用运行期 Bean 装配配置，为尚未接入完整持久化的端口提供可启动、可联调的最小实现。
 */
@Configuration
public class ApplicationRuntimeConfig {

    /**
     * 演示目录标识，作为第一版无目录管理界面时的默认协作目录。
     */
    private static final String DEFAULT_DIRECTORY_ID = "1";

    /**
     * 演示目录名称，用于文件管理和进度页面展示。
     */
    private static final String DEFAULT_DIRECTORY_NAME = "默认分工目录";

    /**
     * 提供统一应用时钟，便于用例记录稳定的操作时间。
     *
     * @return 系统 UTC 时钟。
     */
    @Bean
    @ConditionalOnMissingBean
    public Clock applicationClock() {
        return Clock.systemUTC();
    }

    /**
     * 注册文件上传用例。
     */
    @Bean
    @ConditionalOnMissingBean
    public UploadFileUseCase uploadFileUseCase(
            FileAssetRepository files,
            FileStoragePort storage,
            PermissionChecker permissionChecker,
            OperationLogWriter operationLogs,
            NotificationPublisher notifications
    ) {
        return new UploadFileUseCase(files, storage, permissionChecker, operationLogs, notifications);
    }

    /**
     * 注册文件删除用例。
     */
    @Bean
    @ConditionalOnMissingBean
    public DeleteFileUseCase deleteFileUseCase(
            FileAssetRepository files,
            PermissionChecker permissionChecker,
            OperationLogWriter operationLogs,
            NotificationPublisher notifications
    ) {
        return new DeleteFileUseCase(files, permissionChecker, operationLogs, notifications);
    }

    /**
     * 注册文件恢复用例。
     */
    @Bean
    @ConditionalOnMissingBean
    public RestoreFileUseCase restoreFileUseCase(
            FileAssetRepository files,
            DirectoryLookupPort directories,
            PermissionChecker permissionChecker,
            OperationLogWriter operationLogs,
            NotificationPublisher notifications
    ) {
        return new RestoreFileUseCase(files, directories, permissionChecker, operationLogs, notifications);
    }

    /**
     * 注册目录状态更新用例。
     */
    @Bean
    @ConditionalOnMissingBean
    public UpdateDirectoryStatusUseCase updateDirectoryStatusUseCase(
            DirectoryRepository directories,
            PermissionChecker permissionChecker,
            OperationLogWriter operationLogs,
            NotificationPublisher notifications
    ) {
        return new UpdateDirectoryStatusUseCase(directories, permissionChecker, operationLogs, notifications);
    }

    /**
     * 注册创建小组用例。
     */
    @Bean
    @ConditionalOnMissingBean
    public CreateGroupUseCase createGroupUseCase(
            GroupRepository groupRepository,
            MembershipRepository membershipRepository,
            OperationLogRepository operationLogRepository
    ) {
        return new CreateGroupUseCase(groupRepository, membershipRepository, operationLogRepository);
    }

    /**
     * 注册创建项目用例。
     */
    @Bean
    @ConditionalOnMissingBean
    public CreateProjectUseCase createProjectUseCase(
            ProjectRepository projectRepository,
            MembershipRepository membershipRepository,
            OperationLogRepository operationLogRepository,
            Clock clock
    ) {
        return new CreateProjectUseCase(projectRepository, membershipRepository, operationLogRepository, clock);
    }

    /**
     * 注册结束项目用例。
     */
    @Bean
    @ConditionalOnMissingBean
    public EndProjectUseCase endProjectUseCase(ProjectRepository projectRepository, Clock clock) {
        EndProjectUseCase.ProjectRepository repository = new EndProjectUseCase.ProjectRepository() {
            @Override
            public Optional<Project> findById(Long projectId) {
                return projectRepository.findById(projectId);
            }

            @Override
            public Project save(Project project) {
                return projectRepository.save(project);
            }
        };
        return new EndProjectUseCase(
                repository,
                projectId -> {
                },
                (projectId, actorId, retainUntil) -> {
                },
                (projectId, type) -> {
                },
                clock
        );
    }

    /**
     * 注册重新打开项目用例。
     */
    @Bean
    @ConditionalOnMissingBean
    public ReopenProjectUseCase reopenProjectUseCase(ProjectRepository projectRepository) {
        ReopenProjectUseCase.ProjectRepository repository = new ReopenProjectUseCase.ProjectRepository() {
            @Override
            public Optional<Project> findById(Long projectId) {
                return projectRepository.findById(projectId);
            }

            @Override
            public Project save(Project project) {
                return projectRepository.save(project);
            }
        };
        return new ReopenProjectUseCase(
                repository,
                projectId -> {
                },
                (projectId, actorId, action) -> {
                },
                (projectId, type) -> {
                }
        );
    }

    /**
     * 注册打包检查用例。
     */
    @Bean
    @ConditionalOnMissingBean
    public RunPackageCheckUseCase runPackageCheckUseCase(
            ProjectPackageSnapshotRepository snapshots,
            OperationLogRepository logs
    ) {
        return new RunPackageCheckUseCase(snapshots, logs);
    }

    /**
     * 注册清理建议用例。
     */
    @Bean
    @ConditionalOnMissingBean
    public ApplyCleanupSuggestionUseCase applyCleanupSuggestionUseCase(
            CleanupTargetRepository targets,
            CleanupPermissionChecker permissions,
            OperationLogRepository logs
    ) {
        return new ApplyCleanupSuggestionUseCase(targets, permissions, logs);
    }

    /**
     * 注册创建压缩包用例。
     */
    @Bean
    @ConditionalOnMissingBean
    public CreatePackageUseCase createPackageUseCase(
            PackageSnapshotRepository snapshots,
            @Qualifier("zipArchiveAdapter")
            PackageArchivePort archivePort,
            PackageArtifactRepository packages,
            OperationLogRepository logs
    ) {
        return new CreatePackageUseCase(snapshots, archivePort, packages, logs);
    }

    /**
     * 注册邮件草稿创建用例。
     */
    @Bean
    @ConditionalOnMissingBean
    public CreateMailDraftUseCase createMailDraftUseCase(RuntimeMailStore store) {
        return new CreateMailDraftUseCase(store, store, store, store);
    }

    /**
     * 注册邮件草稿发送用例。
     */
    @Bean
    @ConditionalOnMissingBean
    public SendMailDraftUseCase sendMailDraftUseCase(RuntimeMailStore store, Clock clock) {
        return new SendMailDraftUseCase(store, store, store, store, clock);
    }

    /**
     * 注册成员邀请加入用例。
     */
    @Bean
    @ConditionalOnMissingBean
    public JoinByInvitationUseCase joinByInvitationUseCase(
            InvitationRepository invitations,
            MembershipRepository memberships,
            JoinRequestRepository joinRequests,
            OperationLogRepository logs
    ) {
        return new JoinByInvitationUseCase(invitations, memberships, joinRequests, logs);
    }

    /**
     * 注册成员权限更新用例。
     */
    @Bean
    @ConditionalOnMissingBean
    public UpdateMemberPermissionUseCase updateMemberPermissionUseCase(
            MembershipRepository memberships,
            OperationLogRepository logs
    ) {
        return new UpdateMemberPermissionUseCase(memberships, logs);
    }

    /**
     * 提供默认允许的权限检查器，后续可替换为成员与目录权限表实现。
     */
    @Bean
    @ConditionalOnMissingBean
    public PermissionChecker permissionChecker() {
        return (userId, projectId, directoryId, permissionCode) -> true;
    }

    /**
     * 提供空通知发布器，保证业务动作可先完成再补平台内通知持久化。
     */
    @Bean
    @ConditionalOnMissingBean
    public NotificationPublisher notificationPublisher() {
        return new NotificationPublisher() {
        };
    }

    /**
     * 提供不抛错的操作记录写入器，避免演示阶段非数字用户标识导致记录写入中断业务。
     */
    @Bean
    @ConditionalOnMissingBean
    public OperationLogWriter operationLogWriter() {
        return operationLog -> {
        };
    }

    /**
     * 提供默认目录查询端口。
     */
    @Bean
    @ConditionalOnMissingBean
    public DirectoryLookupPort directoryLookupPort() {
        return (projectId, directoryId) -> true;
    }

    @Bean
    @ConditionalOnMissingBean
    public RuntimeDirectoryStore runtimeDirectoryStore() {
        return new RuntimeDirectoryStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public RuntimeGroupStore runtimeGroupStore() {
        return new RuntimeGroupStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public RuntimeMembershipStore runtimeMembershipStore() {
        return new RuntimeMembershipStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public RuntimeInvitationStore runtimeInvitationStore() {
        return new RuntimeInvitationStore();
    }

    /**
     * 提供内存邮件草稿和最近压缩包仓储。
     */
    @Bean
    @ConditionalOnMissingBean
    public RuntimeMailStore runtimeMailStore() {
        return new RuntimeMailStore();
    }

    /**
     * 查询文件目录树。
     */
    @Bean
    @ConditionalOnMissingBean
    public ListDirectoryTreeUseCase listDirectoryTreeUseCase(FileAssetRepository files) {
        return projectId -> {
            List<FileDto.FileItemResponse> fileItems = safelyActiveFiles(files, projectId).stream()
                    .map(FileDto.FileItemResponse::from)
                    .toList();
            FileDto.DirectoryTreeResponse.DirectoryNodeResponse directory =
                    new FileDto.DirectoryTreeResponse.DirectoryNodeResponse(
                            DEFAULT_DIRECTORY_ID,
                            null,
                            DEFAULT_DIRECTORY_NAME,
                            DirectoryStatus.IN_PROGRESS.getValue(),
                            fileItems,
                            List.of()
                    );
            return new FileDto.DirectoryTreeResponse(projectId, List.of(directory));
        };
    }

    /**
     * 查询回收站文件。
     */
    @Bean
    @ConditionalOnMissingBean
    public ListTrashFilesUseCase listTrashFilesUseCase(FileAssetRepository files) {
        return projectId -> safelyTrashedFiles(files, projectId).stream()
                .map(file -> new FileDto.TrashFileResponse(
                        file.id(),
                        file.name().value(),
                        file.directoryId(),
                        file.deletedBy(),
                        file.deletedAt() == null ? null : file.deletedAt().toInstant(ZoneOffset.UTC)
                ))
                .toList();
    }

    /**
     * 查询文件下载内容。
     */
    @Bean
    @ConditionalOnMissingBean
    public DownloadFileUseCase downloadFileUseCase(FileAssetRepository files) {
        return fileId -> files.findById(fileId)
                .map(file -> new FileDto.DownloadResponse(file.name().value(), file.mimeType(), new byte[0]))
                .orElseThrow(() -> new IllegalArgumentException("文件不存在"));
    }

    /**
     * 移动文件到目标目录。
     */
    @Bean
    @ConditionalOnMissingBean
    public MoveFileUseCase moveFileUseCase(FileAssetRepository files) {
        return (fileId, request) -> {
            FileAsset file = files.findById(fileId).orElseThrow(() -> new IllegalArgumentException("文件不存在"));
            file.restoreToDirectory(request.targetDirectoryId());
            return FileDto.FileItemResponse.from(files.save(file));
        };
    }

    /**
     * 查询项目进度。
     */
    @Bean
    @ConditionalOnMissingBean
    public ListProjectProgressUseCase listProjectProgressUseCase() {
        return projectId -> new ProgressDto.ProjectProgressResponse(
                projectId,
                1,
                0,
                List.of(new ProgressDto.DirectoryProgressResponse(
                        DEFAULT_DIRECTORY_ID,
                        DEFAULT_DIRECTORY_NAME,
                        DirectoryStatus.IN_PROGRESS.getValue(),
                        DirectoryStatus.IN_PROGRESS.getDisplayName(),
                        Instant.now()
                ))
        );
    }

    /**
     * 查询项目详情。
     */
    @Bean
    @ConditionalOnMissingBean
    public GetProjectDetailUseCase getProjectDetailUseCase(ProjectRepository projects) {
        return query -> projects.findById(query.projectId())
                .map(project -> new ProjectDto.ProjectDetailResponse(
                        project.getId(),
                        project.getGroupId(),
                        project.getName(),
                        project.getOwnerId(),
                        project.getStatus().getValue(),
                        project.getEndedAt(),
                        project.getReopenedAt()
                ))
                .orElseThrow(() -> new IllegalArgumentException("项目不存在"));
    }

    /**
     * 查询小组列表。
     */
    @Bean
    @ConditionalOnMissingBean
    public ListGroupsUseCase listGroupsUseCase(RuntimeGroupStore store) {
        return query -> {
            List<GroupDto.GroupResponse> items = store.groups.values().stream()
                    .map(group -> new GroupDto.GroupResponse(group.getId(), group.getName(), group.getOwnerId(), group.getStatus().name().toLowerCase()))
                    .toList();
            return new ListGroupsUseCase.Result(items, query.page(), query.size(), items.size());
        };
    }

    /**
     * 查询小组详情。
     */
    @Bean
    @ConditionalOnMissingBean
    public GetGroupDetailUseCase getGroupDetailUseCase(RuntimeGroupStore store) {
        return query -> store.findById(query.groupId())
                .map(group -> new GroupDto.GroupResponse(group.getId(), group.getName(), group.getOwnerId(), group.getStatus().name().toLowerCase()))
                .orElseThrow(() -> new IllegalArgumentException("小组不存在"));
    }

    /**
     * 查询操作记录列表。
     */
    @Bean
    @ConditionalOnMissingBean
    public ListOperationLogsUseCase listOperationLogsUseCase() {
        return new ListOperationLogsUseCase(query -> List.of(), (userId, projectId) -> RoleTemplate.MEMBER);
    }

    /**
     * 查询单条操作记录。
     */
    @Bean
    @ConditionalOnMissingBean
    public QueryOperationLogUseCase queryOperationLogUseCase(
            OperationLogRepository logs,
            MembershipRepository memberships
    ) {
        return new QueryOperationLogUseCase(logs, (userId, projectId) -> RoleTemplate.MEMBER);
    }

    /**
     * 查询通知列表。
     */
    @Bean
    @ConditionalOnMissingBean
    public ListNotificationsUseCase listNotificationsUseCase() {
        return new ListNotificationsUseCase(new RuntimeNotificationPort());
    }

    /**
     * 查询全局搜索。
     */
    @Bean
    @ConditionalOnMissingBean
    public SearchUseCase searchUseCase() {
        return new SearchUseCase(new RuntimeSearchRepository());
    }

    /**
     * 查询邮件草稿详情。
     */
    @Bean
    @ConditionalOnMissingBean
    public QueryMailDraftUseCase queryMailDraftUseCase(RuntimeMailStore store) {
        return query -> store.findById(query.draftId())
                .map(draft -> new QueryMailDraftUseCase.Result(
                        query.draftId(),
                        draft.getProjectId(),
                        draft.getRecipients(),
                        draft.getSubject(),
                        draft.getBody(),
                        draft.getPackageId(),
                        store.latestFilename(draft.getProjectId()),
                        draft.getStatus().name().toLowerCase(),
                        Instant.now(),
                        draft.getSentAt()
                ))
                .orElseThrow(() -> new IllegalArgumentException("邮件草稿不存在"));
    }

    /**
     * 更新邮件草稿。
     */
    @Bean
    @ConditionalOnMissingBean
    public UpdateMailDraftUseCase updateMailDraftUseCase(RuntimeMailStore store) {
        return command -> {
            MailDraft draft = store.findById(command.draftId())
                    .orElseThrow(() -> new IllegalArgumentException("邮件草稿不存在"));
            draft.update(command.recipients(), command.subject(), command.body(), command.packageId());
            store.save(draft);
            return new UpdateMailDraftUseCase.Result(
                    command.draftId(),
                    draft.getProjectId(),
                    draft.getRecipients(),
                    draft.getSubject(),
                    draft.getBody(),
                    draft.getPackageId(),
                    store.latestFilename(draft.getProjectId()),
                    draft.getStatus().name().toLowerCase(),
                    Instant.now()
            );
        };
    }

    /**
     * 查询最近压缩包。
     */
    @Bean
    @ConditionalOnMissingBean
    public QueryLatestPackageUseCase queryLatestPackageUseCase(RuntimeMailStore store) {
        return query -> store.latestPackageSummary(query.projectId());
    }

    /**
     * 下载最近压缩包。
     */
    @Bean
    @ConditionalOnMissingBean
    public DownloadLatestPackageUseCase downloadLatestPackageUseCase(RuntimeMailStore store) {
        return command -> new DownloadLatestPackageUseCase.Result(
                store.latestFilename(command.projectId()),
                "application/octet-stream",
                new byte[0]
        );
    }

    /**
     * 打包检查快照仓储。
     */
    @Bean
    @ConditionalOnMissingBean
    public ProjectPackageSnapshotRepository projectPackageSnapshotRepository() {
        return projectId -> new ProjectFileTree(List.of());
    }

    /**
     * 打包源快照仓储。
     */
    @Bean
    @ConditionalOnMissingBean
    public PackageSnapshotRepository packageSnapshotRepository() {
        return new RuntimePackageSnapshotRepository();
    }

    /**
     * 压缩包元数据仓储。
     */
    @Bean
    @ConditionalOnMissingBean
    public PackageArtifactRepository packageArtifactRepository(RuntimeMailStore store) {
        return store;
    }

    /**
     * 清理目标仓储。
     */
    @Bean
    @ConditionalOnMissingBean
    public CleanupTargetRepository cleanupTargetRepository() {
        return new RuntimeCleanupTargetRepository();
    }

    /**
     * 清理权限检查。
     */
    @Bean
    @ConditionalOnMissingBean
    public CleanupPermissionChecker cleanupPermissionChecker() {
        return (projectId, actorId, items) -> {
        };
    }

    private List<FileAsset> safelyActiveFiles(FileAssetRepository files, String projectId) {
        try {
            return files.findActiveByProjectId(projectId);
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private List<FileAsset> safelyTrashedFiles(FileAssetRepository files, String projectId) {
        try {
            return files.findTrashedByProjectId(projectId);
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    /**
     * 运行期目录内存仓储。
     */
    static final class RuntimeDirectoryStore implements DirectoryRepository {

        private final Map<String, DirectoryNode> directories = new ConcurrentHashMap<>();

        @Override
        public DirectoryNode save(String directoryId, DirectoryNode directory) {
            directories.put(directoryId, directory);
            return directory;
        }

        @Override
        public Optional<DirectoryNode> findByProjectIdAndDirectoryId(String projectId, String directoryId) {
            DirectoryNode fallback = DirectoryNode.create(parseLong(projectId, 1L), null, DEFAULT_DIRECTORY_NAME, 1L);
            directories.putIfAbsent(directoryId, fallback);
            return Optional.ofNullable(directories.get(directoryId));
        }
    }

    /**
     * 运行期项目内存仓储，用于本地开发和数据库不可用时保持项目流程可操作。
     */
    static final class RuntimeProjectStore implements ProjectRepository {

        private final AtomicLong projectIds = new AtomicLong(1);
        private final Map<Long, Project> projects = new ConcurrentHashMap<>();

        @Override
        public Project save(Project project) {
            Project saved = project.getId() == null
                    ? Project.restore(projectIds.getAndIncrement(), project.getGroupId(), project.getOwnerId(), project.getName(), project.getStatus())
                    : project;
            projects.put(saved.getId(), saved);
            return saved;
        }

        @Override
        public Optional<Project> findById(Long id) {
            return Optional.ofNullable(projects.get(id));
        }

        @Override
        public List<Project> findRecentByUserId(Long userId, int limit) {
            return projects.values().stream()
                    .filter(project -> Objects.equals(project.getOwnerId(), userId))
                    .limit(Math.max(limit, 0))
                    .toList();
        }
    }

    /**
     * 运行期文件元数据内存仓储，负责支撑上传、目录树、回收站和打包检查。
     */
    static final class RuntimeFileAssetStore implements FileAssetRepository {

        private final Map<String, FileAsset> files = new ConcurrentHashMap<>();

        @Override
        public FileAsset save(FileAsset fileAsset) {
            files.put(fileAsset.id(), fileAsset);
            return fileAsset;
        }

        @Override
        public Optional<FileAsset> findById(String id) {
            return Optional.ofNullable(files.get(id));
        }

        @Override
        public Optional<FileAsset> findActiveByDirectoryIdAndName(String directoryId, com.cooperation.domain.file.FileName name) {
            return files.values().stream()
                    .filter(FileAsset::isActive)
                    .filter(file -> Objects.equals(file.directoryId(), directoryId))
                    .filter(file -> Objects.equals(file.name(), name))
                    .findFirst();
        }

        @Override
        public List<FileAsset> findActiveByProjectId(String projectId) {
            return files.values().stream()
                    .filter(FileAsset::isActive)
                    .filter(file -> Objects.equals(file.projectId(), projectId))
                    .toList();
        }

        @Override
        public List<FileAsset> findTrashedByProjectId(String projectId) {
            return files.values().stream()
                    .filter(file -> !file.isActive())
                    .filter(file -> Objects.equals(file.projectId(), projectId))
                    .toList();
        }
    }

    /**
     * 运行期操作记录内存仓储，避免业务操作因为日志数据库不可用而失败。
     */
    static final class RuntimeOperationLogStore implements OperationLogRepository {

        private final Map<String, OperationLog> logs = new ConcurrentHashMap<>();

        @Override
        public OperationLog save(OperationLog operationLog) {
            String logId = operationLog.getProjectId() + "-" + operationLog.getTargetType() + "-"
                    + operationLog.getTargetId() + "-" + operationLog.getCreatedAt().toEpochMilli();
            logs.put(logId, operationLog);
            return operationLog;
        }

        @Override
        public List<OperationLog> findByProjectId(String projectId) {
            return logs.values().stream()
                    .filter(log -> Objects.equals(log.getProjectId(), projectId))
                    .toList();
        }

        @Override
        public List<OperationLog> findByProjectIdAndAction(String projectId, OperationAction action) {
            return findByProjectId(projectId).stream()
                    .filter(log -> log.getAction() == action)
                    .toList();
        }

        @Override
        public List<OperationLog> findByProjectIdAndActorId(String projectId, String actorId) {
            return findByProjectId(projectId).stream()
                    .filter(log -> Objects.equals(log.getActorId(), actorId))
                    .toList();
        }

        @Override
        public Optional<OperationLog> findById(String id) {
            return Optional.ofNullable(logs.get(id));
        }
    }

    /**
     * 运行期小组、成员、邀请内存仓储。
     */
    static final class RuntimeGroupStore implements GroupRepository {

        private final AtomicLong groupIds = new AtomicLong(1);
        private final Map<Long, Group> groups = new ConcurrentHashMap<>();

        @Override
        public Group save(Group group) {
            Group saved = group.getId() == null
                    ? Group.restore(groupIds.getAndIncrement(), group.getOwnerId(), group.getName(), group.getStatus())
                    : group;
            groups.put(saved.getId(), saved);
            return saved;
        }

        @Override
        public Optional<Group> findById(Long id) {
            return Optional.ofNullable(groups.get(id));
        }
    }

    static final class RuntimeMembershipStore implements MembershipRepository {

        private final AtomicLong membershipIds = new AtomicLong(1);
        private final Map<Long, Membership> memberships = new ConcurrentHashMap<>();

        @Override
        public Membership save(Membership membership) {
            Membership saved = membership.getId() == null
                    ? membership.withId(membershipIds.getAndIncrement())
                    : membership;
            memberships.put(saved.getId(), saved);
            return saved;
        }

        @Override
        public Optional<Membership> findById(Long id) {
            return Optional.ofNullable(memberships.get(id));
        }

        @Override
        public Optional<Membership> findByGroupIdAndUserId(Long groupId, Long userId) {
            return memberships.values().stream()
                    .filter(membership -> Objects.equals(membership.getGroupId(), groupId))
                    .filter(membership -> Objects.equals(membership.getUserId(), userId))
                    .findFirst()
                    .or(() -> Optional.of(Membership.groupLevel(userId, groupId, RoleTemplate.OWNER)));
        }

        @Override
        public Optional<Membership> findByProjectIdAndUserId(Long projectId, Long userId) {
            return memberships.values().stream()
                    .filter(membership -> membership.getProjectId().map(projectId::equals).orElse(false))
                    .filter(membership -> Objects.equals(membership.getUserId(), userId))
                    .findFirst()
                    .or(() -> Optional.of(Membership.projectLevel(userId, 1L, projectId, RoleTemplate.OWNER)));
        }
    }

    static final class RuntimeInvitationStore implements InvitationRepository, JoinRequestRepository {

        private final Map<String, Invitation> invitations = new ConcurrentHashMap<>();
        private final AtomicLong joinRequestIds = new AtomicLong(1);
        private final Map<Long, JoinRequest> joinRequests = new ConcurrentHashMap<>();

        @Override
        public Invitation save(Invitation invitation) {
            invitations.put(invitation.getCode(), invitation);
            return invitation;
        }

        @Override
        public Optional<Invitation> findValidByCode(String code) {
            return Optional.ofNullable(invitations.get(code));
        }

        @Override
        public JoinRequest save(JoinRequest joinRequest) {
            JoinRequest saved = joinRequest.getId() == null ? joinRequest.withId(joinRequestIds.getAndIncrement()) : joinRequest;
            joinRequests.put(saved.getId(), saved);
            return saved;
        }
    }

    /**
     * 运行期邮件和压缩包内存仓储。
     */
    static final class RuntimeMailStore implements
            CreateMailDraftUseCase.PackageRepository,
            CreateMailDraftUseCase.MailDraftRepository,
            CreateMailDraftUseCase.OperationLogWriter,
            CreateMailDraftUseCase.NotificationPublisher,
            SendMailDraftUseCase.MailDraftRepository,
            SendMailDraftUseCase.MailProviderPort,
            SendMailDraftUseCase.OperationLogWriter,
            SendMailDraftUseCase.NotificationPublisher,
            PackageArtifactRepository {

        private static final String DEFAULT_PACKAGE_ID = "package-demo";
        private static final String DEFAULT_PACKAGE_FILENAME = "final-report.zip";

        private final Map<String, MailDraft> drafts = new ConcurrentHashMap<>();
        private final Map<String, QueryLatestPackageUseCase.Result> latestPackages = new ConcurrentHashMap<>();

        @Override
        public Optional<CreateMailDraftUseCase.LatestPackage> findLatestUsableByProjectId(String projectId) {
            QueryLatestPackageUseCase.Result latest = latestPackages.computeIfAbsent(projectId, this::defaultPackage);
            return Optional.of(new CreateMailDraftUseCase.LatestPackage(latest.packageId(), latest.filename()));
        }

        @Override
        public MailDraft save(MailDraft draft) {
            drafts.put("draft-001", draft);
            return draft;
        }

        @Override
        public Optional<MailDraft> findById(String draftId) {
            return Optional.ofNullable(drafts.get(draftId));
        }

        @Override
        public void sendDraft(String draftId, MailDraft draft) {
            throw new IllegalStateException("邮箱服务未配置，无法发送邮件，请先配置邮箱服务或人工下载附件发送");
        }

        @Override
        public void record(String projectId, String actorId, OperationAction action, String targetId) {
        }

        @Override
        public void publishToGroup(String projectId, NotificationEventType type) {
        }

        @Override
        public PackageArtifact save(PackageArtifact artifact) {
            latestPackages.put(artifact.projectId(), new QueryLatestPackageUseCase.Result(
                    artifact.id(),
                    artifact.fileName(),
                    artifact.format().extension().replaceFirst("^\\.", ""),
                    artifact.createdAt(),
                    artifact.size()
            ));
            return artifact;
        }

        @Override
        public void markAsLatest(String projectId, String packageId) {
        }

        private QueryLatestPackageUseCase.Result latestPackageSummary(String projectId) {
            return latestPackages.computeIfAbsent(projectId, this::defaultPackage);
        }

        private String latestFilename(String projectId) {
            return latestPackageSummary(projectId).filename();
        }

        private QueryLatestPackageUseCase.Result defaultPackage(String projectId) {
            return new QueryLatestPackageUseCase.Result(
                    DEFAULT_PACKAGE_ID,
                    DEFAULT_PACKAGE_FILENAME,
                    PackageFormat.ZIP.extension().replaceFirst("^\\.", ""),
                    Instant.now(),
                    0L
            );
        }
    }

    /**
     * 运行期压缩快照仓储。
     */
    static final class RuntimePackageSnapshotRepository implements PackageSnapshotRepository {

        @Override
        public List<com.cooperation.application.packageartifact.PackageSourceEntry> findSnapshotEntries(String projectId) {
            return List.of();
        }

        @Override
        public Instant snapshotCreatedAt(String projectId) {
            return Instant.now();
        }
    }

    /**
     * 运行期清理目标仓储。
     */
    static final class RuntimeCleanupTargetRepository implements CleanupTargetRepository {

        @Override
        public Optional<FileAsset> findActiveFileByProjectIdAndPath(String projectId, String path) {
            return Optional.empty();
        }

        @Override
        public FileAsset save(FileAsset fileAsset) {
            return fileAsset;
        }

        private String filenameOf(String path) {
            int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
            return slash >= 0 ? path.substring(slash + 1) : path;
        }
    }

    /**
     * 运行期通知查询端口。
     */
    static final class RuntimeNotificationPort implements ListNotificationsUseCase.NotificationPort {

        @Override
        public List<ListNotificationsUseCase.NotificationItem> listByRecipient(Long recipientId, Optional<String> projectId, Optional<Boolean> read) {
            return List.of();
        }

        @Override
        public Optional<ListNotificationsUseCase.NotificationItem> findById(String notificationId) {
            return Optional.empty();
        }

        @Override
        public ListNotificationsUseCase.NotificationItem save(ListNotificationsUseCase.NotificationItem notification) {
            return notification;
        }
    }

    /**
     * 运行期搜索仓储。
     */
    static final class RuntimeSearchRepository implements SearchUseCase.SearchRepository {

        @Override
        public List<SearchUseCase.ProjectHit> searchProjects(Long userId, String keyword) {
            return List.of();
        }

        @Override
        public List<SearchUseCase.FileHit> searchFiles(Long userId, String keyword) {
            return List.of();
        }

        @Override
        public List<SearchUseCase.MemberHit> searchMembers(Long userId, String keyword) {
            return List.of();
        }
    }

    private static Long parseLong(String value, Long fallback) {
        try {
            return Long.valueOf(value);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
