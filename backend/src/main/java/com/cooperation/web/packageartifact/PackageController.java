package com.cooperation.web.packageartifact;

import com.cooperation.application.packageartifact.ApplyCleanupSuggestionUseCase;
import com.cooperation.application.packageartifact.CreatePackageUseCase;
import com.cooperation.application.packageartifact.DeletePackageUseCase;
import com.cooperation.application.packageartifact.DownloadLatestPackageUseCase;
import com.cooperation.application.packageartifact.ListPackagesUseCase;
import com.cooperation.application.packageartifact.QueryLatestPackageUseCase;
import com.cooperation.application.packageartifact.RunPackageCheckUseCase;
import com.cooperation.domain.packageartifact.PackageFormat;
import com.cooperation.web.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * 打包检查、清理与压缩包下载 Web API。
 */
@RestController
public class PackageController {

    private final RunPackageCheckUseCase runPackageCheckUseCase;
    private final ApplyCleanupSuggestionUseCase applyCleanupSuggestionUseCase;
    private final CreatePackageUseCase createPackageUseCase;
    private final QueryLatestPackageUseCase queryLatestPackageUseCase;
    private final DownloadLatestPackageUseCase downloadLatestPackageUseCase;
    private final DeletePackageUseCase deletePackageUseCase;
    private final ListPackagesUseCase listPackagesUseCase;

    /**
     * 创建打包 Web API 控制器。
     *
     * @param runPackageCheckUseCase 打包检查用例
     * @param applyCleanupSuggestionUseCase 清理建议用例
     * @param createPackageUseCase 创建压缩包用例
     * @param queryLatestPackageUseCase 查询最近压缩包用例
     * @param downloadLatestPackageUseCase 下载最近压缩包用例
     * @param deletePackageUseCase 删除压缩包用例
     * @param listPackagesUseCase 查询所有打包记录用例
     */
    public PackageController(
            RunPackageCheckUseCase runPackageCheckUseCase,
            ApplyCleanupSuggestionUseCase applyCleanupSuggestionUseCase,
            CreatePackageUseCase createPackageUseCase,
            QueryLatestPackageUseCase queryLatestPackageUseCase,
            DownloadLatestPackageUseCase downloadLatestPackageUseCase,
            DeletePackageUseCase deletePackageUseCase,
            ListPackagesUseCase listPackagesUseCase
    ) {
        this.runPackageCheckUseCase = runPackageCheckUseCase;
        this.applyCleanupSuggestionUseCase = applyCleanupSuggestionUseCase;
        this.createPackageUseCase = createPackageUseCase;
        this.queryLatestPackageUseCase = queryLatestPackageUseCase;
        this.downloadLatestPackageUseCase = downloadLatestPackageUseCase;
        this.deletePackageUseCase = deletePackageUseCase;
        this.listPackagesUseCase = listPackagesUseCase;
    }

    /**
     * 执行项目打包前检查。
     *
     * @param projectId 项目标识
     * @param actorId 当前用户标识
     * @return 检查报告和清理建议
     */
    @PostMapping("/projects/{projectId}/checks")
    public ApiResponse<PackageDto.CheckReportResponse> runCheck(
            @PathVariable String projectId,
            @RequestHeader("X-User-Id") String actorId
    ) {
        RunPackageCheckUseCase.Result result = runPackageCheckUseCase.run(
                new RunPackageCheckUseCase.Command(projectId, actorId)
        );
        return ApiResponse.success(PackageDto.CheckReportResponse.from(result));
    }

    /**
     * 预览清理建议将处理的对象。
     *
     * @param projectId 项目标识
     * @param actorId 当前用户标识
     * @param request 清理建议请求
     * @return 清理预览对象列表
     */
    @PostMapping("/projects/{projectId}/cleanup-preview")
    public ApiResponse<PackageDto.CleanupPreviewResponse> previewCleanup(
            @PathVariable String projectId,
            @RequestHeader("X-User-Id") String actorId,
            @Valid @RequestBody PackageDto.CleanupRequest request
    ) {
        ApplyCleanupSuggestionUseCase.PreviewResult result = applyCleanupSuggestionUseCase.preview(
                new ApplyCleanupSuggestionUseCase.PreviewCommand(projectId, actorId, request.toCleanupItems())
        );
        return ApiResponse.success(PackageDto.CleanupPreviewResponse.from(result));
    }

    /**
     * 执行清理建议。
     *
     * @param projectId 项目标识
     * @param actorId 当前用户标识
     * @param request 清理建议请求
     * @return 已清理对象标识列表
     */
    @PostMapping("/projects/{projectId}/cleanup")
    public ApiResponse<PackageDto.CleanupApplyResponse> applyCleanup(
            @PathVariable String projectId,
            @RequestHeader("X-User-Id") String actorId,
            @Valid @RequestBody PackageDto.CleanupRequest request
    ) {
        ApplyCleanupSuggestionUseCase.Result result = applyCleanupSuggestionUseCase.apply(
                new ApplyCleanupSuggestionUseCase.Command(projectId, actorId, request.toCleanupItems())
        );
        return ApiResponse.success(new PackageDto.CleanupApplyResponse(result.cleanedObjectIds()));
    }

    /**
     * 创建项目最终压缩包。
     *
     * @param projectId 项目标识
     * @param actorId 当前用户标识
     * @param request 创建压缩包请求
     * @return 压缩包摘要
     */
    @PostMapping("/projects/{projectId}/packages")
    public ApiResponse<PackageDto.PackageSummaryResponse> createPackage(
            @PathVariable String projectId,
            @RequestHeader("X-User-Id") String actorId,
            @Valid @RequestBody PackageDto.CreatePackageRequest request
    ) {
        CreatePackageUseCase.Result result = createPackageUseCase.create(
                new CreatePackageUseCase.Command(projectId, actorId, request.baseName(), toPackageFormat(request.format()))
        );
        return ApiResponse.success(PackageDto.PackageSummaryResponse.from(result));
    }

    /**
     * 查询项目最近压缩包摘要。
     *
     * @param projectId 项目标识
     * @param actorId 当前用户标识
     * @return 最近压缩包摘要
     */
    @GetMapping("/projects/{projectId}/packages/latest")
    public ApiResponse<PackageDto.PackageSummaryResponse> queryLatestPackage(
            @PathVariable String projectId,
            @RequestHeader("X-User-Id") String actorId
    ) {
        QueryLatestPackageUseCase.Result result = queryLatestPackageUseCase.query(
                new QueryLatestPackageUseCase.Query(projectId, actorId)
        );
        return ApiResponse.success(PackageDto.PackageSummaryResponse.from(result));
    }

    /**
     * 下载项目最近压缩包。
     *
     * @param projectId 项目标识
     * @param actorId 当前用户标识
     * @return 附件二进制响应
     */
    @GetMapping("/projects/{projectId}/packages/latest/download")
    public ResponseEntity<byte[]> downloadLatestPackage(
            @PathVariable String projectId,
            @RequestHeader("X-User-Id") String actorId
    ) {
        DownloadLatestPackageUseCase.Result result = downloadLatestPackageUseCase.download(
                new DownloadLatestPackageUseCase.Command(projectId, actorId)
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.filename() + "\"")
                .contentType(MediaType.parseMediaType(result.contentType()))
                .body(result.content());
    }

    /**
     * 查询项目所有打包记录。
     *
     * @param projectId 项目标识
     * @return 打包记录列表
     */
    @GetMapping("/projects/{projectId}/packages")
    public ApiResponse<List<PackageDto.PackageSummaryResponse>> listPackages(@PathVariable String projectId) {
        List<ListPackagesUseCase.Result> results = listPackagesUseCase.handle(new ListPackagesUseCase.Query(projectId));
        return ApiResponse.success(results.stream().map(PackageDto.PackageSummaryResponse::from).toList());
    }

    /**
     * 删除指定压缩包。
     *
     * @param projectId 项目标识
     * @param packageId 压缩包标识
     * @param actorId 当前用户标识
     * @return 空响应
     */
    @DeleteMapping("/projects/{projectId}/packages/{packageId}")
    public ApiResponse<Void> deletePackage(
            @PathVariable String projectId,
            @PathVariable String packageId,
            @RequestHeader("X-User-Id") String actorId
    ) {
        deletePackageUseCase.handle(new DeletePackageUseCase.Command(projectId, packageId, actorId));
        return ApiResponse.successWithoutData();
    }

    /**
     * 将请求中的压缩格式转换为领域枚举。
     *
     * @param format 请求压缩格式
     * @return 领域压缩格式
     */
    static PackageFormat toPackageFormat(String format) {
        return switch (format) {
            case "zip" -> PackageFormat.ZIP;
            case "7z" -> PackageFormat.SEVEN_ZIP;
            case "tar.gz" -> PackageFormat.TAR_GZ;
            default -> throw new IllegalArgumentException("压缩格式只支持 zip、7z、tar.gz");
        };
    }

    /**
     * 将领域压缩格式转换为接口展示值。
     *
     * @param format 领域压缩格式
     * @return 接口压缩格式值
     */
    static String toFormatValue(PackageFormat format) {
        return switch (format) {
            case ZIP -> "zip";
            case SEVEN_ZIP -> "7z";
            case TAR_GZ -> "tar.gz";
        };
    }
}
