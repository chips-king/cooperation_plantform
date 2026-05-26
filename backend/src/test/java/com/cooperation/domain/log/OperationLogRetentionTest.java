package com.cooperation.domain.log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 操作记录保留期领域规则测试。
 */
class OperationLogRetentionTest {

    /**
     * 验证项目结束后操作记录默认保留三十天。
     */
    @Test
    void shouldRetainOperationLogsForThirtyDaysAfterProjectEnded() {
        OperationLog log = OperationLog.record(
                "project-1",
                "member-1",
                OperationAction.FILE_UPLOAD,
                "file",
                "file-1",
                "上传了成果文件",
                Map.of("path", "/docs/report.docx"),
                Instant.parse("2026-05-20T08:00:00Z")
        );
        Instant endedAt = Instant.parse("2026-05-24T10:00:00Z");

        log.applyProjectEndedRetention(endedAt);

        assertEquals(endedAt.plus(OperationLog.DEFAULT_RETENTION_AFTER_PROJECT_ENDED), log.getRetainUntil().orElseThrow());
    }

    /**
     * 验证未结束项目的操作记录不生成到期清理时间。
     */
    @Test
    void shouldNotCreateRetentionDeadlineForActiveProject() {
        OperationLog log = OperationLog.record(
                "project-1",
                "member-1",
                OperationAction.FILE_UPLOAD,
                "file",
                "file-1",
                "上传了成果文件",
                Map.of("path", "/docs/report.docx"),
                Instant.parse("2026-05-20T08:00:00Z")
        );

        assertTrue(log.getRetainUntil().isEmpty());
    }
}
