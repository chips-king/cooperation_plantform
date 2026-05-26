package com.cooperation.domain.check;

/**
 * 打包前检查项类型。
 */
public enum CheckIssueType {

    /** 项目目录为空，仅提醒负责人确认是否遗漏内容。 */
    EMPTY_DIRECTORY,

    /** 项目文件中存在压缩包，仅提醒负责人确认是否重复打包。 */
    ARCHIVE_FILE,

    /** 项目缺少 README 说明文档。 */
    MISSING_README,

    /** 缓存目录或缓存文件。 */
    CACHE_FILE,

    /** 临时文件或备份文件。 */
    TEMPORARY_FILE,

    /** 日志文件。 */
    LOG_FILE,

    /** 系统自动生成的无关文件。 */
    SYSTEM_JUNK_FILE,

    /** 明显异常或不符合交付预期的文件。 */
    ABNORMAL_FILE,

    /** 与交付无关的大文件。 */
    UNRELATED_LARGE_FILE
}
