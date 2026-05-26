package com.cooperation.application.log;

import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.permission.RoleTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 操作记录列表用例测试。
 */
class ListOperationLogsUseCaseTest {

    /**
     * 验证普通成员可以查看项目内全部操作记录。
     */
    @Test
    @DisplayName("成员可以查看项目全部操作记录")
    void shouldAllowMemberToListProjectOperationLogs() {
        FakeOperationLogPort logPort = new FakeOperationLogPort();
        FakeProjectAccessPort accessPort = new FakeProjectAccessPort(RoleTemplate.MEMBER);
        ListOperationLogsUseCase useCase = new ListOperationLogsUseCase(logPort, accessPort);

        ListOperationLogsUseCase.Result result = useCase.handle(ListOperationLogsUseCase.Query.all(100L, "project-1"));

        assertEquals(3, result.logs().size());
        assertEquals(OperationAction.FILE_UPLOAD, result.logs().get(0).action());
    }

    /**
     * 验证只读用户不能访问项目操作记录。
     */
    @Test
    @DisplayName("只读用户不可查看操作记录")
    void shouldRejectReadOnlyUserWhenListOperationLogs() {
        FakeOperationLogPort logPort = new FakeOperationLogPort();
        FakeProjectAccessPort accessPort = new FakeProjectAccessPort(RoleTemplate.READ_ONLY);
        ListOperationLogsUseCase useCase = new ListOperationLogsUseCase(logPort, accessPort);

        assertThrows(AccessDeniedException.class, () -> useCase.handle(ListOperationLogsUseCase.Query.all(100L, "project-1")));
    }

    /**
     * 验证操作记录支持按项目、类型、操作人和时间范围组合筛选。
     */
    @Test
    @DisplayName("按项目类型操作人和时间筛选操作记录")
    void shouldFilterOperationLogsByProjectTypeActorAndTime() {
        FakeOperationLogPort logPort = new FakeOperationLogPort();
        FakeProjectAccessPort accessPort = new FakeProjectAccessPort(RoleTemplate.MEMBER);
        ListOperationLogsUseCase useCase = new ListOperationLogsUseCase(logPort, accessPort);

        ListOperationLogsUseCase.Result result = useCase.handle(new ListOperationLogsUseCase.Query(
                100L,
                "project-1",
                Optional.of(OperationAction.FILE_DELETE),
                Optional.of(201L),
                Optional.of(Instant.parse("2026-05-24T08:00:00Z")),
                Optional.of(Instant.parse("2026-05-24T10:00:00Z"))
        ));

        assertEquals(List.of("删除旧版报告"), result.logs().stream().map(ListOperationLogsUseCase.LogItem::summary).toList());
        assertEquals(201L, result.logs().get(0).actorId());
    }

    /**
     * 操作记录内存假端口，表达应用层需要的查询能力。
     */
    private static final class FakeOperationLogPort implements ListOperationLogsUseCase.OperationLogPort {

        private final List<ListOperationLogsUseCase.LogItem> logs = List.of(
                log("log-1", "project-1", 100L, OperationAction.FILE_UPLOAD, "上传结题报告", "2026-05-24T07:30:00Z"),
                log("log-2", "project-1", 201L, OperationAction.FILE_DELETE, "删除旧版报告", "2026-05-24T09:00:00Z"),
                log("log-3", "project-1", 100L, OperationAction.MAIL_SENT, "发送最终邮件", "2026-05-24T11:00:00Z"),
                log("log-4", "project-2", 201L, OperationAction.FILE_DELETE, "删除其他项目文件", "2026-05-24T09:30:00Z")
        );

        @Override
        public List<ListOperationLogsUseCase.LogItem> listByProject(ListOperationLogsUseCase.Query query) {
            return logs.stream()
                    .filter(log -> log.projectId().equals(query.projectId()))
                    .filter(log -> query.action().map(value -> value == log.action()).orElse(true))
                    .filter(log -> query.actorId().map(value -> value.equals(log.actorId())).orElse(true))
                    .filter(log -> query.from().map(value -> !log.createdAt().isBefore(value)).orElse(true))
                    .filter(log -> query.to().map(value -> !log.createdAt().isAfter(value)).orElse(true))
                    .toList();
        }

        /**
         * 创建操作记录列表项。
         */
        private static ListOperationLogsUseCase.LogItem log(
                String id,
                String projectId,
                Long actorId,
                OperationAction action,
                String summary,
                String createdAt
        ) {
            return new ListOperationLogsUseCase.LogItem(
                    id,
                    projectId,
                    actorId,
                    action,
                    "file",
                    "file-1",
                    summary,
                    Instant.parse(createdAt)
            );
        }
    }

    /**
     * 项目访问内存假端口，返回当前用户在项目内的角色模板。
     */
    private record FakeProjectAccessPort(RoleTemplate roleTemplate) implements ListOperationLogsUseCase.ProjectAccessPort {

        @Override
        public RoleTemplate roleOf(Long userId, String projectId) {
            return roleTemplate;
        }
    }
}
