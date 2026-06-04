# Cooperation —— 学生小组协作与项目提交平台

## 这是什么？

一个帮助同学们完成小组作业的协作工具。它的工作方式是：

1. **负责人**创建一个项目，设置好目录结构（比如"前端"、"后端"、"报告"）
2. **成员们**各自把自己负责的文件上传到对应目录
3. **负责人**一键检查有没有问题（空目录？缓存文件？），然后打包导出或生成邮件草稿发给老师

不用再手动收集每个人的文件、来回传压缩包了。

> 本项目采用 **SDD（Specification-Driven Development，规格驱动开发）** 方法论，所有功能先写需求规格说明（见 `specs/spec.md`），再按任务计划（见 `specs/tasks.md`）逐步实现。开发流程遵循 [AGENTS.md](./AGENTS.md) 中的约定。
> SMTP 邮件配置等专项设计已整合进主需求规格 `specs/spec.md`，不再维护单独的 `docs/specs` 子文档。

---

## 你需要准备的

在开始之前，你的电脑需要安装以下工具：

### 必须安装

| 工具 | 用途 | 下载地址 |
|---|---|---|
| **Docker Desktop** | 一键运行所有服务（数据库、后端、前端） | https://www.docker.com/products/docker-desktop/ |
| **Git** | 下载代码 | https://git-scm.com/downloads |

### 如果你要开发（改代码）

| 工具 | 用途 | 下载地址 |
|---|---|---|
| **JDK 17** | 编译和运行后端 Java 代码 | https://adoptium.net/download/ |
| **Maven** | 管理后端依赖 | https://maven.apache.org/download.cgi |
| **Node.js 18+** | 运行前端代码 | https://nodejs.org/ |
| **MySQL 8.4** | 本地数据库 | https://dev.mysql.com/downloads/mysql/ |

> 如果你只是想用这个系统，不是改代码，只装 Docker Desktop 就够了。

---

## 快速启动（适合想先跑起来看看的新手）

### 第一步：下载代码

打开终端（Windows 用户搜索"命令提示符"或"PowerShell"），输入：

```bash
git clone https://github.com/<你的用户名>/cooperation.git
cd cooperation
```

> 如果你还不知道 GitHub 是什么，也可以直接下载 ZIP 压缩包，解压后进入文件夹。

### 第二步：创建配置文件

项目里有一个 `.env.example` 文件，这是一个"配置模板"。你需要复制一份，改名为 `.env`：

**推荐：运行配置向导**

如果你想通过向导生成 Docker 环境配置，可以运行：

```powershell
.\scripts\init-env.ps1
```

脚本会自动生成 `.env`，依次询问数据库业务用户名、业务密码、MySQL root 密码、MySQL 端口、后端端口和前端端口，并自动生成 `APP_AES_KEY`。如果端口已被占用，脚本会提醒你确认是否继续使用。SMTP 默认不配置，后续可在系统个人中心里添加。

**手动复制模板**

**Windows (PowerShell)：**
```powershell
copy .env.example .env
```

**Mac / Linux：**
```bash
cp .env.example .env
```

> `.env` 文件里存的是数据库密码、邮箱密码等敏感信息。这个文件已经被 git 忽略了，不会上传到 GitHub，所以你可以放心填写。

如果你只是想本地试试，`.env` 文件里的内容**不用修改**，默认值就能跑起来。但如果你要部署到服务器上，记得把里面的 `change_me` 和 `change_root_me` 改成你自己的强密码。

### 配置应该写在哪里

| 使用方式 | 配置文件 | 说明 |
|---|---|---|
| Docker Compose 一键启动 | `.env` | 数据库账号密码、端口、`APP_AES_KEY`、SMTP 等环境变量都写这里 |
| 本地手动启动后端 | `backend/src/main/resources/application-local.yml` | 只在不使用 Docker、直接运行 Spring Boot 时使用 |
| 配置模板 | `.env.example`、`application-local.example.yml` | 只放占位值和说明，不填写真实密码 |

> 推荐默认使用 Docker Compose：复制 `.env.example` 为 `.env` 后，只改 `.env`。不要把真实密码、邮箱授权码或生产配置写进 `application.yml`、`.env.example` 或源码。

### 第三步：一键启动

确保 Docker Desktop 正在运行（任务栏右下角能看到 Docker 的小鲸鱼图标），然后执行：

```bash
docker compose up --build
```

第一次运行会下载镜像和编译代码，可能需要 **5~10 分钟**。等看到类似下面的输出就说明启动成功了：

```
cooperation-backend  | Started CooperationApplication in 8.5 seconds
cooperation-frontend | VITE v5.x.x  ready in 350 ms
```

### 第四步：打开浏览器

| 服务 | 地址 |
|---|---|
| 前端页面 | http://localhost:5173 |
| 后端 API | http://localhost:8080 |

---

## 停止和重启

- **停止：** 在终端里按 `Ctrl + C`，Docker 会自动关闭所有服务。
- **再次启动：** 进入项目目录，再次运行 `docker compose up --build`。
- **彻底清理（删除数据库数据）：** `docker compose down -v`

---

## 功能特性

### 核心功能

- **小组管理**：创建小组、邀请成员、管理成员权限
- **账号与个人中心**：注册、登录、维护个人资料、修改密码
- **项目管理**：创建项目、查看项目、删除项目、管理项目状态
- **文件管理**：上传、下载、移动、删除文件、文件评论
- **目录管理**：创建、删除目录，设置目录状态
- **回收站**：查看、恢复和清空项目回收站
- **进度追踪**：查看项目完成进度、目录状态统计
- **打包导出**：打包前检查、清理建议、导出压缩包
- **邮件草稿**：生成邮件草稿、编辑草稿、选择 SMTP 配置发送邮件
- **邮件设置**：在个人中心维护 SMTP/IMAP 配置、发送测试邮件、设置默认发件配置
- **操作记录**：查看所有操作历史
- **通知系统**：接收平台内通知

### 页面列表

| 页面 | 路径 | 说明 |
|---|---|---|
| 项目总览 | `/` | 显示所有项目卡片，可创建小组和项目 |
| 登录 | `/login` | 用户登录 |
| 注册 | `/register` | 创建新用户账号 |
| 个人中心 | `/profile` | 维护个人资料、密码和邮件配置 |
| 小组详情 | `/groups/:groupId` | 查看小组信息和成员 |
| 项目工作台 | `/projects/:projectId` | 项目文件管理、目录操作 |
| 打包检查 | `/projects/:projectId/package/check` | 打包前检查问题 |
| 打包导出 | `/projects/:projectId/package/export` | 导出压缩包 |
| 邮件草稿 | `/projects/:projectId/mail` | 生成和发送邮件 |
| 操作记录 | `/projects/:projectId/logs` | 查看操作历史 |
| 通知 | `/notifications` | 查看通知消息 |
| 邮件总览 | `/mail-drafts` | 查看所有邮件草稿 |
| 邀请加入 | `/join/:code` | 通过邀请链接加入项目 |

---

## 项目目录结构（给想了解代码的同学）

```
cooperation/
├── delivery/                       交付索引（集中说明 SDD 文档、测试和验收报告位置）
│   ├── README.md                   交付材料索引
│   └── unit-tests.md               单元测试与集成测试清单
├── backend/                       后端（Spring Boot）
│   ├── src/main/java/              Java 源码
│   │   └── com/cooperation/
│   │       ├── application/        业务逻辑层（用例、命令、查询）
│   │       ├── domain/             领域模型层（实体、仓库接口）
│   │       ├── infrastructure/     基础设施层（数据库、外部服务）
│   │       └── web/                Web 层（控制器、请求/响应模型）
│   ├── src/main/resources/         配置文件
│   ├── src/test/                   后端测试（JUnit 5、Testcontainers）
│   ├── Dockerfile                  后端 Docker 镜像
│   └── pom.xml                     Maven 依赖管理
├── frontend/                       前端（Vue 3）
│   ├── src/
│   │   ├── pages/                  页面组件
│   │   ├── components/             通用组件
│   │   ├── stores/                 Pinia 状态管理
│   │   ├── services/               API 请求封装
│   │   ├── router/                 路由配置
│   │   ├── types/                  TypeScript 类型定义
│   │   ├── layouts/                布局组件
│   │   └── **/*.spec.ts            前端单元测试（Vitest）
│   ├── index.html                  入口 HTML
│   ├── Dockerfile                  前端 Docker 镜像
│   └── package.json                npm 依赖管理
├── specs/                          SDD 需求文档和任务计划
│   ├── spec.md                     需求规格说明
│   ├── plan.md                     实施计划
│   ├── tasks.md                    开发任务清单
│   ├── test-report.md              项目验收测试报告
│   └── agent-dispatch-plan.md      Agent 调度计划
├── data/                           运行时数据（已 git 忽略）
├── docker-compose.yml              一键部署编排文件
├── .env.example                    环境变量模板
├── AGENTS.md                       开发约定与 Agent 规范
└── README.md                       本文件
```

---

## 本地开发（写给要改代码的贡献者）

### 后端开发

**准备工作：** 你需要一个本地 MySQL。用 Docker 单独启动一个也可以：

```bash
docker run -d --name mysql-dev \
  -e MYSQL_ROOT_PASSWORD=change_root_me \
  -e MYSQL_DATABASE=cooperation \
  -e MYSQL_USER=cooperation_user \
  -e MYSQL_PASSWORD=change_me \
  -p 3306:3306 \
  mysql:8.4
```

然后创建本地配置：

```bash
cp backend/src/main/resources/application-local.example.yml backend/src/main/resources/application-local.yml
```

> 把 `application-local.yml` 里的密码改成你本地 MySQL 的密码。这个文件已被 git 忽略，不会提交。

启动后端：

```bash
cd backend
mvn spring-boot:run
```

运行测试：

```bash
mvn test
```

### 前端开发

```bash
cd frontend
npm install      # 第一次运行需要装依赖
npm run dev      # 启动开发服务器
```

运行测试：

```bash
npm run test:unit      # 单元测试
npm run test:e2e       # E2E 测试（需要先装 Playwright 浏览器）
```

---

## 常见问题

### Docker 启动报错 "port is already allocated"

说明你电脑的 3306、8080 或 5173 端口已经被占用了。修改 `.env` 文件里的端口号：

```env
MYSQL_PORT=3307
BACKEND_PORT=8081
FRONTEND_PORT=5174
VITE_API_BASE_URL=http://localhost:8081
```

然后重新启动。

### 后端启动失败，日志显示连不上数据库

1. 确认 Docker Desktop 正在运行
2. 进入项目目录，执行 `docker compose down -v` 清理旧数据，再重新 `docker compose up --build`
3. 如果还是不行，检查 `.env` 文件里的数据库配置是否正确

### 前端页面打开了，但是登录不了 / 请求后端报错

检查 `.env` 文件里的 `VITE_API_BASE_URL` 是否和后端地址一致。本地默认是 `http://localhost:8080`。

### 我想改完代码部署到我的服务器上

1. 把项目上传到你的 Git 仓库
2. 在服务器上克隆代码，创建 `.env` 并填入服务器的真实配置
3. 运行 `docker compose up --build`
4. 记得把 `.env` 里所有密码都改成强密码，不要用默认值

### npm install 很慢

设置国内镜像：

```bash
npm config set registry https://registry.npmmirror.com
```

### Maven 下载依赖很慢

在 `backend/pom.xml` 的 `<project>` 标签内添加：

```xml
<repositories>
    <repository>
        <id>aliyun</id>
        <url>https://maven.aliyun.com/repository/public</url>
    </repository>
</repositories>
```

---

## 技术栈总览

| 层级 | 技术 | 版本 |
|---|---|---|
| 后端框架 | Spring Boot | 3.3.x |
| 安全框架 | Spring Security | — |
| ORM | MyBatis-Plus | 3.5.x |
| 数据库 | MySQL | 8.4 |
| 数据库迁移 | Flyway | — |
| 前端框架 | Vue | 3.5.x |
| 构建工具 | Vite | 5.4.x |
| UI 组件库 | Element Plus | 2.8.x |
| 状态管理 | Pinia | 2.2.x |
| HTTP 客户端 | Axios | 1.7.x |
| 后端测试 | JUnit 5 + Testcontainers | — |
| 前端测试 | Vitest + Playwright | — |
| 部署 | Docker Compose | V2 |

---

## 配置约定

- 所有密码、Token、邮箱授权码不得写入源码或示例配置
- `.env.example` 和 `application-local.example.yml` 只包含占位值（如 `change_me`）
- 真实的 `.env` 和 `application-local.yml` 已加入 `.gitignore`，不会被提交到仓库
- 后端所有敏感配置通过环境变量注入，不硬编码

---

最后更新时间：2026-06-04 09:15:00
