package com.cooperation.application.directory;

import com.cooperation.domain.directory.DirectoryNode;

/**
 * 更新目录状态结果。
 *
 * @param directory 已更新状态的目录实体。
 */
public record UpdateDirectoryStatusResult(DirectoryNode directory) {
}
