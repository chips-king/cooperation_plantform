package com.cooperation.domain.directory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 目录状态领域规则测试。
 */
class DirectoryStatusTest {

    /**
     * 验证目录状态枚举值用于持久化，中文展示值用于页面呈现。
     */
    @Test
    void statusShouldExposePersistedValueAndChineseDisplayName() {
        assertEquals("not_started", DirectoryStatus.NOT_STARTED.getValue());
        assertEquals("未开始", DirectoryStatus.NOT_STARTED.getDisplayName());
        assertEquals("in_progress", DirectoryStatus.IN_PROGRESS.getValue());
        assertEquals("进行中", DirectoryStatus.IN_PROGRESS.getDisplayName());
        assertEquals("completed", DirectoryStatus.COMPLETED.getValue());
        assertEquals("已完成", DirectoryStatus.COMPLETED.getDisplayName());
    }

    /**
     * 验证新建目录默认处于未开始状态。
     */
    @Test
    void newDirectoryShouldBeNotStartedByDefault() {
        DirectoryNode directory = DirectoryNode.create(1L, null, "需求文档", 10L);

        assertEquals(DirectoryStatus.NOT_STARTED, directory.getStatus());
    }

    /**
     * 验证目录可以从未开始进入进行中，再进入已完成。
     */
    @Test
    void shouldMoveFromNotStartedToInProgressThenCompleted() {
        DirectoryNode directory = DirectoryNode.create(1L, null, "需求文档", 10L);

        directory.changeStatus(DirectoryStatus.IN_PROGRESS, 20L);
        directory.changeStatus(DirectoryStatus.COMPLETED, 20L);

        assertEquals(DirectoryStatus.COMPLETED, directory.getStatus());
    }

    /**
     * 验证目录可以从已完成退回进行中，便于成员修正任务进度。
     */
    @Test
    void completedDirectoryCanReturnToInProgress() {
        DirectoryNode directory = DirectoryNode.create(1L, null, "需求文档", 10L);
        directory.changeStatus(DirectoryStatus.IN_PROGRESS, 20L);
        directory.changeStatus(DirectoryStatus.COMPLETED, 20L);

        directory.changeStatus(DirectoryStatus.IN_PROGRESS, 20L);

        assertEquals(DirectoryStatus.IN_PROGRESS, directory.getStatus());
    }

    /**
     * 验证跳过进行中直接完成会被拒绝，避免误标未开始任务。
     */
    @Test
    void notStartedDirectoryShouldNotBeCompletedDirectly() {
        DirectoryNode directory = DirectoryNode.create(1L, null, "需求文档", 10L);

        assertThrows(IllegalStateException.class, () -> directory.changeStatus(DirectoryStatus.COMPLETED, 20L));
        assertEquals(DirectoryStatus.NOT_STARTED, directory.getStatus());
    }
}
