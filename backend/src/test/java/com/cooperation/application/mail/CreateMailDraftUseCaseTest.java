package com.cooperation.application.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.mail.MailDraft;
import com.cooperation.domain.notification.NotificationEventType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 创建邮件草稿应用用例测试。
 */
class CreateMailDraftUseCaseTest {

    /**
     * 验证用例会基于最近压缩包创建草稿，并写入记录和全组通知。
     */
    @Test
    void shouldCreateDraftFromLatestPackageAndNotifyGroupMembers() {
        FakePackageRepository packageRepository = new FakePackageRepository();
        packageRepository.latestPackage = Optional.of(new CreateMailDraftUseCase.LatestPackage("package-1", "project-final.zip"));
        FakeMailDraftRepository draftRepository = new FakeMailDraftRepository();
        FakeOperationLogWriter logWriter = new FakeOperationLogWriter();
        FakeNotificationPublisher notificationPublisher = new FakeNotificationPublisher();
        CreateMailDraftUseCase useCase = new CreateMailDraftUseCase(
                packageRepository,
                draftRepository,
                logWriter,
                notificationPublisher
        );

        CreateMailDraftUseCase.Result result = useCase.handle(new CreateMailDraftUseCase.Command(
                "project-1",
                "owner-1",
                List.of("teacher@example.com"),
                "分工协作成果提交",
                "附件为本次项目最终压缩包。"
        ));

        assertEquals("package-1", result.draft().getPackageId());
        assertEquals("project-final.zip", result.attachmentFileName());
        assertEquals(List.of("teacher@example.com"), result.draft().getRecipients());
        assertEquals("project-1", draftRepository.savedDraft.getProjectId());
        assertEquals(OperationAction.MAIL_DRAFT_CREATED, logWriter.records.get(0).action());
        assertEquals(NotificationEventType.MAIL_DRAFT_CREATED, notificationPublisher.events.get(0).type());
    }

    /**
     * 验证没有最近压缩包时不会创建草稿。
     */
    @Test
    void shouldRejectCreateDraftWhenLatestPackageMissing() {
        FakePackageRepository packageRepository = new FakePackageRepository();
        FakeMailDraftRepository draftRepository = new FakeMailDraftRepository();
        CreateMailDraftUseCase useCase = new CreateMailDraftUseCase(
                packageRepository,
                draftRepository,
                new FakeOperationLogWriter(),
                new FakeNotificationPublisher()
        );

        assertThrows(IllegalStateException.class, () -> useCase.handle(new CreateMailDraftUseCase.Command(
                "project-1",
                "owner-1",
                List.of("teacher@example.com"),
                "分工协作成果提交",
                "附件为本次项目最终压缩包。"
        )));
        assertEquals(null, draftRepository.savedDraft);
    }

    /**
     * 操作记录只暴露测试断言需要的字段。
     */
    private record LogRecord(String projectId, String actorId, OperationAction action, String targetId) {
    }

    /**
     * 通知事件只暴露测试断言需要的字段。
     */
    private record NotificationEvent(String projectId, NotificationEventType type) {
    }

    /**
     * 压缩包仓储假实现，避免依赖数据库。
     */
    private static final class FakePackageRepository implements CreateMailDraftUseCase.PackageRepository {
        private Optional<CreateMailDraftUseCase.LatestPackage> latestPackage = Optional.empty();

        @Override
        public Optional<CreateMailDraftUseCase.LatestPackage> findLatestUsableByProjectId(String projectId) {
            return latestPackage;
        }
    }

    /**
     * 邮件草稿仓储假实现，记录保存结果。
     */
    private static final class FakeMailDraftRepository implements CreateMailDraftUseCase.MailDraftRepository {
        private MailDraft savedDraft;

        @Override
        public MailDraft save(MailDraft draft) {
            this.savedDraft = draft;
            return draft;
        }
    }

    /**
     * 操作记录端口假实现，记录用例写入动作。
     */
    private static final class FakeOperationLogWriter implements CreateMailDraftUseCase.OperationLogWriter {
        private final List<LogRecord> records = new ArrayList<>();

        @Override
        public void record(String projectId, String actorId, OperationAction action, String targetId) {
            records.add(new LogRecord(projectId, actorId, action, targetId));
        }
    }

    /**
     * 通知端口假实现，记录用例发布事件。
     */
    private static final class FakeNotificationPublisher implements CreateMailDraftUseCase.NotificationPublisher {
        private final List<NotificationEvent> events = new ArrayList<>();

        @Override
        public void publishToGroup(String projectId, NotificationEventType type) {
            events.add(new NotificationEvent(projectId, type));
        }
    }
}
