package com.cooperation.domain.file;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 文件名值对象领域规则测试。
 */
class FileNameTest {

    /**
     * 验证空文件名会被拒绝，避免生成无法展示或落盘的文件记录。
     */
    @Test
    void shouldRejectBlankFileName() {
        assertThatThrownBy(() -> FileName.of(""))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> FileName.of("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * 验证路径穿越片段会被拒绝，防止文件名绕过项目目录边界。
     */
    @Test
    void shouldRejectPathTraversalFileName() {
        assertThatThrownBy(() -> FileName.of("../report.docx"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> FileName.of("..\\report.docx"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * 验证路径分隔符会被拒绝，文件名只表达展示名而不携带路径。
     */
    @Test
    void shouldRejectFileNameWithPathSeparator() {
        assertThatThrownBy(() -> FileName.of("docs/report.docx"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> FileName.of("docs\\report.docx"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * 验证控制字符会被拒绝，避免破坏日志、响应头或文件系统语义。
     */
    @Test
    void shouldRejectFileNameWithControlCharacter() {
        assertThatThrownBy(() -> FileName.of("report\n.docx"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> FileName.of("report\u0000.docx"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * 验证合法文件名通过校验，并保留用户上传时的原始展示名。
     */
    @Test
    void shouldKeepOriginalDisplayNameWhenFileNameIsValid() {
        FileName fileName = FileName.of("阶段报告 v1.0.docx");

        assertThat(fileName.value()).isEqualTo("阶段报告 v1.0.docx");
    }
}
