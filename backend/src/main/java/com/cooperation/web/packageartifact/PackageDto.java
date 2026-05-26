package com.cooperation.web.packageartifact;

import com.cooperation.application.packageartifact.ApplyCleanupSuggestionUseCase;
import com.cooperation.application.packageartifact.CreatePackageUseCase;
import com.cooperation.application.packageartifact.QueryLatestPackageUseCase;
import com.cooperation.application.packageartifact.RunPackageCheckUseCase;
import com.cooperation.domain.check.CheckIssue;
import com.cooperation.domain.check.CheckIssueType;
import com.cooperation.domain.check.CleanupItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.List;

/**
 * 打包检查、清理与压缩包 Web API 数据传输对象集合。
 */
public final class PackageDto {

    private PackageDto() {
    }

    /**
     * 清理建议请求。
     *
     * @param items 待清理或预览的建议项列表
     */
    public record CleanupRequest(@NotEmpty(message = "清理建议不能为空") List<@Valid CleanupItemRequest> items) {

        /**
         * 转换为应用层清理建议项。
         *
         * @return 应用层清理建议项列表
         */
        public List<CleanupItem> toCleanupItems() {
            return items.stream().map(CleanupItemRequest::toCleanupItem).toList();
        }
    }

    /**
     * 清理建议项请求。
     *
     * @param path 项目内相对路径
     * @param reason 清理原因说明
     * @param type 检查项类型，可为空
     */
    public record CleanupItemRequest(
            @NotBlank(message = "清理路径不能为空") String path,
            String reason,
            CheckIssueType type
    ) {

        /**
         * 转换为领域清理建议项。
         *
         * @return 领域清理建议项
         */
        public CleanupItem toCleanupItem() {
            return new CleanupItem(type == null ? CheckIssueType.TEMPORARY_FILE : type, path);
        }
    }

    /**
     * 创建压缩包请求。
     *
     * @param baseName 不含扩展名的压缩包文件名
     * @param format 压缩格式，支持 zip、7z、tar.gz
     * @param continueAfterCheck 是否确认忽略非阻断检查项继续打包
     */
    public record CreatePackageRequest(
            @NotBlank(message = "压缩包文件名不能为空") String baseName,
            @NotBlank(message = "压缩格式不能为空")
            @Pattern(regexp = "zip|7z|tar\\.gz", message = "压缩格式只支持 zip、7z、tar.gz")
            String format,
            Boolean continueAfterCheck
    ) {
    }

    /**
     * 打包检查响应。
     *
     * @param canContinuePackaging 是否可以继续打包
     * @param issues 检查问题列表
     * @param cleanupSuggestions 清理建议列表
     */
    public record CheckReportResponse(
            boolean canContinuePackaging,
            List<CheckIssueResponse> issues,
            List<CleanupSuggestionResponse> cleanupSuggestions
    ) {

        /**
         * 从应用层检查结果创建响应。
         *
         * @param result 应用层检查结果
         * @return 打包检查响应
         */
        public static CheckReportResponse from(RunPackageCheckUseCase.Result result) {
            return new CheckReportResponse(
                    result.canContinuePackaging(),
                    result.report().issues().stream().map(CheckIssueResponse::from).toList(),
                    result.cleanupSuggestion().items().stream().map(CleanupSuggestionResponse::from).toList()
            );
        }
    }

    /**
     * 检查问题响应。
     *
     * @param type 检查项类型
     * @param path 项目内相对路径
     * @param level 提醒级别
     * @param blocking 是否阻断打包
     * @param cleanupCandidate 是否可作为清理候选
     */
    public record CheckIssueResponse(
            CheckIssueType type,
            String path,
            String level,
            boolean blocking,
            boolean cleanupCandidate
    ) {

        /**
         * 从领域检查问题创建响应。
         *
         * @param issue 领域检查问题
         * @return 检查问题响应
         */
        public static CheckIssueResponse from(CheckIssue issue) {
            return new CheckIssueResponse(
                    issue.type(),
                    issue.path(),
                    issue.level().name(),
                    issue.blocksPackaging(),
                    issue.cleanupCandidate()
            );
        }
    }

    /**
     * 清理建议响应。
     *
     * @param type 清理来源检查类型
     * @param path 建议清理路径
     */
    public record CleanupSuggestionResponse(CheckIssueType type, String path) {

        /**
         * 从领域清理建议创建响应。
         *
         * @param item 领域清理建议项
         * @return 清理建议响应
         */
        public static CleanupSuggestionResponse from(CleanupItem item) {
            return new CleanupSuggestionResponse(item.type(), item.path());
        }
    }

    /**
     * 清理预览响应。
     *
     * @param previewObjects 将被处理的对象列表
     */
    public record CleanupPreviewResponse(List<PreviewObjectResponse> previewObjects) {

        /**
         * 从应用层清理预览结果创建响应。
         *
         * @param result 应用层清理预览结果
         * @return 清理预览响应
         */
        public static CleanupPreviewResponse from(ApplyCleanupSuggestionUseCase.PreviewResult result) {
            return new CleanupPreviewResponse(
                    result.objects().stream().map(PreviewObjectResponse::from).toList()
            );
        }
    }

    /**
     * 清理预览对象响应。
     *
     * @param path 项目内相对路径
     * @param objectId 对象标识
     * @param fileName 文件展示名
     * @param size 文件大小，单位字节
     */
    public record PreviewObjectResponse(String path, String objectId, String fileName, long size) {

        /**
         * 从应用层预览对象创建响应。
         *
         * @param object 应用层预览对象
         * @return 清理预览对象响应
         */
        public static PreviewObjectResponse from(ApplyCleanupSuggestionUseCase.PreviewObject object) {
            return new PreviewObjectResponse(object.path(), object.objectId(), object.fileName(), object.size());
        }
    }

    /**
     * 清理执行响应。
     *
     * @param cleanedObjectIds 已清理对象标识列表
     */
    public record CleanupApplyResponse(List<String> cleanedObjectIds) {
    }

    /**
     * 压缩包摘要响应。
     *
     * @param packageId 压缩包标识
     * @param filename 压缩包文件名
     * @param format 压缩格式
     * @param snapshotCreatedAt 快照创建时间
     * @param size 压缩包大小，单位字节
     */
    public record PackageSummaryResponse(
            String packageId,
            String filename,
            String format,
            Instant snapshotCreatedAt,
            long size
    ) {

        /**
         * 从创建压缩包结果创建响应。
         *
         * @param result 创建压缩包结果
         * @return 压缩包摘要响应
         */
        public static PackageSummaryResponse from(CreatePackageUseCase.Result result) {
            return new PackageSummaryResponse(
                    result.packageId(),
                    result.fileName(),
                    PackageController.toFormatValue(result.format()),
                    result.snapshotCreatedAt(),
                    result.size()
            );
        }

        /**
         * 从最近压缩包查询结果创建响应。
         *
         * @param result 最近压缩包查询结果
         * @return 压缩包摘要响应
         */
        public static PackageSummaryResponse from(QueryLatestPackageUseCase.Result result) {
            return new PackageSummaryResponse(
                    result.packageId(),
                    result.filename(),
                    result.format(),
                    result.snapshotCreatedAt(),
                    result.size()
            );
        }
    }
}
