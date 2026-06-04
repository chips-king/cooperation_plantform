# 单元测试与集成测试清单

本文件集中列出当前项目随代码一起提交的测试文件。测试文件保留在原框架约定目录中，确保 `mvn test`、`npm run test:unit` 可以直接运行。

## 后端测试

- `backend/src/test/java/com/cooperation/CooperationApplicationTests.java`
- `backend/src/test/java/com/cooperation/application/directory/UpdateDirectoryStatusUseCaseTest.java`
- `backend/src/test/java/com/cooperation/application/file/DeleteFileUseCaseTest.java`
- `backend/src/test/java/com/cooperation/application/file/RestoreFileUseCaseTest.java`
- `backend/src/test/java/com/cooperation/application/file/UploadFileUseCaseTest.java`
- `backend/src/test/java/com/cooperation/application/group/CreateGroupUseCaseTest.java`
- `backend/src/test/java/com/cooperation/application/home/ListRecentProjectsUseCaseTest.java`
- `backend/src/test/java/com/cooperation/application/home/RecentProjectsQueryUseCaseTest.java`
- `backend/src/test/java/com/cooperation/application/log/ListOperationLogsUseCaseTest.java`
- `backend/src/test/java/com/cooperation/application/log/QueryOperationLogUseCaseTest.java`
- `backend/src/test/java/com/cooperation/application/mail/CreateMailDraftUseCaseTest.java`
- `backend/src/test/java/com/cooperation/application/mail/SendMailDraftUseCaseTest.java`
- `backend/src/test/java/com/cooperation/application/member/CreateInvitationUseCaseTest.java`
- `backend/src/test/java/com/cooperation/application/member/JoinByInvitationUseCaseTest.java`
- `backend/src/test/java/com/cooperation/application/member/RemoveMemberUseCaseTest.java`
- `backend/src/test/java/com/cooperation/application/notification/ListNotificationsUseCaseTest.java`
- `backend/src/test/java/com/cooperation/application/notification/QueryNotificationUseCaseTest.java`
- `backend/src/test/java/com/cooperation/application/packageartifact/ApplyCleanupSuggestionUseCaseTest.java`
- `backend/src/test/java/com/cooperation/application/packageartifact/CreatePackageUseCaseTest.java`
- `backend/src/test/java/com/cooperation/application/packageartifact/RunPackageCheckUseCaseTest.java`
- `backend/src/test/java/com/cooperation/application/permission/UpdateMemberPermissionUseCaseTest.java`
- `backend/src/test/java/com/cooperation/application/project/CreateProjectUseCaseTest.java`
- `backend/src/test/java/com/cooperation/application/project/EndProjectUseCaseTest.java`
- `backend/src/test/java/com/cooperation/application/project/ReopenProjectUseCaseTest.java`
- `backend/src/test/java/com/cooperation/application/search/SearchUseCaseTest.java`
- `backend/src/test/java/com/cooperation/domain/check/CheckRuleTest.java`
- `backend/src/test/java/com/cooperation/domain/check/CleanupSuggestionTest.java`
- `backend/src/test/java/com/cooperation/domain/directory/DirectoryStatusTest.java`
- `backend/src/test/java/com/cooperation/domain/file/DuplicateFilePolicyTest.java`
- `backend/src/test/java/com/cooperation/domain/file/FileAssetStatusTest.java`
- `backend/src/test/java/com/cooperation/domain/file/FileNameTest.java`
- `backend/src/test/java/com/cooperation/domain/log/OperationLogRetentionTest.java`
- `backend/src/test/java/com/cooperation/domain/mail/MailDraftTest.java`
- `backend/src/test/java/com/cooperation/domain/notification/NotificationRoutingRuleTest.java`
- `backend/src/test/java/com/cooperation/domain/packageartifact/PackageFileNameTest.java`
- `backend/src/test/java/com/cooperation/domain/permission/PermissionSetTest.java`
- `backend/src/test/java/com/cooperation/domain/permission/RoleTemplateTest.java`
- `backend/src/test/java/com/cooperation/domain/project/ProjectStatusTest.java`
- `backend/src/test/java/com/cooperation/infrastructure/mail/SmtpMailDraftSenderTest.java`
- `backend/src/test/java/com/cooperation/infrastructure/mail/SmtpPasswordEncryptorTest.java`
- `backend/src/test/java/com/cooperation/infrastructure/security/AuthTokenServiceTest.java`
- `backend/src/test/java/com/cooperation/integration/archive/PackageArchiveIT.java`
- `backend/src/test/java/com/cooperation/integration/security/SecurityIT.java`
- `backend/src/test/java/com/cooperation/integration/storage/LocalFileStorageAdapterIT.java`
- `backend/src/test/java/com/cooperation/web/auth/AuthControllerTest.java`
- `backend/src/test/java/com/cooperation/web/file/FileControllerTest.java`
- `backend/src/test/java/com/cooperation/web/group/GroupControllerTest.java`
- `backend/src/test/java/com/cooperation/web/log/OperationLogControllerTest.java`
- `backend/src/test/java/com/cooperation/web/mail/MailDraftControllerTest.java`
- `backend/src/test/java/com/cooperation/web/mail/SmtpConfigControllerTest.java`
- `backend/src/test/java/com/cooperation/web/member/MemberControllerTest.java`
- `backend/src/test/java/com/cooperation/web/notification/NotificationControllerTest.java`
- `backend/src/test/java/com/cooperation/web/packageartifact/PackageControllerTest.java`
- `backend/src/test/java/com/cooperation/web/permission/PermissionControllerTest.java`
- `backend/src/test/java/com/cooperation/web/progress/ProgressControllerTest.java`
- `backend/src/test/java/com/cooperation/web/project/ProjectControllerTest.java`
- `backend/src/test/java/com/cooperation/web/search/SearchControllerTest.java`

## 前端测试

- `frontend/src/pages/MailDraftPage.spec.ts`
- `frontend/src/pages/PackageExportPage.spec.ts`
- `frontend/src/pages/ProgressHomePage.spec.ts`
- `frontend/src/pages/ProjectWorkspacePage.spec.ts`
- `frontend/src/router/index.spec.ts`
- `frontend/src/services/http.spec.ts`
- `frontend/src/stores/auth.spec.ts`

最后更新时间：2026-06-04 09:15:00
