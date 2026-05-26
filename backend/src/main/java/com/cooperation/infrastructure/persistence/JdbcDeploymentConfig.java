package com.cooperation.infrastructure.persistence;

import com.cooperation.application.group.GetGroupDetailUseCase;
import com.cooperation.application.group.Group;
import com.cooperation.application.group.GroupRepository;
import com.cooperation.application.group.ListGroupsUseCase;
import com.cooperation.application.invitation.Invitation;
import com.cooperation.application.invitation.InvitationRepository;
import com.cooperation.application.file.ListDirectoryTreeUseCase;
import com.cooperation.application.mail.CreateMailDraftUseCase;
import com.cooperation.application.mail.QueryMailDraftUseCase;
import com.cooperation.application.mail.SendMailDraftUseCase;
import com.cooperation.application.mail.UpdateMailDraftUseCase;
import com.cooperation.application.member.Membership;
import com.cooperation.application.member.MembershipRepository;
import com.cooperation.application.packageartifact.DownloadLatestPackageUseCase;
import com.cooperation.application.packageartifact.PackageArtifact;
import com.cooperation.application.packageartifact.PackageArtifactRepository;
import com.cooperation.application.packageartifact.PackageSourceEntry;
import com.cooperation.application.packageartifact.PackageSnapshotRepository;
import com.cooperation.application.packageartifact.ProjectPackageSnapshotRepository;
import com.cooperation.application.packageartifact.QueryLatestPackageUseCase;
import com.cooperation.domain.check.ProjectFileTree;
import com.cooperation.domain.check.CheckTarget;
import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.mail.MailDraft;
import com.cooperation.domain.notification.NotificationEventType;
import com.cooperation.domain.packageartifact.PackageFormat;
import com.cooperation.domain.permission.PermissionSet;
import com.cooperation.domain.permission.RoleTemplate;
import com.cooperation.infrastructure.mail.MailSmtpProperties;
import com.cooperation.infrastructure.mail.SmtpMailDraftSender;
import com.cooperation.infrastructure.storage.StorageProperties;
import com.cooperation.web.file.FileDto;
import com.cooperation.web.group.GroupDto.GroupResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

/**
 * 部署态 JDBC 适配配置，运行态数据只落 MySQL，不使用内存仓储保存业务数据。
 */
@Configuration
public class JdbcDeploymentConfig {

    @Bean
    @ConditionalOnMissingBean
    @Primary
    public GroupRepository jdbcGroupRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcGroupRepository(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    @Primary
    public MembershipRepository jdbcMembershipRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcMembershipRepository(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public ListGroupsUseCase jdbcListGroupsUseCase(JdbcTemplate jdbcTemplate) {
        return query -> {
            int page = Math.max(query.page(), 1);
            int size = Math.max(query.size(), 1);
            int offset = (page - 1) * size;
            Long total = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM user_groups g
                    JOIN memberships m ON m.group_id = g.id AND m.project_id IS NULL
                    WHERE m.user_id = ? AND m.status = 'active'
                    """, Long.class, query.actorId());
            List<GroupResponse> items = jdbcTemplate.query("""
                    SELECT g.id, g.name, g.owner_id, g.status
                    FROM user_groups g
                    JOIN memberships m ON m.group_id = g.id AND m.project_id IS NULL
                    WHERE m.user_id = ? AND m.status = 'active'
                    ORDER BY g.id DESC
                    LIMIT ? OFFSET ?
                    """, (rs, row) -> new GroupResponse(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getLong("owner_id"),
                    rs.getString("status")
            ), query.actorId(), size, offset);
            return new ListGroupsUseCase.Result(items, page, size, total == null ? 0 : total);
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public GetGroupDetailUseCase jdbcGetGroupDetailUseCase(JdbcTemplate jdbcTemplate) {
        return query -> jdbcTemplate.queryForObject("""
                SELECT g.id, g.name, g.owner_id, g.status
                FROM user_groups g
                JOIN memberships m ON m.group_id = g.id AND m.project_id IS NULL
                WHERE g.id = ? AND m.user_id = ? AND m.status = 'active'
                """, (rs, row) -> new GroupResponse(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getLong("owner_id"),
                rs.getString("status")
        ), query.groupId(), query.actorId());
    }

    @Bean
    @Primary
    public ListDirectoryTreeUseCase jdbcListDirectoryTreeUseCase(JdbcTemplate jdbcTemplate) {
        return projectId -> buildDirectoryTree(projectId, jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    @Primary
    public PackageSnapshotRepository jdbcPackageSnapshotRepository(JdbcTemplate jdbcTemplate) {
        JdbcPackageStore store = new JdbcPackageStore(jdbcTemplate);
        return new PackageSnapshotRepository() {
            @Override
            public List<PackageSourceEntry> findSnapshotEntries(String projectId) {
                return store.findSnapshotEntries(projectId);
            }

            @Override
            public Instant snapshotCreatedAt(String projectId) {
                return store.snapshotCreatedAt(projectId);
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public ProjectPackageSnapshotRepository jdbcProjectPackageSnapshotRepository(JdbcTemplate jdbcTemplate) {
        return projectId -> new ProjectFileTree(jdbcTemplate.query("""
                SELECT name, storage_key, size, status
                FROM file_assets
                WHERE project_id = ? AND status = 'active'
                ORDER BY name ASC
                """, (rs, row) -> CheckTarget.file(
                rs.getString("name"),
                rs.getLong("size")
        ), parseLong(projectId, "项目标识")));
    }

    @Bean
    @ConditionalOnMissingBean
    @Primary
    public PackageArtifactRepository jdbcPackageArtifactRepository(JdbcTemplate jdbcTemplate) {
        JdbcPackageStore store = new JdbcPackageStore(jdbcTemplate);
        return new PackageArtifactRepository() {
            @Override
            public PackageArtifact save(PackageArtifact artifact) {
                return store.save(artifact);
            }

            @Override
            public void markAsLatest(String projectId, String packageId) {
                store.markAsLatest(projectId, packageId);
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public QueryLatestPackageUseCase jdbcQueryLatestPackageUseCase(JdbcTemplate jdbcTemplate) {
        JdbcPackageStore store = new JdbcPackageStore(jdbcTemplate);
        return query -> store.findLatestPackage(query.projectId())
                .orElseThrow(() -> new IllegalStateException("项目没有可用的最近压缩包"));
    }

    @Bean
    @ConditionalOnMissingBean
    public DownloadLatestPackageUseCase jdbcDownloadLatestPackageUseCase(JdbcTemplate jdbcTemplate, StorageProperties storageProperties) {
        JdbcPackageStore store = new JdbcPackageStore(jdbcTemplate);
        return command -> {
            QueryLatestPackageUseCase.Result latest = store.findLatestPackage(command.projectId())
                    .orElseThrow(() -> new IllegalStateException("项目没有可用的最近压缩包"));
            String storageKey = jdbcTemplate.queryForObject(
                    "SELECT storage_key FROM package_artifacts WHERE id = ?",
                    String.class,
                    latest.packageId()
            );
            Path root = storageProperties.getRoot().toAbsolutePath().normalize();
            Path file = root.resolve(storageKey).toAbsolutePath().normalize();
            if (!file.startsWith(root) || !Files.isRegularFile(file)) {
                throw new IllegalStateException("压缩包文件不存在，请重新生成");
            }
            try {
                return new DownloadLatestPackageUseCase.Result(latest.filename(), "application/zip", Files.readAllBytes(file));
            } catch (java.io.IOException exception) {
                throw new IllegalStateException("读取压缩包失败", exception);
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public CreateMailDraftUseCase jdbcCreateMailDraftUseCase(JdbcTemplate jdbcTemplate) {
        JdbcMailStore store = new JdbcMailStore(jdbcTemplate);
        return new CreateMailDraftUseCase(store, store, store, store);
    }

    @Bean
    @ConditionalOnMissingBean
    public SendMailDraftUseCase jdbcSendMailDraftUseCase(
            JdbcTemplate jdbcTemplate,
            MailSmtpProperties mailProperties,
            StorageProperties storageProperties,
            java.time.Clock clock
    ) {
        JdbcMailStore store = new JdbcMailStore(jdbcTemplate);
        return new SendMailDraftUseCase(
                store,
                new SmtpMailDraftSender(mailProperties, storageProperties, jdbcTemplate),
                store,
                store,
                clock
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public QueryMailDraftUseCase jdbcQueryMailDraftUseCase(JdbcTemplate jdbcTemplate) {
        JdbcMailStore store = new JdbcMailStore(jdbcTemplate);
        return query -> store.query(query.draftId());
    }

    @Bean
    @ConditionalOnMissingBean
    public UpdateMailDraftUseCase jdbcUpdateMailDraftUseCase(JdbcTemplate jdbcTemplate) {
        JdbcMailStore store = new JdbcMailStore(jdbcTemplate);
        return command -> store.update(command);
    }

    private static long parseLong(String value, String fieldName) {
        try {
            return Long.parseLong(value);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(fieldName + "必须是数字", exception);
        }
    }

    private static FileDto.DirectoryTreeResponse buildDirectoryTree(String projectId, JdbcTemplate jdbcTemplate) {
        long projectKey = parseLong(projectId, "项目标识");
        Map<String, MutableDirectoryNode> nodes = new LinkedHashMap<>();
        jdbcTemplate.query("""
                SELECT id, parent_id, name, status
                FROM directories
                WHERE project_id = ?
                ORDER BY COALESCE(parent_id, 0), id
                """, rs -> {
            String id = String.valueOf(rs.getLong("id"));
            Long parentId = rs.getObject("parent_id", Long.class);
            nodes.put(id, new MutableDirectoryNode(
                    id,
                    parentId == null ? null : String.valueOf(parentId),
                    rs.getString("name"),
                    rs.getString("status")
            ));
        }, projectKey);

        jdbcTemplate.query("""
                SELECT id, directory_id, name, size, mime_type, version_no, status
                FROM file_assets
                WHERE project_id = ? AND status = 'active'
                ORDER BY directory_id, name, version_no DESC
                """, rs -> {
            String directoryId = String.valueOf(rs.getLong("directory_id"));
            MutableDirectoryNode node = nodes.get(directoryId);
            if (node != null) {
                node.files.add(new FileDto.FileItemResponse(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getLong("size"),
                        rs.getString("mime_type"),
                        rs.getInt("version_no"),
                        rs.getString("status")
                ));
            }
        }, projectKey);

        List<MutableDirectoryNode> roots = new ArrayList<>();
        for (MutableDirectoryNode node : nodes.values()) {
            if (node.parentId == null || !nodes.containsKey(node.parentId)) {
                roots.add(node);
            } else {
                nodes.get(node.parentId).children.add(node);
            }
        }

        return new FileDto.DirectoryTreeResponse(
                projectId,
                roots.stream().map(MutableDirectoryNode::toResponse).toList()
        );
    }

    private static final class MutableDirectoryNode {
        private final String id;
        private final String parentId;
        private final String name;
        private final String status;
        private final List<FileDto.FileItemResponse> files = new ArrayList<>();
        private final List<MutableDirectoryNode> children = new ArrayList<>();

        private MutableDirectoryNode(String id, String parentId, String name, String status) {
            this.id = id;
            this.parentId = parentId;
            this.name = name;
            this.status = status;
        }

        private FileDto.DirectoryTreeResponse.DirectoryNodeResponse toResponse() {
            return new FileDto.DirectoryTreeResponse.DirectoryNodeResponse(
                    id,
                    parentId,
                    name,
                    status,
                    files,
                    children.stream().map(MutableDirectoryNode::toResponse).toList()
            );
        }
    }

    static final class JdbcGroupRepository implements GroupRepository {

        private final JdbcTemplate jdbcTemplate;

        JdbcGroupRepository(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        @Override
        public Group save(Group group) {
            if (group.getId() != null) {
                jdbcTemplate.update("UPDATE user_groups SET name = ?, status = ? WHERE id = ?",
                        group.getName(), group.getStatus().name().toLowerCase(), group.getId());
                return group;
            }
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO user_groups (name, owner_id, status) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS
                );
                statement.setString(1, group.getName());
                statement.setLong(2, group.getOwnerId());
                statement.setString(3, group.getStatus().name().toLowerCase());
                return statement;
            }, keyHolder);
            return findById(Objects.requireNonNull(keyHolder.getKey()).longValue()).orElseThrow();
        }

        @Override
        public Optional<Group> findById(Long id) {
            return jdbcTemplate.query("""
                    SELECT id, owner_id, name, status FROM user_groups WHERE id = ?
                    """, (rs, row) -> Group.restore(
                    rs.getLong("id"),
                    rs.getLong("owner_id"),
                    rs.getString("name"),
                    Group.Status.ACTIVE
            ), id).stream().findFirst();
        }
    }

    static final class JdbcMembershipRepository implements MembershipRepository {

        private final JdbcTemplate jdbcTemplate;

        JdbcMembershipRepository(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        @Override
        public Membership save(Membership membership) {
            if (membership.getId() != null) {
                jdbcTemplate.update("UPDATE memberships SET role_template = ?, custom_permissions = ?, status = ? WHERE id = ?",
                        membership.getRoleTemplate().name(), toPermissionJson(membership), membership.getStatus().name().toLowerCase(), membership.getId());
                return membership;
            }
            KeyHolder keyHolder = new GeneratedKeyHolder();
            jdbcTemplate.update(connection -> {
                PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO memberships (user_id, group_id, project_id, role_template, custom_permissions, status)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """, Statement.RETURN_GENERATED_KEYS);
                statement.setLong(1, membership.getUserId());
                statement.setLong(2, membership.getGroupId());
                if (membership.getProjectId().isPresent()) {
                    statement.setLong(3, membership.getProjectId().get());
                } else {
                    statement.setObject(3, null);
                }
                statement.setString(4, membership.getRoleTemplate().name());
                statement.setString(5, toPermissionJson(membership));
                statement.setString(6, membership.getStatus().name().toLowerCase());
                return statement;
            }, keyHolder);
            return membership.withId(Objects.requireNonNull(keyHolder.getKey()).longValue());
        }

        @Override
        public Optional<Membership> findById(Long id) {
            return query("WHERE id = ?", id);
        }

        @Override
        public Optional<Membership> findByGroupIdAndUserId(Long groupId, Long userId) {
            return query("WHERE group_id = ? AND user_id = ? AND project_id IS NULL AND status = 'active'", groupId, userId);
        }

        @Override
        public Optional<Membership> findByProjectIdAndUserId(Long projectId, Long userId) {
            return query("WHERE project_id = ? AND user_id = ? AND status = 'active'", projectId, userId);
        }

        private Optional<Membership> query(String where, Object... args) {
            return jdbcTemplate.query("""
                    SELECT id, user_id, group_id, project_id, role_template FROM memberships
                    """ + where, (rs, row) -> {
                Long projectId = rs.getObject("project_id", Long.class);
                Membership membership = projectId == null
                        ? Membership.groupLevel(rs.getLong("user_id"), rs.getLong("group_id"), RoleTemplate.valueOf(rs.getString("role_template")))
                        : Membership.projectLevel(rs.getLong("user_id"), rs.getLong("group_id"), projectId, RoleTemplate.valueOf(rs.getString("role_template")));
                return membership.withId(rs.getLong("id"));
            }, args).stream().findFirst();
        }

        private String toPermissionJson(Membership membership) {
            return "[\"" + String.join("\",\"", membership.getCustomPermissions().asSet().stream().map(Enum::name).toList()) + "\"]";
        }
    }

    static final class JdbcPackageStore implements PackageSnapshotRepository, PackageArtifactRepository, CreateMailDraftUseCase.PackageRepository {

        private final JdbcTemplate jdbcTemplate;

        JdbcPackageStore(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        @Override
        public List<PackageSourceEntry> findSnapshotEntries(String projectId) {
            return jdbcTemplate.query("""
                    SELECT name, storage_key, size FROM file_assets
                    WHERE project_id = ? AND status = 'active'
                    ORDER BY name ASC
                    """, (rs, row) -> PackageSourceEntry.activeFile(
                    rs.getString("name"),
                    rs.getString("storage_key"),
                    rs.getLong("size")
            ), parseLong(projectId, "项目标识"));
        }

        @Override
        public Instant snapshotCreatedAt(String projectId) {
            return Instant.now();
        }

        @Override
        public PackageArtifact save(PackageArtifact artifact) {
            jdbcTemplate.update("""
                    INSERT INTO package_artifacts (id, project_id, filename, format, storage_key, size, created_by, created_at, snapshot_created_at, is_latest)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
                    """,
                    artifact.id(),
                    parseLong(artifact.projectId(), "项目标识"),
                    artifact.fileName(),
                    artifact.format().extension().replaceFirst("^\\.", ""),
                    artifact.storageKey(),
                    artifact.size(),
                    parseLong(artifact.createdBy(), "创建人"),
                    Timestamp.from(artifact.createdAt()),
                    Timestamp.from(Instant.now()));
            return artifact;
        }

        @Override
        public void markAsLatest(String projectId, String packageId) {
            jdbcTemplate.update("UPDATE package_artifacts SET is_latest = 0 WHERE project_id = ?", parseLong(projectId, "项目标识"));
            jdbcTemplate.update("UPDATE package_artifacts SET is_latest = 1 WHERE id = ?", packageId);
            jdbcTemplate.update("UPDATE projects SET latest_package_id = ? WHERE id = ?", packageId, parseLong(projectId, "项目标识"));
        }

        @Override
        public Optional<CreateMailDraftUseCase.LatestPackage> findLatestUsableByProjectId(String projectId) {
            return findLatestPackage(projectId).map(result -> new CreateMailDraftUseCase.LatestPackage(result.packageId(), result.filename()));
        }

        Optional<QueryLatestPackageUseCase.Result> findLatestPackage(String projectId) {
            return jdbcTemplate.query("""
                    SELECT id, filename, format, created_at, size
                    FROM package_artifacts
                    WHERE project_id = ? AND is_latest = 1
                    ORDER BY created_at DESC
                    LIMIT 1
                    """, (rs, row) -> new QueryLatestPackageUseCase.Result(
                    rs.getString("id"),
                    rs.getString("filename"),
                    rs.getString("format"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getLong("size")
            ), parseLong(projectId, "项目标识")).stream().findFirst();
        }
    }

    static final class JdbcMailStore implements
            CreateMailDraftUseCase.PackageRepository,
            CreateMailDraftUseCase.MailDraftRepository,
            CreateMailDraftUseCase.OperationLogWriter,
            CreateMailDraftUseCase.NotificationPublisher,
            SendMailDraftUseCase.MailDraftRepository,
            SendMailDraftUseCase.OperationLogWriter,
            SendMailDraftUseCase.NotificationPublisher {

        private final JdbcTemplate jdbcTemplate;
        private final JdbcPackageStore packageStore;

        JdbcMailStore(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
            this.packageStore = new JdbcPackageStore(jdbcTemplate);
        }

        @Override
        public Optional<CreateMailDraftUseCase.LatestPackage> findLatestUsableByProjectId(String projectId) {
            return packageStore.findLatestUsableByProjectId(projectId);
        }

        @Override
        public MailDraft save(MailDraft draft) {
            if (draft.getId() == null) {
                KeyHolder keyHolder = new GeneratedKeyHolder();
                jdbcTemplate.update(connection -> {
                    PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO mail_drafts (project_id, recipient, subject, body, package_id, attachment_filename, status, created_by, sent_at)
                        SELECT ?, ?, ?, ?, ?, filename, ?, ?, ?
                        FROM package_artifacts WHERE id = ?
                        """, Statement.RETURN_GENERATED_KEYS);
                    statement.setLong(1, parseLong(draft.getProjectId(), "项目标识"));
                    statement.setString(2, String.join(",", draft.getRecipients()));
                    statement.setString(3, draft.getSubject());
                    statement.setString(4, draft.getBody());
                    statement.setString(5, draft.getPackageId());
                    statement.setString(6, draft.getStatus().name().toLowerCase());
                    statement.setLong(7, parseLong(draft.getCreatedBy(), "创建人"));
                    statement.setTimestamp(8, draft.getSentAt() == null ? null : Timestamp.from(draft.getSentAt()));
                    statement.setString(9, draft.getPackageId());
                    return statement;
                }, keyHolder);
                return draft.withId(String.valueOf(Objects.requireNonNull(keyHolder.getKey()).longValue()));
            }

            jdbcTemplate.update("""
                        UPDATE mail_drafts
                        SET recipient = ?, subject = ?, body = ?, package_id = ?, attachment_filename = (
                            SELECT filename FROM package_artifacts WHERE id = ?
                        ), status = ?, sent_at = ?
                        WHERE id = ?
                        """,
                        String.join(",", draft.getRecipients()),
                        draft.getSubject(),
                        draft.getBody(),
                        draft.getPackageId(),
                        draft.getPackageId(),
                        draft.getStatus().name().toLowerCase(),
                        draft.getSentAt() == null ? null : Timestamp.from(draft.getSentAt()),
                        parseLong(draft.getId(), "草稿标识"));
            return draft;
        }

        @Override
        public Optional<MailDraft> findById(String draftId) {
            return jdbcTemplate.query("""
                    SELECT project_id, package_id, recipient, subject, body, created_by, status, sent_at
                    FROM mail_drafts WHERE id = ?
                    """, (rs, row) -> {
                MailDraft draft = MailDraft.create(
                        String.valueOf(rs.getLong("project_id")),
                        rs.getString("package_id"),
                        List.of(rs.getString("recipient").split(",")),
                        rs.getString("subject"),
                        rs.getString("body"),
                        String.valueOf(rs.getLong("created_by"))
                );
                Timestamp sentAt = rs.getTimestamp("sent_at");
                if ("sent".equals(rs.getString("status")) && sentAt != null) {
                    draft.markSent(sentAt.toInstant());
                }
                return draft.withId(draftId);
            }, parseLong(draftId, "草稿标识")).stream().findFirst();
        }

        QueryMailDraftUseCase.Result query(String draftId) {
            return jdbcTemplate.queryForObject("""
                    SELECT id, project_id, recipient, subject, body, package_id, attachment_filename, status, created_at, sent_at
                    FROM mail_drafts WHERE id = ?
                    """, (rs, row) -> new QueryMailDraftUseCase.Result(
                    String.valueOf(rs.getLong("id")),
                    String.valueOf(rs.getLong("project_id")),
                    List.of(rs.getString("recipient").split(",")),
                    rs.getString("subject"),
                    rs.getString("body"),
                    rs.getString("package_id"),
                    rs.getString("attachment_filename"),
                    rs.getString("status"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("sent_at") == null ? null : rs.getTimestamp("sent_at").toInstant()
            ), parseLong(draftId, "草稿标识"));
        }

        UpdateMailDraftUseCase.Result update(UpdateMailDraftUseCase.Command command) {
            jdbcTemplate.update("""
                    UPDATE mail_drafts
                    SET recipient = ?, subject = ?, body = ?, package_id = ?, attachment_filename = (
                        SELECT filename FROM package_artifacts WHERE id = ?
                    )
                    WHERE id = ? AND created_by = ? AND status = 'draft'
                    """,
                    String.join(",", command.recipients()),
                    command.subject(),
                    command.body(),
                    command.packageId(),
                    command.packageId(),
                    parseLong(command.draftId(), "草稿标识"),
                    parseLong(command.actorId(), "当前用户"));
            QueryMailDraftUseCase.Result result = query(command.draftId());
            return new UpdateMailDraftUseCase.Result(
                    result.draftId(),
                    result.projectId(),
                    result.recipients(),
                    result.subject(),
                    result.body(),
                    result.packageId(),
                    result.attachmentFilename(),
                    result.status(),
                    result.createdAt()
            );
        }

        @Override
        public void record(String projectId, String actorId, OperationAction action, String targetId) {
            jdbcTemplate.update("""
                    INSERT INTO operation_logs (project_id, actor_id, action, target_type, target_id, summary, metadata, created_at)
                    VALUES (?, ?, ?, ?, ?, ?, '{}', ?)
                    """,
                    parseLong(projectId, "项目标识"),
                    parseLong(actorId, "操作人"),
                    action.name(),
                    "mail",
                    targetId,
                    action == OperationAction.MAIL_SENT ? "发送邮件" : "创建邮件草稿",
                    Timestamp.from(Instant.now()));
        }

        @Override
        public void publishToGroup(String projectId, NotificationEventType type) {
        }
    }
}
