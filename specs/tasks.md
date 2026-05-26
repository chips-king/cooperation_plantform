# 分工协作系统原子任务清单

本文档基于 `specs/spec.md` 与 `specs/plan.md` 生成，用于指导后续 AI 逐项执行开发。

任务编号规则：

- `F` 表示 Foundation & Skeleton。
- `D` 表示 Domain Model & Domain Tests。
- `A` 表示 Application Use Cases & Application Tests。
- `W` 表示 API Contracts & Web API。
- `I` 表示 Infrastructure & Integration。
- `U` 表示 Frontend UI & Interaction。

并行标记：

- 标记 `[P]` 的任务没有直接文件依赖，可与同阶段其他 `[P]` 任务并行。
- 未标记 `[P]` 的任务需要等待其依赖任务完成。

执行铁律：

- Phase 2、Phase 3、Phase 4 必须先执行测试任务，再执行对应实现任务。
- 每个任务只创建或主要修改一个文件。
- 每个任务完成后，必须运行与该任务相关的最小测试或检查。
- 所有源码注释必须遵守 `AGENTS.md` 中的中文注释规则。

## Phase 1: Foundation & Skeleton

目标：建立前后端工程骨架、基础配置、依赖注入、日志、环境配置和前端基础工程，不实现具体业务功能。

### Phase 1 / F001 [P] 创建后端 Maven 工程配置

- 标记：[P]
- 目标文件：`backend/pom.xml`
- 依赖：无
- 内容：配置 Java 17、Spring Boot 3、Spring Web、Spring Security、Validation、MyBatis-Plus、MySQL Driver、JUnit 5、Testcontainers、Lombok。
- 验收：`backend/pom.xml` 可被 Maven 识别，依赖版本集中且无业务代码。

### Phase 1 / F002 [P] 创建后端应用入口

- 标记：[P]
- 目标文件：`backend/src/main/java/com/cooperation/CooperationApplication.java`
- 依赖：F001
- 内容：创建 Spring Boot 启动类，添加中文类注释。
- 验收：应用入口类结构正确，不包含业务逻辑。

### Phase 1 / F003 [P] 创建后端测试启动类

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/CooperationApplicationTests.java`
- 依赖：F001, F002
- 内容：创建最小上下文加载测试。
- 验收：后续可通过 JUnit 运行 Spring 上下文测试。

### Phase 1 / F004 [P] 创建后端基础配置文件

- 标记：[P]
- 目标文件：`backend/src/main/resources/application.yml`
- 依赖：F001
- 内容：配置应用名、端口、激活环境、日志级别占位、文件存储根路径占位。
- 验收：每个配置项有用途说明；不包含真实密码或敏感信息。

### Phase 1 / F005 [P] 创建后端本地环境示例配置

- 标记：[P]
- 目标文件：`backend/src/main/resources/application-local.example.yml`
- 依赖：F004
- 内容：提供 MySQL、文件存储、邮箱 API 的示例配置和取值说明。
- 验收：只包含示例值，不包含真实凭据。

### Phase 1 / F006 [P] 创建后端分层包说明

- 标记：[P]
- 目标文件：`backend/src/main/java/com/cooperation/package-info.java`
- 依赖：F002
- 内容：说明后端采用表现层、应用层、领域层、基础设施层分层。
- 验收：包说明不包含业务实现。

### Phase 1 / F007 [P] 创建统一日志配置

- 标记：[P]
- 目标文件：`backend/src/main/resources/logback-spring.xml`
- 依赖：F001
- 内容：配置控制台日志格式，避免输出敏感字段。
- 验收：日志配置存在敏感信息规避说明。

### Phase 1 / F008 [P] 创建后端全局异常占位类型

- 标记：[P]
- 目标文件：`backend/src/main/java/com/cooperation/common/error/ErrorCode.java`
- 依赖：F002
- 内容：定义通用错误码枚举占位，如 `VALIDATION_ERROR`、`FORBIDDEN`、`NOT_FOUND`、`CONFLICT`。
- 验收：仅包含通用错误码，不绑定具体业务流程。

### Phase 1 / F009 [P] 创建统一结果模型

- 标记：[P]
- 目标文件：`backend/src/main/java/com/cooperation/common/result/ApiResult.java`
- 依赖：F008
- 内容：定义成功、失败响应结构和静态工厂方法。
- 验收：包含中文 JSDoc 风格注释，字段含义清晰。

### Phase 1 / F010 [P] 创建 Docker Compose 编排文件

- 标记：[P]
- 目标文件：`docker-compose.yml`
- 依赖：无
- 内容：定义 MySQL、后端、前端服务占位和文件存储挂载目录。
- 验收：配置项有注释说明用途；不写真实密码。

### Phase 1 / F011 [P] 创建环境变量示例文件

- 标记：[P]
- 目标文件：`.env.example`
- 依赖：F010
- 内容：列出 MySQL、后端端口、前端端口、文件存储、邮箱 API 相关变量。
- 验收：每个变量都有中文说明和示例取值。

### Phase 1 / F012 [P] 创建前端包配置

- 标记：[P]
- 目标文件：`frontend/package.json`
- 依赖：无
- 内容：配置 Vue 3、TypeScript、Vite、Element Plus、Pinia、Vue Router、Vitest、Playwright。
- 验收：脚本包含 `dev`、`build`、`test:unit`、`test:e2e`、`lint`。

### Phase 1 / F013 [P] 创建前端 TypeScript 配置

- 标记：[P]
- 目标文件：`frontend/tsconfig.json`
- 依赖：F012
- 内容：配置 Vue + TypeScript 编译选项和路径别名。
- 验收：不包含业务逻辑，配置项有说明文档引用。

### Phase 1 / F014 [P] 创建前端 Vite 配置

- 标记：[P]
- 目标文件：`frontend/vite.config.ts`
- 依赖：F012
- 内容：配置 Vue 插件、测试环境、路径别名、开发代理占位。
- 验收：配置中代理目标来自环境变量，不硬编码生产地址。

### Phase 1 / F015 [P] 创建前端应用入口

- 标记：[P]
- 目标文件：`frontend/src/main.ts`
- 依赖：F012, F014
- 内容：创建 Vue 应用，挂载 Element Plus、Pinia、Router。
- 验收：不实现业务页面。

### Phase 1 / F016 [P] 创建前端根组件

- 标记：[P]
- 目标文件：`frontend/src/App.vue`
- 依赖：F015
- 内容：提供路由出口和基础布局挂载点。
- 验收：无业务逻辑，仅骨架。

### Phase 1 / F017 [P] 创建前端路由骨架

- 标记：[P]
- 目标文件：`frontend/src/router/index.ts`
- 依赖：F015
- 内容：创建路由实例，预留登录、首页、项目详情占位路由。
- 验收：路由仅指向占位页面。

### Phase 1 / F018 [P] 创建前端状态管理入口

- 标记：[P]
- 目标文件：`frontend/src/stores/index.ts`
- 依赖：F015
- 内容：导出 Pinia 初始化函数。
- 验收：不包含业务状态。

### Phase 1 / F019 [P] 创建前端 HTTP 客户端骨架

- 标记：[P]
- 目标文件：`frontend/src/services/http.ts`
- 依赖：F012
- 内容：封装请求实例、基础错误处理和鉴权头占位。
- 验收：无具体业务 API。

### Phase 1 / F020 [P] 创建项目 README

- 标记：[P]
- 目标文件：`README.md`
- 依赖：F001, F012
- 内容：说明项目结构、技术栈、启动命令、目录约定。
- 验收：不包含未确认业务需求，不包含敏感配置。

## Phase 2: Domain Model & Domain Tests (TDD)

目标：实现领域实体、值对象、聚合、领域服务、仓储抽象和领域规则。必须先生成测试任务，再生成实现任务。

### Domain Tests

### Phase 2 / D001 [P] 编写权限模板领域测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/domain/permission/RoleTemplateTest.java`
- 依赖：F001
- 内容：测试负责人、成员、只读三种模板的默认权限差异。
- 验收：测试覆盖只读不能下载、成员可下载、负责人拥有管理权限。

### Phase 2 / D002 [P] 编写自定义权限合并测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/domain/permission/PermissionSetTest.java`
- 依赖：F001
- 内容：测试模板权限与成员自定义权限的覆盖、追加和移除规则。
- 验收：测试能表达权限点合并结果。

### Phase 2 / D003 [P] 编写项目状态领域测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/domain/project/ProjectStatusTest.java`
- 依赖：F001
- 内容：测试项目 `active`、`ended` 状态转换和结束后写操作限制。
- 验收：测试覆盖结束、重新打开、重复结束边界。

### Phase 2 / D004 [P] 编写目录状态领域测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/domain/directory/DirectoryStatusTest.java`
- 依赖：F001
- 内容：测试目录状态 `not_started`、`in_progress`、`completed` 的合法变更。
- 验收：测试覆盖目录状态中文展示值和枚举值。

### Phase 2 / D005 [P] 编写文件名值对象测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/domain/file/FileNameTest.java`
- 依赖：F001
- 内容：测试空文件名、路径穿越、路径分隔符、控制字符、合法文件名。
- 验收：非法名称必须被拒绝，合法名称保留原始展示名。

### Phase 2 / D006 [P] 编写同名文件策略测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/domain/file/DuplicateFilePolicyTest.java`
- 依赖：F001
- 内容：测试覆盖、重命名、保留新版本三种策略的领域结果。
- 验收：每种策略都产生明确状态变化。

### Phase 2 / D007 [P] 编写文件状态领域测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/domain/file/FileAssetStatusTest.java`
- 依赖：F001
- 内容：测试 active、trashed、superseded 状态转换。
- 验收：删除进入回收站，回收站文件可恢复，覆盖后旧文件不再是 active。

### Phase 2 / D008 [P] 编写检查规则测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/domain/check/CheckRuleTest.java`
- 依赖：F001
- 内容：测试空目录、压缩包、缺少说明文档、缓存文件、日志文件的风险识别。
- 验收：检查结果只产生提醒，不产生阻断。

### Phase 2 / D009 [P] 编写清理建议测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/domain/check/CleanupSuggestionTest.java`
- 依赖：F001
- 内容：测试 `__pycache__/`、`.DS_Store`、`Thumbs.db`、`*.tmp`、`*.bak`、`*.log` 进入清理建议。
- 验收：压缩包、空目录、无关大文件不进入一键清理建议。

### Phase 2 / D010 [P] 编写压缩包命名测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/domain/packageartifact/PackageFileNameTest.java`
- 依赖：F001
- 内容：测试压缩包文件名不能为空、不能包含非法字符、可指定格式。
- 验收：`.zip`、`.7z`、`.tar.gz` 均可通过格式校验。

### Phase 2 / D011 [P] 编写邮件草稿领域测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/domain/mail/MailDraftTest.java`
- 依赖：F001
- 内容：测试草稿创建、修改、发送状态转换和发送失败保持草稿状态。
- 验收：草稿必须绑定压缩包且收件人不能为空。

### Phase 2 / D012 [P] 编写操作记录保留期测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/domain/log/OperationLogRetentionTest.java`
- 依赖：F001
- 内容：测试项目结束后操作记录默认保留 30 天。
- 验收：未结束项目不生成到期清理时间。

### Phase 2 / D013 [P] 编写通知路由规则测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/domain/notification/NotificationRoutingRuleTest.java`
- 依赖：F001
- 内容：测试文件变化通知相关成员，打包、邮件、项目结束通知全组。
- 验收：每类操作映射到正确接收范围。

### Domain Implementation

### Phase 2 / D014 实现权限点枚举

- 目标文件：`backend/src/main/java/com/cooperation/domain/permission/PermissionCode.java`
- 依赖：D001
- 内容：定义 `project.view`、`file.upload`、`package.create` 等权限点。
- 验收：D001 可引用全部权限点。

### Phase 2 / D015 实现角色模板枚举

- 目标文件：`backend/src/main/java/com/cooperation/domain/permission/RoleTemplate.java`
- 依赖：D001, D014
- 内容：定义负责人、成员、只读默认权限集合。
- 验收：D001 通过。

### Phase 2 / D016 实现权限集合值对象

- 目标文件：`backend/src/main/java/com/cooperation/domain/permission/PermissionSet.java`
- 依赖：D002, D014, D015
- 内容：实现模板权限和自定义权限合并逻辑。
- 验收：D002 通过。

### Phase 2 / D017 实现项目状态枚举

- 目标文件：`backend/src/main/java/com/cooperation/domain/project/ProjectStatus.java`
- 依赖：D003
- 内容：定义 `active`、`ended` 状态和值说明。
- 验收：D003 可引用项目状态。

### Phase 2 / D018 实现项目聚合

- 目标文件：`backend/src/main/java/com/cooperation/domain/project/Project.java`
- 依赖：D003, D017
- 内容：实现结束、重新打开和写操作状态校验。
- 验收：D003 通过。

### Phase 2 / D019 实现目录状态枚举

- 目标文件：`backend/src/main/java/com/cooperation/domain/directory/DirectoryStatus.java`
- 依赖：D004
- 内容：定义未开始、进行中、已完成三态。
- 验收：D004 可引用目录状态。

### Phase 2 / D020 实现目录实体

- 目标文件：`backend/src/main/java/com/cooperation/domain/directory/DirectoryNode.java`
- 依赖：D004, D019
- 内容：实现目录名称、父目录、状态变更规则。
- 验收：D004 通过。

### Phase 2 / D021 实现文件名值对象

- 目标文件：`backend/src/main/java/com/cooperation/domain/file/FileName.java`
- 依赖：D005
- 内容：封装文件名校验，拒绝路径穿越、路径分隔符和控制字符。
- 验收：D005 通过。

### Phase 2 / D022 实现同名文件策略枚举

- 目标文件：`backend/src/main/java/com/cooperation/domain/file/DuplicateFilePolicy.java`
- 依赖：D006
- 内容：定义覆盖、重命名、保留新版本三种策略。
- 验收：D006 可引用策略。

### Phase 2 / D023 实现文件状态枚举

- 目标文件：`backend/src/main/java/com/cooperation/domain/file/FileAssetStatus.java`
- 依赖：D007
- 内容：定义 active、trashed、superseded 状态。
- 验收：D007 可引用状态。

### Phase 2 / D024 实现文件实体

- 目标文件：`backend/src/main/java/com/cooperation/domain/file/FileAsset.java`
- 依赖：D005, D006, D007, D021, D022, D023
- 内容：实现上传人、目录、版本组、状态转换、删除和恢复。
- 验收：D006、D007 通过。

### Phase 2 / D025 实现检查项类型枚举

- 目标文件：`backend/src/main/java/com/cooperation/domain/check/CheckIssueType.java`
- 依赖：D008
- 内容：定义空目录、压缩包、缺少说明、缓存、临时文件、日志、异常文件等类型。
- 验收：D008 可引用检查类型。

### Phase 2 / D026 实现检查结果实体

- 目标文件：`backend/src/main/java/com/cooperation/domain/check/CheckIssue.java`
- 依赖：D008, D025
- 内容：表达检查问题类型、路径、提醒级别、是否阻止、是否可清理。
- 验收：D008 通过。

### Phase 2 / D027 实现清理建议规则

- 目标文件：`backend/src/main/java/com/cooperation/domain/check/CleanupSuggestionPolicy.java`
- 依赖：D009, D025, D026
- 内容：判断哪些检查项可进入清理建议。
- 验收：D009 通过。

### Phase 2 / D028 实现压缩包格式枚举

- 目标文件：`backend/src/main/java/com/cooperation/domain/packageartifact/PackageFormat.java`
- 依赖：D010
- 内容：定义 zip、7z、tar.gz 及扩展名。
- 验收：D010 可引用格式。

### Phase 2 / D029 实现压缩包文件名值对象

- 目标文件：`backend/src/main/java/com/cooperation/domain/packageartifact/PackageFileName.java`
- 依赖：D010, D028
- 内容：校验负责人输入的压缩包名称和格式后缀。
- 验收：D010 通过。

### Phase 2 / D030 实现邮件草稿实体

- 目标文件：`backend/src/main/java/com/cooperation/domain/mail/MailDraft.java`
- 依赖：D011
- 内容：实现草稿创建、修改、发送成功、发送失败状态规则。
- 验收：D011 通过。

### Phase 2 / D031 实现操作记录实体

- 目标文件：`backend/src/main/java/com/cooperation/domain/log/OperationLog.java`
- 依赖：D012
- 内容：实现操作摘要、元数据、项目结束后 30 天保留期计算。
- 验收：D012 通过。

### Phase 2 / D032 实现通知路由规则

- 目标文件：`backend/src/main/java/com/cooperation/domain/notification/NotificationRoutingRule.java`
- 依赖：D013
- 内容：根据操作类型计算通知接收范围类型。
- 验收：D013 通过。

### Phase 2 / D033 [P] 定义用户仓储抽象

- 标记：[P]
- 目标文件：`backend/src/main/java/com/cooperation/domain/user/UserRepository.java`
- 依赖：D014
- 内容：定义按 id、邮箱查找用户的领域仓储接口。
- 验收：无基础设施依赖。

### Phase 2 / D034 [P] 定义项目仓储抽象

- 标记：[P]
- 目标文件：`backend/src/main/java/com/cooperation/domain/project/ProjectRepository.java`
- 依赖：D018
- 内容：定义项目保存、按 id 查询、按用户查询最近项目接口。
- 验收：无 MyBatis 依赖。

### Phase 2 / D035 [P] 定义文件仓储抽象

- 标记：[P]
- 目标文件：`backend/src/main/java/com/cooperation/domain/file/FileAssetRepository.java`
- 依赖：D024
- 内容：定义文件保存、同目录同名查询、项目文件树查询、回收站查询接口。
- 验收：无存储实现细节。

### Phase 2 / D036 [P] 定义操作记录仓储抽象

- 标记：[P]
- 目标文件：`backend/src/main/java/com/cooperation/domain/log/OperationLogRepository.java`
- 依赖：D031
- 内容：定义操作记录保存和项目记录查询接口。
- 验收：无数据库实现细节。

## Phase 3: Application Use Cases & Application Tests (TDD)

目标：实现应用服务、输入输出模型、业务编排、事务边界抽象和应用层测试。必须先生成测试任务，再生成实现任务。

### Application Tests

### Phase 3 / A001 [P] 编写创建小组用例测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/application/group/CreateGroupUseCaseTest.java`
- 依赖：D033
- 内容：测试负责人创建小组后生成成员关系和操作记录。
- 验收：测试使用仓储假实现，不依赖数据库。

### Phase 3 / A002 [P] 编写创建项目用例测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/application/project/CreateProjectUseCaseTest.java`
- 依赖：D034
- 内容：测试负责人创建项目、初始化项目状态和更新时间。
- 验收：无权限时创建失败。

### Phase 3 / A003 [P] 编写邀请加入用例测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/application/member/JoinByInvitationUseCaseTest.java`
- 依赖：D033, D034
- 内容：测试直接加入和需要审核两种模式。
- 验收：需要审核时不直接成为正式成员。

### Phase 3 / A004 [P] 编写权限更新用例测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/application/permission/UpdateMemberPermissionUseCaseTest.java`
- 依赖：D016
- 内容：测试负责人可调整成员权限，普通成员不可调整权限。
- 验收：权限变更写入操作记录。

### Phase 3 / A005 [P] 编写文件上传用例测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/application/file/UploadFileUseCaseTest.java`
- 依赖：D024, D035
- 内容：测试上传源文件、上传压缩包、同名策略选择和上传记录。
- 验收：只读用户上传失败。

### Phase 3 / A006 [P] 编写文件删除用例测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/application/file/DeleteFileUseCaseTest.java`
- 依赖：D024, D035
- 内容：测试有目录权限成员删除文件进入回收站。
- 验收：删除记录和通知事件被创建。

### Phase 3 / A007 [P] 编写文件恢复用例测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/application/file/RestoreFileUseCaseTest.java`
- 依赖：D024, D035
- 内容：测试负责人和原目录有权限成员可恢复回收站文件。
- 验收：原目录不存在时返回需要选择恢复目录。

### Phase 3 / A008 [P] 编写目录状态更新用例测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/application/directory/UpdateDirectoryStatusUseCaseTest.java`
- 依赖：D020
- 内容：测试负责人和有权限成员可更新目录三态。
- 验收：状态变更写入操作记录并触发通知。

### Phase 3 / A009 [P] 编写打包检查用例测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/application/check/RunPackageCheckUseCaseTest.java`
- 依赖：D026, D027
- 内容：测试生成风险列表和清理建议，检查结果不阻止打包。
- 验收：压缩包只提示风险，不进入清理建议。

### Phase 3 / A010 [P] 编写应用清理建议用例测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/application/check/ApplyCleanupUseCaseTest.java`
- 依赖：D027, D035
- 内容：测试清理建议执行前校验权限，执行后对象进入回收站。
- 验收：清理动作写入操作记录。

### Phase 3 / A011 [P] 编写生成压缩包用例测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/application/packageartifact/CreatePackageUseCaseTest.java`
- 依赖：D029
- 内容：测试生成 zip、7z、tar.gz 时只包含 active 文件并替换最近一次压缩包。
- 验收：回收站文件和旧压缩包不进入快照。

### Phase 3 / A012 [P] 编写邮件草稿用例测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/application/mail/CreateMailDraftUseCaseTest.java`
- 依赖：D030
- 内容：测试基于最近压缩包创建草稿，收件人手动填写。
- 验收：不存在可用压缩包时返回错误。

### Phase 3 / A013 [P] 编写邮件发送用例测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/application/mail/SendMailDraftUseCaseTest.java`
- 依赖：D030
- 内容：测试负责人确认后发送，失败时保持草稿状态。
- 验收：发送成功写入记录并触发全组通知。

### Phase 3 / A014 [P] 编写项目结束用例测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/application/project/EndProjectUseCaseTest.java`
- 依赖：D018, D031
- 内容：测试手动结束项目、设置记录保留到期时间、锁定成员写操作。
- 验收：结束动作通知全组成员。

### Phase 3 / A015 [P] 编写项目重新打开用例测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/application/project/ReopenProjectUseCaseTest.java`
- 依赖：D018
- 内容：测试负责人可重新打开已结束项目。
- 验收：重新打开后恢复协作状态并记录操作。

### Phase 3 / A016 [P] 编写首页查询用例测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/application/home/ListRecentProjectsUseCaseTest.java`
- 依赖：D034
- 内容：测试按当前用户返回最近参与项目并支持小组筛选。
- 验收：不同小组数据隔离。

### Phase 3 / A017 [P] 编写搜索用例测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/application/search/SearchUseCaseTest.java`
- 依赖：D034, D035
- 内容：测试按项目名、文件名、成员名搜索。
- 验收：搜索结果只包含用户有权访问的数据。

### Phase 3 / A018 [P] 编写操作记录查询用例测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/application/log/ListOperationLogsUseCaseTest.java`
- 依赖：D036
- 内容：测试成员可查看全部记录，只读用户不可查看。
- 验收：支持按类型、操作人、时间筛选。

### Phase 3 / A019 [P] 编写通知查询用例测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/application/notification/ListNotificationsUseCaseTest.java`
- 依赖：D032
- 内容：测试用户只能查看自己的通知并可标记已读。
- 验收：未读和已读状态正确。

### Application Implementation

### Phase 3 / A020 创建应用层命令基类

- 目标文件：`backend/src/main/java/com/cooperation/application/common/UseCaseCommand.java`
- 依赖：A001
- 内容：定义命令对象标记接口，统一注释约束。
- 验收：无业务逻辑。

### Phase 3 / A021 创建应用层结果基类

- 目标文件：`backend/src/main/java/com/cooperation/application/common/UseCaseResult.java`
- 依赖：A001
- 内容：定义用例返回结果标记接口。
- 验收：无框架依赖。

### Phase 3 / A022 实现权限检查应用服务

- 目标文件：`backend/src/main/java/com/cooperation/application/permission/PermissionChecker.java`
- 依赖：A004, D016
- 内容：封装用户、项目、目录级权限判断。
- 验收：A004 通过。

### Phase 3 / A023 实现操作记录应用端口

- 目标文件：`backend/src/main/java/com/cooperation/application/log/OperationLogWriter.java`
- 依赖：A001, D036
- 内容：封装写操作记录的应用层接口。
- 验收：应用测试可使用假实现。

### Phase 3 / A024 实现通知发布应用端口

- 目标文件：`backend/src/main/java/com/cooperation/application/notification/NotificationPublisher.java`
- 依赖：A006, D032
- 内容：封装通知发布接口。
- 验收：应用测试可断言通知事件。

### Phase 3 / A025 实现创建小组用例

- 目标文件：`backend/src/main/java/com/cooperation/application/group/CreateGroupUseCase.java`
- 依赖：A001, A020, A021, A023
- 内容：创建小组、负责人关系和操作记录。
- 验收：A001 通过。

### Phase 3 / A026 实现创建项目用例

- 目标文件：`backend/src/main/java/com/cooperation/application/project/CreateProjectUseCase.java`
- 依赖：A002, A022, A023
- 内容：创建项目并校验负责人权限。
- 验收：A002 通过。

### Phase 3 / A027 实现邀请加入用例

- 目标文件：`backend/src/main/java/com/cooperation/application/member/JoinByInvitationUseCase.java`
- 依赖：A003, A023, A024
- 内容：处理直接加入和需要审核两种邀请模式。
- 验收：A003 通过。

### Phase 3 / A028 实现成员权限更新用例

- 目标文件：`backend/src/main/java/com/cooperation/application/permission/UpdateMemberPermissionUseCase.java`
- 依赖：A004, A022, A023, A024
- 内容：更新成员权限并记录通知。
- 验收：A004 通过。

### Phase 3 / A029 实现文件上传用例

- 目标文件：`backend/src/main/java/com/cooperation/application/file/UploadFileUseCase.java`
- 依赖：A005, A022, A023, A024
- 内容：校验权限、处理同名策略、保存文件元数据、记录上传。
- 验收：A005 通过。

### Phase 3 / A030 实现文件删除用例

- 目标文件：`backend/src/main/java/com/cooperation/application/file/DeleteFileUseCase.java`
- 依赖：A006, A022, A023, A024
- 内容：按目录权限删除文件并移入回收站。
- 验收：A006 通过。

### Phase 3 / A031 实现文件恢复用例

- 目标文件：`backend/src/main/java/com/cooperation/application/file/RestoreFileUseCase.java`
- 依赖：A007, A022, A023, A024
- 内容：按权限恢复回收站文件，处理原目录不存在场景。
- 验收：A007 通过。

### Phase 3 / A032 实现目录状态更新用例

- 目标文件：`backend/src/main/java/com/cooperation/application/directory/UpdateDirectoryStatusUseCase.java`
- 依赖：A008, A022, A023, A024
- 内容：更新目录状态并发布通知。
- 验收：A008 通过。

### Phase 3 / A033 实现打包检查用例

- 目标文件：`backend/src/main/java/com/cooperation/application/check/RunPackageCheckUseCase.java`
- 依赖：A009, D026, D027
- 内容：扫描项目文件树并生成检查报告和清理建议。
- 验收：A009 通过。

### Phase 3 / A034 实现清理建议用例

- 目标文件：`backend/src/main/java/com/cooperation/application/check/ApplyCleanupUseCase.java`
- 依赖：A010, A022, A023, A024
- 内容：清理建议执行时移入回收站而非永久删除。
- 验收：A010 通过。

### Phase 3 / A035 实现生成压缩包用例

- 目标文件：`backend/src/main/java/com/cooperation/application/packageartifact/CreatePackageUseCase.java`
- 依赖：A011, A022, A023, A024
- 内容：创建打包快照、调用压缩端口、标记最近压缩包。
- 验收：A011 通过。

### Phase 3 / A036 实现邮件草稿创建用例

- 目标文件：`backend/src/main/java/com/cooperation/application/mail/CreateMailDraftUseCase.java`
- 依赖：A012, A022, A023, A024
- 内容：创建与压缩包绑定的邮件草稿。
- 验收：A012 通过。

### Phase 3 / A037 实现邮件草稿发送用例

- 目标文件：`backend/src/main/java/com/cooperation/application/mail/SendMailDraftUseCase.java`
- 依赖：A013, A022, A023, A024
- 内容：调用邮箱端口发送草稿，失败保持草稿状态。
- 验收：A013 通过。

### Phase 3 / A038 实现项目结束用例

- 目标文件：`backend/src/main/java/com/cooperation/application/project/EndProjectUseCase.java`
- 依赖：A014, A023, A024
- 内容：结束项目、设置记录保留期、通知全组。
- 验收：A014 通过。

### Phase 3 / A039 实现项目重新打开用例

- 目标文件：`backend/src/main/java/com/cooperation/application/project/ReopenProjectUseCase.java`
- 依赖：A015, A023, A024
- 内容：重新打开已结束项目并写入记录。
- 验收：A015 通过。

### Phase 3 / A040 实现最近项目查询用例

- 目标文件：`backend/src/main/java/com/cooperation/application/home/ListRecentProjectsUseCase.java`
- 依赖：A016
- 内容：按用户返回最近参与项目并支持小组筛选。
- 验收：A016 通过。

### Phase 3 / A041 实现搜索用例

- 目标文件：`backend/src/main/java/com/cooperation/application/search/SearchUseCase.java`
- 依赖：A017
- 内容：按项目名、文件名、成员名搜索并做访问范围过滤。
- 验收：A017 通过。

### Phase 3 / A042 实现操作记录查询用例

- 目标文件：`backend/src/main/java/com/cooperation/application/log/ListOperationLogsUseCase.java`
- 依赖：A018
- 内容：返回项目操作记录并校验只读不可访问。
- 验收：A018 通过。

### Phase 3 / A043 实现通知查询用例

- 目标文件：`backend/src/main/java/com/cooperation/application/notification/ListNotificationsUseCase.java`
- 依赖：A019
- 内容：返回当前用户通知并支持已读状态。
- 验收：A019 通过。

## Phase 4: API Contracts & Web API (TDD)

目标：实现 API DTO、错误响应模型、Controller、请求校验、DTO 映射、Result 到 HTTP 响应映射和接口测试。必须先生成接口测试任务，再生成实现任务。

### API Tests

### Phase 4 / W001 [P] 编写小组接口测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/web/group/GroupControllerTest.java`
- 依赖：A025
- 内容：测试 `GET /groups`、`POST /groups`、`GET /groups/{groupId}`。
- 验收：创建小组返回统一响应结构。

### Phase 4 / W002 [P] 编写项目接口测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/web/project/ProjectControllerTest.java`
- 依赖：A026, A038, A039
- 内容：测试创建项目、查询项目、结束项目、重新打开项目。
- 验收：项目结束后返回状态变更。

### Phase 4 / W003 [P] 编写成员与邀请接口测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/web/member/MemberControllerTest.java`
- 依赖：A027
- 内容：测试成员列表、手动添加成员、创建邀请、加入邀请、审批加入。
- 验收：直接加入和审核加入响应不同状态。

### Phase 4 / W004 [P] 编写权限接口测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/web/permission/PermissionControllerTest.java`
- 依赖：A028
- 内容：测试获取项目权限和更新成员权限。
- 验收：无权限用户收到 403。

### Phase 4 / W005 [P] 编写目录与文件接口测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/web/file/FileControllerTest.java`
- 依赖：A029, A030, A031, A032
- 内容：测试目录树、上传、下载、移动、删除、回收站和恢复。
- 验收：同名冲突返回处理选项。

### Phase 4 / W006 [P] 编写进度接口测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/web/progress/ProgressControllerTest.java`
- 依赖：A032
- 内容：测试获取进度和更新目录状态。
- 验收：目录状态仅允许三态。

### Phase 4 / W007 [P] 编写检查清理打包接口测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/web/packageartifact/PackageControllerTest.java`
- 依赖：A033, A034, A035
- 内容：测试打包检查、清理预览、执行清理、创建压缩包、下载最近压缩包。
- 验收：检查问题不阻止打包。

### Phase 4 / W008 [P] 编写邮件接口测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/web/mail/MailDraftControllerTest.java`
- 依赖：A036, A037
- 内容：测试创建草稿、查询草稿、修改草稿、发送草稿。
- 验收：发送失败返回明确错误且草稿状态不变。

### Phase 4 / W009 [P] 编写操作记录接口测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/web/log/OperationLogControllerTest.java`
- 依赖：A042
- 内容：测试项目操作记录查询、筛选和只读访问拒绝。
- 验收：成员可查看全部记录。

### Phase 4 / W010 [P] 编写通知接口测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/web/notification/NotificationControllerTest.java`
- 依赖：A043
- 内容：测试通知列表和标记已读。
- 验收：只能操作自己的通知。

### Phase 4 / W011 [P] 编写搜索接口测试

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/web/search/SearchControllerTest.java`
- 依赖：A041
- 内容：测试项目、文件、成员搜索。
- 验收：搜索结果按权限隔离。

### Web API Implementation

### Phase 4 / W012 创建错误响应 DTO

- 目标文件：`backend/src/main/java/com/cooperation/web/dto/ErrorResponse.java`
- 依赖：W001
- 内容：定义错误码、消息、字段错误列表。
- 验收：接口测试可断言错误结构。

### Phase 4 / W013 创建全局异常处理器

- 目标文件：`backend/src/main/java/com/cooperation/web/error/GlobalExceptionHandler.java`
- 依赖：W012
- 内容：将校验、权限、未找到、冲突异常映射到 HTTP 响应。
- 验收：W004 可断言 403。

### Phase 4 / W014 [P] 创建分页响应 DTO

- 标记：[P]
- 目标文件：`backend/src/main/java/com/cooperation/web/dto/PageResponse.java`
- 依赖：W001
- 内容：定义列表数据、页码、页大小、总数。
- 验收：列表类接口可复用。

### Phase 4 / W015 创建小组 DTO

- 目标文件：`backend/src/main/java/com/cooperation/web/group/GroupDto.java`
- 依赖：W001
- 内容：定义创建小组请求、更新请求、响应 DTO。
- 验收：W001 可编译。

### Phase 4 / W016 创建小组 Controller

- 目标文件：`backend/src/main/java/com/cooperation/web/group/GroupController.java`
- 依赖：W001, W015, A025
- 内容：实现小组查询和创建接口。
- 验收：W001 通过。

### Phase 4 / W017 创建项目 DTO

- 目标文件：`backend/src/main/java/com/cooperation/web/project/ProjectDto.java`
- 依赖：W002
- 内容：定义项目创建、详情、结束、重新打开响应 DTO。
- 验收：W002 可编译。

### Phase 4 / W018 创建项目 Controller

- 目标文件：`backend/src/main/java/com/cooperation/web/project/ProjectController.java`
- 依赖：W002, W017, A026, A038, A039
- 内容：实现项目创建、查询、结束、重新打开接口。
- 验收：W002 通过。

### Phase 4 / W019 创建成员 DTO

- 目标文件：`backend/src/main/java/com/cooperation/web/member/MemberDto.java`
- 依赖：W003
- 内容：定义成员、邀请、加入申请、审批相关 DTO。
- 验收：W003 可编译。

### Phase 4 / W020 创建成员 Controller

- 目标文件：`backend/src/main/java/com/cooperation/web/member/MemberController.java`
- 依赖：W003, W019, A027
- 内容：实现成员列表、添加成员、邀请加入、审批接口。
- 验收：W003 通过。

### Phase 4 / W021 创建权限 DTO

- 目标文件：`backend/src/main/java/com/cooperation/web/permission/PermissionDto.java`
- 依赖：W004
- 内容：定义权限列表、权限更新请求和响应 DTO。
- 验收：W004 可编译。

### Phase 4 / W022 创建权限 Controller

- 目标文件：`backend/src/main/java/com/cooperation/web/permission/PermissionController.java`
- 依赖：W004, W021, A028
- 内容：实现权限查询和更新接口。
- 验收：W004 通过。

### Phase 4 / W023 创建文件 DTO

- 目标文件：`backend/src/main/java/com/cooperation/web/file/FileDto.java`
- 依赖：W005
- 内容：定义目录树、文件列表、上传结果、同名冲突、回收站响应 DTO。
- 验收：W005 可编译。

### Phase 4 / W024 创建文件 Controller

- 目标文件：`backend/src/main/java/com/cooperation/web/file/FileController.java`
- 依赖：W005, W023, A029, A030, A031
- 内容：实现目录树、上传、下载、移动、删除、回收站、恢复接口。
- 验收：W005 通过。

### Phase 4 / W025 创建进度 DTO

- 目标文件：`backend/src/main/java/com/cooperation/web/progress/ProgressDto.java`
- 依赖：W006
- 内容：定义目录进度列表和状态更新请求 DTO。
- 验收：W006 可编译。

### Phase 4 / W026 创建进度 Controller

- 目标文件：`backend/src/main/java/com/cooperation/web/progress/ProgressController.java`
- 依赖：W006, W025, A032
- 内容：实现进度查询和目录状态更新接口。
- 验收：W006 通过。

### Phase 4 / W027 创建检查打包 DTO

- 目标文件：`backend/src/main/java/com/cooperation/web/packageartifact/PackageDto.java`
- 依赖：W007
- 内容：定义检查报告、清理预览、打包请求、压缩包响应 DTO。
- 验收：W007 可编译。

### Phase 4 / W028 创建打包 Controller

- 目标文件：`backend/src/main/java/com/cooperation/web/packageartifact/PackageController.java`
- 依赖：W007, W027, A033, A034, A035
- 内容：实现检查、清理、打包、下载最近压缩包接口。
- 验收：W007 通过。

### Phase 4 / W029 创建邮件 DTO

- 目标文件：`backend/src/main/java/com/cooperation/web/mail/MailDraftDto.java`
- 依赖：W008
- 内容：定义草稿创建、更新、发送和详情 DTO。
- 验收：W008 可编译。

### Phase 4 / W030 创建邮件 Controller

- 目标文件：`backend/src/main/java/com/cooperation/web/mail/MailDraftController.java`
- 依赖：W008, W029, A036, A037
- 内容：实现草稿创建、查询、修改、发送接口。
- 验收：W008 通过。

### Phase 4 / W031 创建操作记录 Controller

- 目标文件：`backend/src/main/java/com/cooperation/web/log/OperationLogController.java`
- 依赖：W009, A042
- 内容：实现项目操作记录查询和筛选接口。
- 验收：W009 通过。

### Phase 4 / W032 创建通知 Controller

- 目标文件：`backend/src/main/java/com/cooperation/web/notification/NotificationController.java`
- 依赖：W010, A043
- 内容：实现通知列表和标记已读接口。
- 验收：W010 通过。

### Phase 4 / W033 创建搜索 Controller

- 目标文件：`backend/src/main/java/com/cooperation/web/search/SearchController.java`
- 依赖：W011, A041
- 内容：实现项目、文件、成员搜索接口。
- 验收：W011 通过。

## Phase 5: Infrastructure & Integration

目标：实现仓储、数据库映射、外部服务适配、认证落地、配置实现和集成测试支撑。

### Phase 5 / I001 [P] 创建数据库迁移初始化脚本

- 标记：[P]
- 目标文件：`backend/src/main/resources/db/migration/V1__init_schema.sql`
- 依赖：D014-D036
- 内容：创建用户、小组、项目、成员、目录、文件、压缩包、邮件草稿、操作记录、通知表。
- 验收：字段覆盖 `plan.md` 核心实体模型。

### Phase 5 / I002 [P] 创建 MyBatis-Plus 用户实体

- 标记：[P]
- 目标文件：`backend/src/main/java/com/cooperation/infrastructure/persistence/user/UserEntity.java`
- 依赖：I001
- 内容：映射用户表字段。
- 验收：不包含业务规则。

### Phase 5 / I003 [P] 创建 MyBatis-Plus 项目实体

- 标记：[P]
- 目标文件：`backend/src/main/java/com/cooperation/infrastructure/persistence/project/ProjectEntity.java`
- 依赖：I001
- 内容：映射项目表字段。
- 验收：包含项目状态和最近压缩包字段。

### Phase 5 / I004 [P] 创建 MyBatis-Plus 文件实体

- 标记：[P]
- 目标文件：`backend/src/main/java/com/cooperation/infrastructure/persistence/file/FileAssetEntity.java`
- 依赖：I001
- 内容：映射文件表字段。
- 验收：包含版本组、状态、删除信息。

### Phase 5 / I005 [P] 创建 MyBatis-Plus 操作记录实体

- 标记：[P]
- 目标文件：`backend/src/main/java/com/cooperation/infrastructure/persistence/log/OperationLogEntity.java`
- 依赖：I001
- 内容：映射操作记录表字段。
- 验收：包含 `retain_until`。

### Phase 5 / I006 实现用户仓储

- 目标文件：`backend/src/main/java/com/cooperation/infrastructure/persistence/user/MyBatisUserRepository.java`
- 依赖：I002, D033
- 内容：实现用户仓储抽象。
- 验收：仓储集成测试可保存和查询用户。

### Phase 5 / I007 实现项目仓储

- 目标文件：`backend/src/main/java/com/cooperation/infrastructure/persistence/project/MyBatisProjectRepository.java`
- 依赖：I003, D034
- 内容：实现项目保存、查询、最近项目查询。
- 验收：项目集成测试可按用户过滤项目。

### Phase 5 / I008 实现文件仓储

- 目标文件：`backend/src/main/java/com/cooperation/infrastructure/persistence/file/MyBatisFileAssetRepository.java`
- 依赖：I004, D035
- 内容：实现同名查询、文件树查询、回收站查询。
- 验收：文件集成测试可查询 active 和 trashed 文件。

### Phase 5 / I009 实现操作记录仓储

- 目标文件：`backend/src/main/java/com/cooperation/infrastructure/persistence/log/MyBatisOperationLogRepository.java`
- 依赖：I005, D036
- 内容：实现操作记录写入和筛选查询。
- 验收：记录集成测试可按项目、类型、操作人筛选。

### Phase 5 / I010 [P] 创建本地文件存储配置

- 标记：[P]
- 目标文件：`backend/src/main/java/com/cooperation/infrastructure/storage/StorageProperties.java`
- 依赖：F004
- 内容：绑定文件存储根路径、单文件大小限制、临时目录配置。
- 验收：配置项有中文注释。

### Phase 5 / I011 实现本地文件存储适配器

- 目标文件：`backend/src/main/java/com/cooperation/infrastructure/storage/LocalFileStorageAdapter.java`
- 依赖：I010, A029
- 内容：实现文件保存、读取、移动到回收站、下载流。
- 验收：路径限制在配置根目录内。

### Phase 5 / I012 [P] 创建对象存储端口

- 标记：[P]
- 目标文件：`backend/src/main/java/com/cooperation/infrastructure/storage/ObjectStoragePort.java`
- 依赖：I010
- 内容：定义后续对象存储扩展接口。
- 验收：不实现具体云厂商逻辑。

### Phase 5 / I013 [P] 创建压缩服务端口

- 标记：[P]
- 目标文件：`backend/src/main/java/com/cooperation/application/packageartifact/PackageArchivePort.java`
- 依赖：A035
- 内容：定义 zip、7z、tar.gz 生成接口。
- 验收：应用层不依赖具体压缩库。

### Phase 5 / I014 实现 zip 压缩适配器

- 目标文件：`backend/src/main/java/com/cooperation/infrastructure/archive/ZipArchiveAdapter.java`
- 依赖：I013
- 内容：生成不额外套项目根目录的 zip 包。
- 验收：打包集成测试可验证内部结构。

### Phase 5 / I015 实现 7z 压缩适配器

- 目标文件：`backend/src/main/java/com/cooperation/infrastructure/archive/SevenZipArchiveAdapter.java`
- 依赖：I013
- 内容：生成 7z 压缩包。
- 验收：打包集成测试可生成 7z 文件。

### Phase 5 / I016 实现 tar.gz 压缩适配器

- 目标文件：`backend/src/main/java/com/cooperation/infrastructure/archive/TarGzArchiveAdapter.java`
- 依赖：I013
- 内容：生成 tar.gz 压缩包。
- 验收：打包集成测试可生成 tar.gz 文件。

### Phase 5 / I017 [P] 创建邮箱 API 端口

- 标记：[P]
- 目标文件：`backend/src/main/java/com/cooperation/application/mail/MailProviderPort.java`
- 依赖：A037
- 内容：定义创建第三方草稿、更新草稿、发送草稿接口。
- 验收：应用层不依赖具体邮箱厂商。

### Phase 5 / I018 实现国内邮箱 API 占位适配器

- 目标文件：`backend/src/main/java/com/cooperation/infrastructure/mail/DomesticMailProviderAdapter.java`
- 依赖：I017
- 内容：封装 QQ/163 草稿箱 API 优先策略，若不支持则返回可降级错误。
- 验收：不写真实授权信息；错误信息不泄露凭据。

### Phase 5 / I019 创建 Spring Security 配置

- 目标文件：`backend/src/main/java/com/cooperation/infrastructure/security/SecurityConfig.java`
- 依赖：A022
- 内容：配置账号密码登录、认证过滤、接口鉴权入口。
- 验收：未登录访问受保护接口返回 401。

### Phase 5 / I020 创建当前用户上下文

- 目标文件：`backend/src/main/java/com/cooperation/infrastructure/security/CurrentUserProvider.java`
- 依赖：I019
- 内容：为应用层提供当前用户 id。
- 验收：Controller 不直接解析安全上下文。

### Phase 5 / I021 [P] 创建密码加密配置

- 标记：[P]
- 目标文件：`backend/src/main/java/com/cooperation/infrastructure/security/PasswordEncoderConfig.java`
- 依赖：I019
- 内容：配置密码哈希策略。
- 验收：不明文存储密码。

### Phase 5 / I022 [P] 创建 Testcontainers MySQL 测试基类

- 标记：[P]
- 目标文件：`backend/src/test/java/com/cooperation/integration/IntegrationTestBase.java`
- 依赖：F001, I001
- 内容：提供 MySQL 容器、迁移执行、测试上下文。
- 验收：集成测试可复用。

### Phase 5 / I023 编写文件存储集成测试

- 目标文件：`backend/src/test/java/com/cooperation/integration/storage/LocalFileStorageAdapterIT.java`
- 依赖：I011
- 内容：测试保存、读取、路径穿越拒绝、移动回收站。
- 验收：所有文件操作限制在测试目录内。

### Phase 5 / I024 编写打包集成测试

- 目标文件：`backend/src/test/java/com/cooperation/integration/archive/PackageArchiveIT.java`
- 依赖：I014, I015, I016
- 内容：测试三种格式生成和不套项目根目录。
- 验收：回收站文件不出现在压缩包中。

### Phase 5 / I025 编写认证集成测试

- 目标文件：`backend/src/test/java/com/cooperation/integration/security/SecurityIT.java`
- 依赖：I019, I020, I021
- 内容：测试未登录 401、无权限 403、登录后可访问。
- 验收：安全链路可用。

## Phase 6: Frontend UI & Interaction

目标：实现路由、页面、组件、布局、API Service、表单处理、页面状态、鉴权态和联调任务。

### Phase 6 / U001 [P] 编写前端 HTTP 客户端测试

- 标记：[P]
- 目标文件：`frontend/src/services/http.spec.ts`
- 依赖：F019
- 内容：测试统一响应解析、错误响应处理、鉴权头注入。
- 验收：HTTP 客户端行为可验证。

### Phase 6 / U002 [P] 实现 API 类型定义

- 标记：[P]
- 目标文件：`frontend/src/types/api.ts`
- 依赖：W012-W033
- 内容：定义统一响应、分页响应、错误响应基础类型。
- 验收：后续 Service 可复用。

### Phase 6 / U003 [P] 实现项目领域前端类型

- 标记：[P]
- 目标文件：`frontend/src/types/project.ts`
- 依赖：U002
- 内容：定义 Group、Project、Membership、Permission、Directory、FileAsset、OperationLog、Notification 类型。
- 验收：字段与 API DTO 对齐。

### Phase 6 / U004 [P] 创建认证状态 Store

- 标记：[P]
- 目标文件：`frontend/src/stores/auth.ts`
- 依赖：F018, U002
- 内容：保存当前用户、登录态、权限摘要。
- 验收：不包含页面逻辑。

### Phase 6 / U005 [P] 创建项目状态 Store

- 标记：[P]
- 目标文件：`frontend/src/stores/project.ts`
- 依赖：F018, U003
- 内容：保存当前小组、当前项目、项目列表和筛选条件。
- 验收：不直接调用 DOM。

### Phase 6 / U006 [P] 创建小组项目 API Service

- 标记：[P]
- 目标文件：`frontend/src/services/groupProjectApi.ts`
- 依赖：U001, U002, U003
- 内容：封装小组、项目、首页、搜索接口。
- 验收：方法名与 REST API 对齐。

### Phase 6 / U007 [P] 创建成员权限 API Service

- 标记：[P]
- 目标文件：`frontend/src/services/memberPermissionApi.ts`
- 依赖：U001, U002, U003
- 内容：封装成员、邀请、权限接口。
- 验收：支持直接加入和审核加入调用。

### Phase 6 / U008 [P] 创建文件 API Service

- 标记：[P]
- 目标文件：`frontend/src/services/fileApi.ts`
- 依赖：U001, U002, U003
- 内容：封装目录树、上传、下载、移动、删除、回收站、恢复接口。
- 验收：上传方法支持同名策略参数。

### Phase 6 / U009 [P] 创建打包 API Service

- 标记：[P]
- 目标文件：`frontend/src/services/packageApi.ts`
- 依赖：U001, U002, U003
- 内容：封装检查、清理预览、清理执行、打包、下载最近压缩包接口。
- 验收：支持三种压缩格式参数。

### Phase 6 / U010 [P] 创建邮件 API Service

- 标记：[P]
- 目标文件：`frontend/src/services/mailApi.ts`
- 依赖：U001, U002, U003
- 内容：封装邮件草稿创建、详情、更新、发送接口。
- 验收：支持附件格式和草稿修改字段。

### Phase 6 / U011 [P] 创建操作记录通知 API Service

- 标记：[P]
- 目标文件：`frontend/src/services/activityApi.ts`
- 依赖：U001, U002, U003
- 内容：封装操作记录和通知接口。
- 验收：支持通知标记已读。

### Phase 6 / U012 [P] 创建主布局组件

- 标记：[P]
- 目标文件：`frontend/src/layouts/MainLayout.vue`
- 依赖：F016
- 内容：实现顶部导航、侧边栏插槽、内容区和通知入口。
- 验收：不包含业务请求。

### Phase 6 / U013 [P] 创建登录页面

- 标记：[P]
- 目标文件：`frontend/src/pages/LoginPage.vue`
- 依赖：U004
- 内容：实现账号密码登录表单和基础校验。
- 验收：不保存明文密码。

### Phase 6 / U014 [P] 创建首页页面

- 标记：[P]
- 目标文件：`frontend/src/pages/HomePage.vue`
- 依赖：U005, U006
- 内容：展示最近参与项目、小组筛选、项目搜索入口。
- 验收：符合 `spec.md` 首页要求。

### Phase 6 / U015 [P] 创建小组详情页面

- 标记：[P]
- 目标文件：`frontend/src/pages/GroupDetailPage.vue`
- 依赖：U006, U007
- 内容：展示小组项目、成员列表、邀请链接管理入口。
- 验收：邀请模式可选择直接加入或需要审核。

### Phase 6 / U016 [P] 创建项目工作台页面

- 标记：[P]
- 目标文件：`frontend/src/pages/ProjectWorkspacePage.vue`
- 依赖：U005, U006, U011
- 内容：展示项目概览、目录进度、最近变化、检查打包邮件入口。
- 验收：项目结束状态下展示锁定提示。

### Phase 6 / U017 [P] 创建文件管理页面

- 标记：[P]
- 目标文件：`frontend/src/pages/FileManagerPage.vue`
- 依赖：U008
- 内容：展示目录树、文件列表、上传入口、移动重命名删除入口、回收站入口。
- 验收：只显示文件列表和基本信息，不做复杂预览。

### Phase 6 / U018 [P] 创建同名文件处理弹窗

- 标记：[P]
- 目标文件：`frontend/src/components/file/DuplicateFileDialog.vue`
- 依赖：U017
- 内容：提供覆盖、重命名、保留新版本三个选择。
- 验收：选择结果返回给上传流程。

### Phase 6 / U019 [P] 创建回收站抽屉组件

- 标记：[P]
- 目标文件：`frontend/src/components/file/TrashDrawer.vue`
- 依赖：U008
- 内容：展示可恢复文件，支持恢复操作。
- 验收：按权限展示恢复入口。

### Phase 6 / U020 [P] 创建任务进度页面

- 标记：[P]
- 目标文件：`frontend/src/pages/ProgressPage.vue`
- 依赖：U006
- 内容：按目录展示未开始、进行中、已完成状态。
- 验收：有权限成员可切换状态。

### Phase 6 / U021 [P] 创建打包检查页面

- 标记：[P]
- 目标文件：`frontend/src/pages/PackageCheckPage.vue`
- 依赖：U009
- 内容：展示风险列表、清理建议、忽略风险继续打包入口。
- 验收：清理前展示将处理对象。

### Phase 6 / U022 [P] 创建打包导出页面

- 标记：[P]
- 目标文件：`frontend/src/pages/PackageExportPage.vue`
- 依赖：U009
- 内容：填写压缩包文件名、选择格式、展示覆盖提示和导出进度。
- 验收：支持 zip、7z、tar.gz。

### Phase 6 / U023 [P] 创建邮件草稿页面

- 标记：[P]
- 目标文件：`frontend/src/pages/MailDraftPage.vue`
- 依赖：U010
- 内容：编辑收件人、主题、正文、附件格式和发送确认。
- 验收：提示推荐 zip 作为附件格式。

### Phase 6 / U024 [P] 创建操作记录页面

- 标记：[P]
- 目标文件：`frontend/src/pages/OperationLogPage.vue`
- 依赖：U011
- 内容：展示记录列表、类型筛选、操作人筛选、时间筛选。
- 验收：只读用户不可进入。

### Phase 6 / U025 [P] 创建通知中心页面

- 标记：[P]
- 目标文件：`frontend/src/pages/NotificationPage.vue`
- 依赖：U011
- 内容：展示通知列表、已读未读状态、按项目查看。
- 验收：支持标记已读。

### Phase 6 / U026 更新路由表

- 目标文件：`frontend/src/router/index.ts`
- 依赖：U012-U025
- 内容：注册登录、首页、小组详情、项目工作台、文件、进度、检查、打包、邮件、记录、通知路由。
- 验收：受保护页面需要登录态。

### Phase 6 / U027 [P] 编写首页组件测试

- 标记：[P]
- 目标文件：`frontend/src/pages/HomePage.spec.ts`
- 依赖：U014
- 内容：测试最近项目、小组筛选和项目搜索入口展示。
- 验收：组件测试通过。

### Phase 6 / U028 [P] 编写文件管理组件测试

- 标记：[P]
- 目标文件：`frontend/src/pages/FileManagerPage.spec.ts`
- 依赖：U017, U018, U019
- 内容：测试文件列表基本信息、同名弹窗、回收站入口。
- 验收：组件测试通过。

### Phase 6 / U029 [P] 编写打包页面组件测试

- 标记：[P]
- 目标文件：`frontend/src/pages/PackageExportPage.spec.ts`
- 依赖：U021, U022
- 内容：测试格式选择、文件名校验、覆盖提示。
- 验收：组件测试通过。

### Phase 6 / U030 [P] 编写邮件草稿组件测试

- 标记：[P]
- 目标文件：`frontend/src/pages/MailDraftPage.spec.ts`
- 依赖：U023
- 内容：测试草稿字段可编辑、发送前确认、zip 推荐提示。
- 验收：组件测试通过。

### Phase 6 / U031 编写端到端协作流程测试

- 目标文件：`frontend/e2e/collaboration-flow.spec.ts`
- 依赖：U026, W001-W033, I019
- 内容：测试登录、进入项目、上传文件、查看记录、打包检查、生成压缩包。
- 验收：核心协作流程可跑通。

### Phase 6 / U032 编写端到端邮件草稿流程测试

- 目标文件：`frontend/e2e/mail-draft-flow.spec.ts`
- 依赖：U023, W008, I018
- 内容：测试基于最近压缩包生成草稿、修改草稿、发送失败降级提示。
- 验收：不调用真实邮箱凭据。

## 任务依赖总览

建议执行顺序：

1. Phase 1 全部任务。
2. Phase 2 的 Domain Tests。
3. Phase 2 的 Domain Implementation。
4. Phase 3 的 Application Tests。
5. Phase 3 的 Application Implementation。
6. Phase 4 的 API Tests。
7. Phase 4 的 Web API Implementation。
8. Phase 5 的 Infrastructure & Integration。
9. Phase 6 的 Frontend UI & Interaction。

关键依赖链：

```text
F001 -> D001-D013 -> D014-D036 -> A001-A019 -> A020-A043 -> W001-W011 -> W012-W033 -> I001-I025 -> U001-U032
```

最后生成时间：2026-05-24 15:55:17

