package com.cooperation.application.log;

import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.log.OperationLog;
import com.cooperation.domain.log.OperationLogRepository;
import com.cooperation.domain.permission.RoleTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 操作记录查询用例测试。
 */
class QueryOperationLogUseCaseTest {

    /**
     * 验证成员可查看项目全部操作记录。
     */
    @Test
    @DisplayName("成员可以查询项目操作记录")
    void shouldAllowMemberToQueryOperationLogs() {
        FakeOperationLogRepository logRepository = new FakeOperationLogRepository();
        FakeProjectPermissionRepository permissionRepository = new FakeProjectPermissionRepository(RoleTemplate.MEMBER);
        QueryOperationLogUseCase useCase = new QueryOperationLogUseCase(logRepository, permissionRepository);

        QueryOperationLogUseCase.Result result = useCase.execute(QueryOperationLogUseCase.Query.all(100L, "project-1"));

        assertEquals(3, result.logs().size());
        assertEquals(OperationAction.FILE_UPLOAD, result.logs().get(0).action());
    }

    /**
     * 验证只读用户没有操作记录查看权限。
     */
    @Test
    @DisplayName("只读用户不可查询操作记录")
    void shouldRejectReadOnlyUserWhenQueryOperationLogs() {
        FakeOperationLogRepository logRepository = new FakeOperationLogRepository();
        FakeProjectPermissionRepository permissionRepository = new FakeProjectPermissionRepository(RoleTemplate.READ_ONLY);
        QueryOperationLogUseCase useCase = new QueryOperationLogUseCase(logRepository, permissionRepository);

        assertThrows(AccessDeniedException.class, () -> useCase.execute(QueryOperationLogUseCase.Query.all(100L, "project-1")));
    }

    /**
     * 验证操作记录支持动作、操作人和时间范围筛选。
     */
    @Test
    @DisplayName("支持按类型操作人和时间筛选")
    void shouldFilterOperationLogsByActionActorAndTimeRange() {
        FakeOperationLogRepository logRepository = new FakeOperationLogRepository();
        FakeProjectPermissionRepository permissionRepository = new FakeProjectPermissionRepository(RoleTemplate.MEMBER);
        QueryOperationLogUseCase useCase = new QueryOperationLogUseCase(logRepository, permissionRepository);
        QueryOperationLogUseCase.Query query = new QueryOperationLogUseCase.Query(
                100L,
                "project-1",
                Optional.of(OperationAction.FILE_DELETE),
                Optional.of("member-2"),
                Optional.of(Instant.parse("2026-05-24T08:00:00Z")),
                Optional.of(Instant.parse("2026-05-24T10:00:00Z"))
        );

        QueryOperationLogUseCase.Result result = useCase.execute(query);

        assertEquals(1, result.logs().size());
        assertEquals("删除了旧文件", result.logs().get(0).summary());
    }

    /**
     * 操作记录测试内存仓储，避免依赖数据库。
     */
    private static final class FakeOperationLogRepository implements OperationLogRepository {

        private final List<OperationLog> logs = new ArrayList<>(List.of(
                log(OperationAction.FILE_UPLOAD, "member-1", "上传了报告", "2026-05-24T07:00:00Z"),
                log(OperationAction.FILE_DELETE, "member-2", "删除了旧文件", "2026-05-24T09:00:00Z"),
                log(OperationAction.MAIL_SENT, "owner-1", "发送了邮件", "2026-05-24T11:00:00Z")
        ));

        @Override
        public OperationLog save(OperationLog operationLog) {
            logs.add(operationLog);
            return operationLog;
        }

        @Override
        public List<OperationLog> findByProjectId(String projectId) {
            return logs.stream().filter(log -> log.getProjectId().equals(projectId)).toList();
        }

        @Override
        public List<OperationLog> findByProjectIdAndAction(String projectId, OperationAction action) {
            return logs.stream()
                    .filter(log -> log.getProjectId().equals(projectId))
                    .filter(log -> log.getAction() == action)
                    .toList();
        }

        @Override
        public List<OperationLog> findByProjectIdAndActorId(String projectId, String actorId) {
            return logs.stream()
                    .filter(log -> log.getProjectId().equals(projectId))
                    .filter(log -> log.getActorId().equals(actorId))
                    .toList();
        }

        @Override
        public Optional<OperationLog> findById(String id) {
            return Optional.empty();
        }

        private static OperationLog log(OperationAction action, String actorId, String summary, String createdAt) {
            return OperationLog.record("project-1", actorId, action, "file", summary, summary, Map.of(), Instant.parse(createdAt));
        }
    }

    /**
     * 权限测试内存仓储，表达应用层读取项目角色模板的端口。
     */
    private record FakeProjectPermissionRepository(RoleTemplate roleTemplate) implements QueryOperationLogUseCase.ProjectPermissionRepository {

        @Override
        public RoleTemplate findRoleTemplate(Long userId, String projectId) {
            return roleTemplate;
        }
    }
}
