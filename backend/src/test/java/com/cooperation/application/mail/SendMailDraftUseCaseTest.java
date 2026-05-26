package com.cooperation.application.mail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.mail.MailDraft;
import com.cooperation.domain.mail.MailDraftStatus;
import com.cooperation.domain.notification.NotificationEventType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * 发送邮件草稿应用用例测试。
 */
class SendMailDraftUseCaseTest {

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-05-24T10:00:00Z"), ZoneOffset.UTC);

    /**
     * 验证发送成功后草稿进入已发送状态，并写入记录和全组通知。
     */
    @Test
    void shouldMarkDraftSentAndNotifyGroupWhenProviderSucceeds() {
        FakeMailDraftRepository draftRepository = new FakeMailDraftRepository();
        draftRepository.savedDraft = MailDraft.create(
                "project-1",
                "package-1",
                List.of("teacher@example.com"),
                "分工协作成果提交",
                "附件为本次项目最终压缩包。",
                "owner-1"
        );
        FakeMailProviderPort mailProviderPort = new FakeMailProviderPort();
        FakeOperationLogWriter logWriter = new FakeOperationLogWriter();
        FakeNotificationPublisher notificationPublisher = new FakeNotificationPublisher();
        SendMailDraftUseCase useCase = new SendMailDraftUseCase(
                draftRepository,
                mailProviderPort,
                logWriter,
                notificationPublisher,
                fixedClock
        );

        SendMailDraftUseCase.Result result = useCase.handle(new SendMailDraftUseCase.Command("draft-1", "owner-1", true));

        assertEquals(MailDraftStatus.SENT, result.draft().getStatus());
        assertEquals(fixedClock.instant(), draftRepository.savedDraft.getSentAt());
        assertEquals("邮件发送成功", result.message());
        assertEquals("draft-1", mailProviderPort.sentDraftIds.get(0));
        assertEquals(OperationAction.MAIL_SENT, logWriter.records.get(0).action());
        assertEquals(NotificationEventType.MAIL_SENT, notificationPublisher.events.get(0).type());
    }

    /**
     * 验证发送失败时草稿保持草稿状态，方便负责人修正后重试。
     */
    @Test
    void shouldKeepDraftStatusWhenProviderFails() {
        FakeMailDraftRepository draftRepository = new FakeMailDraftRepository();
        draftRepository.savedDraft = MailDraft.create(
                "project-1",
                "package-1",
                List.of("teacher@example.com"),
                "分工协作成果提交",
                "附件为本次项目最终压缩包。",
                "owner-1"
        );
        FakeMailProviderPort mailProviderPort = new FakeMailProviderPort();
        mailProviderPort.failureReason = "邮箱服务暂不可用";
        SendMailDraftUseCase useCase = new SendMailDraftUseCase(
                draftRepository,
                mailProviderPort,
                new FakeOperationLogWriter(),
                new FakeNotificationPublisher(),
                fixedClock
        );

        assertThrows(IllegalStateException.class, () -> useCase.handle(new SendMailDraftUseCase.Command("draft-1", "owner-1", true)));
        assertEquals(MailDraftStatus.DRAFT, draftRepository.savedDraft.getStatus());
        assertEquals("邮箱服务暂不可用", draftRepository.savedDraft.getLastFailureReason());
    }

    /**
     * 验证没有负责人确认时不会调用真实发送端口，避免误发邮件。
     */
    @Test
    void shouldRejectSendWhenOwnerHasNotConfirmed() {
        FakeMailDraftRepository draftRepository = new FakeMailDraftRepository();
        draftRepository.savedDraft = MailDraft.create(
                "project-1",
                "package-1",
                List.of("teacher@example.com"),
                "分工协作成果提交",
                "附件为本次项目最终压缩包。",
                "owner-1"
        );
        FakeMailProviderPort mailProviderPort = new FakeMailProviderPort();
        SendMailDraftUseCase useCase = new SendMailDraftUseCase(
                draftRepository,
                mailProviderPort,
                new FakeOperationLogWriter(),
                new FakeNotificationPublisher(),
                fixedClock
        );

        assertThrows(IllegalStateException.class, () -> useCase.handle(new SendMailDraftUseCase.Command("draft-1", "owner-1", false)));
        assertEquals(List.of(), mailProviderPort.sentDraftIds);
        assertEquals(MailDraftStatus.DRAFT, draftRepository.savedDraft.getStatus());
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
     * 邮件草稿仓储假实现，避免依赖数据库。
     */
    private static final class FakeMailDraftRepository implements SendMailDraftUseCase.MailDraftRepository {
        private MailDraft savedDraft;

        @Override
        public Optional<MailDraft> findById(String draftId) {
            return Optional.ofNullable(savedDraft);
        }

        @Override
        public MailDraft save(MailDraft draft) {
            this.savedDraft = draft;
            return draft;
        }
    }

    /**
     * 邮箱发送端口假实现，避免调用真实邮箱。
     */
    private static final class FakeMailProviderPort implements SendMailDraftUseCase.MailProviderPort {
        private final List<String> sentDraftIds = new ArrayList<>();
        private String failureReason;

        @Override
        public void sendDraft(String draftId, MailDraft draft) {
            if (failureReason != null) {
                throw new IllegalStateException(failureReason);
            }
            sentDraftIds.add(draftId);
        }
    }

    /**
     * 操作记录端口假实现，记录用例写入动作。
     */
    private static final class FakeOperationLogWriter implements SendMailDraftUseCase.OperationLogWriter {
        private final List<LogRecord> records = new ArrayList<>();

        @Override
        public void record(String projectId, String actorId, OperationAction action, String targetId) {
            records.add(new LogRecord(projectId, actorId, action, targetId));
        }
    }

    /**
     * 通知端口假实现，记录用例发布事件。
     */
    private static final class FakeNotificationPublisher implements SendMailDraftUseCase.NotificationPublisher {
        private final List<NotificationEvent> events = new ArrayList<>();

        @Override
        public void publishToGroup(String projectId, NotificationEventType type) {
            events.add(new NotificationEvent(projectId, type));
        }
    }
}
