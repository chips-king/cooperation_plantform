package com.cooperation.domain.project;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 项目状态领域规则测试。
 */
class ProjectStatusTest {

    /**
     * 验证新建项目默认处于协作中状态。
     */
    @Test
    void newProjectShouldBeActiveByDefault() {
        Project project = Project.create(1L, 10L, "课程设计协作");

        assertEquals(ProjectStatus.ACTIVE, project.getStatus());
        assertTrue(project.canWrite(), "协作中项目应允许成员执行写操作");
    }

    /**
     * 验证项目结束后进入已结束状态，并拒绝后续写操作。
     */
    @Test
    void endShouldLockWriteOperations() {
        Project project = Project.create(1L, 10L, "课程设计协作");

        project.end(10L);

        assertEquals(ProjectStatus.ENDED, project.getStatus());
        assertFalse(project.canWrite(), "已结束项目应锁定成员写操作");
        assertThrows(IllegalStateException.class, () -> project.checkWritable());
    }

    /**
     * 验证已结束项目可以被负责人重新打开并恢复协作。
     */
    @Test
    void reopenShouldRestoreActiveStatusAndWritableState() {
        Project project = Project.create(1L, 10L, "课程设计协作");
        project.end(10L);

        project.reopen(10L);

        assertEquals(ProjectStatus.ACTIVE, project.getStatus());
        assertTrue(project.canWrite(), "重新打开后项目应恢复写操作能力");
    }

    /**
     * 验证重复结束项目会被拒绝，避免重复写入结束语义。
     */
    @Test
    void repeatedEndShouldBeRejected() {
        Project project = Project.create(1L, 10L, "课程设计协作");
        project.end(10L);

        assertThrows(IllegalStateException.class, () -> project.end(10L));
        assertEquals(ProjectStatus.ENDED, project.getStatus());
    }
}
