package com.cooperation.application.packageartifact;

import static org.assertj.core.api.Assertions.assertThat;

import com.cooperation.domain.log.OperationAction;
import com.cooperation.domain.log.OperationLog;
import com.cooperation.domain.log.OperationLogRepository;
import com.cooperation.domain.packageartifact.PackageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * 创建最终压缩包应用用例测试。
 */
class CreatePackageUseCaseTest {

    /** 打包快照时间用于验证打包基于固定项目视图生成。 */
    private static final Instant SNAPSHOT_CREATED_AT = Instant.parse("2026-05-24T08:00:00Z");

    /**
     * 验证三种格式均会生成压缩包，并排除回收站文件和旧压缩包。
     */
    @ParameterizedTest
    @MethodSource("packageFormats")
    void shouldCreatePackageInSupportedFormatAndExcludeTrashAndOldPackages(
            PackageFormat format,
            String expectedFileName
    ) {
        FakePackageSnapshotRepository snapshots = new FakePackageSnapshotRepository(List.of(
                PackageSourceEntry.activeFile("src/App.java", "storage/src-app", 1024),
                PackageSourceEntry.activeFile("README.md", "storage/readme", 512),
                PackageSourceEntry.trashedFile("old/notes.tmp", "storage/trash-notes", 64),
                PackageSourceEntry.oldPackage("packages/last-export.zip", "storage/packages/last", 2048)
        ));
        FakePackageArchivePort archivePort = new FakePackageArchivePort();
        FakePackageArtifactRepository packages = new FakePackageArtifactRepository();
        FakeOperationLogRepository logs = new FakeOperationLogRepository();
        CreatePackageUseCase useCase = new CreatePackageUseCase(snapshots, archivePort, packages, logs);

        CreatePackageUseCase.Result result = useCase.create(new CreatePackageUseCase.Command(
                "project-1",
                "leader-1",
                "final-delivery",
                format
        ));

        assertThat(result.fileName()).isEqualTo(expectedFileName);
        assertThat(result.format()).isEqualTo(format);
        assertThat(result.snapshotCreatedAt()).isEqualTo(SNAPSHOT_CREATED_AT);
        assertThat(archivePort.lastRequest().fileName()).isEqualTo(expectedFileName);
        assertThat(archivePort.lastRequest().snapshotCreatedAt()).isEqualTo(SNAPSHOT_CREATED_AT);
        assertThat(archivePort.lastRequest().entries())
                .extracting(PackageArchiveEntry::path)
                .containsExactlyInAnyOrder("src/App.java", "README.md")
                .doesNotContain("old/notes.tmp", "packages/last-export.zip");
        assertThat(packages.savedPackageIds()).containsExactly(result.packageId());
        assertThat(packages.latestPackageId()).hasValue(result.packageId());
        assertThat(logs.actions()).containsExactly(OperationAction.PACKAGE_CREATED);
    }

    /**
     * 提供最终打包支持的三种格式。
     */
    private static Stream<Arguments> packageFormats() {
        return Stream.of(
                Arguments.of(PackageFormat.ZIP, "final-delivery.zip"),
                Arguments.of(PackageFormat.SEVEN_ZIP, "final-delivery.7z"),
                Arguments.of(PackageFormat.TAR_GZ, "final-delivery.tar.gz")
        );
    }

    /**
     * 内存打包快照仓储，避免访问真实文件系统。
     */
    private static final class FakePackageSnapshotRepository implements PackageSnapshotRepository {

        private final List<PackageSourceEntry> entries;

        private FakePackageSnapshotRepository(List<PackageSourceEntry> entries) {
            this.entries = List.copyOf(entries);
        }

        @Override
        public List<PackageSourceEntry> findSnapshotEntries(String projectId) {
            return entries;
        }

        @Override
        public Instant snapshotCreatedAt(String projectId) {
            return SNAPSHOT_CREATED_AT;
        }
    }

    /**
     * 内存压缩端口，只记录请求，不调用真实压缩库。
     */
    private static final class FakePackageArchivePort implements PackageArchivePort {

        private PackageArchiveRequest lastRequest;

        @Override
        public PackageArchiveResult create(PackageArchiveRequest request) {
            this.lastRequest = request;
            return new PackageArchiveResult("storage/packages/" + request.fileName(), request.entries().size());
        }

        private PackageArchiveRequest lastRequest() {
            return lastRequest;
        }
    }

    /**
     * 内存压缩包仓储，记录保存和最近压缩包标记。
     */
    private static final class FakePackageArtifactRepository implements PackageArtifactRepository {

        private final List<PackageArtifact> saved = new ArrayList<>();
        private String latestPackageId;

        @Override
        public PackageArtifact save(PackageArtifact artifact) {
            saved.add(artifact);
            return artifact;
        }

        @Override
        public void markAsLatest(String projectId, String packageId) {
            latestPackageId = packageId;
        }

        private List<String> savedPackageIds() {
            return saved.stream().map(PackageArtifact::id).toList();
        }

        private Optional<String> latestPackageId() {
            return Optional.ofNullable(latestPackageId);
        }
    }

    /**
     * 内存操作记录仓储，仅保存本测试关心的记录。
     */
    private static final class FakeOperationLogRepository implements OperationLogRepository {

        private final List<OperationLog> logs = new ArrayList<>();

        @Override
        public OperationLog save(OperationLog operationLog) {
            logs.add(operationLog);
            return operationLog;
        }

        private List<OperationAction> actions() {
            return logs.stream().map(OperationLog::getAction).toList();
        }

        @Override
        public List<OperationLog> findByProjectId(String projectId) {
            return logs.stream().filter(log -> log.getProjectId().equals(projectId)).toList();
        }

        @Override
        public List<OperationLog> findByProjectIdAndAction(String projectId, OperationAction action) {
            return logs.stream()
                    .filter(log -> log.getProjectId().equals(projectId) && log.getAction() == action)
                    .toList();
        }

        @Override
        public List<OperationLog> findByProjectIdAndActorId(String projectId, String actorId) {
            return logs.stream()
                    .filter(log -> log.getProjectId().equals(projectId) && log.getActorId().equals(actorId))
                    .toList();
        }

        @Override
        public Optional<OperationLog> findById(String id) {
            return Optional.empty();
        }
    }
}
