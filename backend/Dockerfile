# syntax=docker/dockerfile:1

# 构建阶段负责编译 Spring Boot 可执行 Jar。
FROM maven:3.9.9-eclipse-temurin-17 AS build

# WORKDIR 指定容器内后端源码目录。
WORKDIR /workspace

# 先复制 pom.xml 以便 Docker 缓存 Maven 依赖层。
COPY pom.xml .

# 预取依赖，减少源码变更后的重复下载成本。
RUN mvn -B -DskipTests dependency:go-offline

# 复制后端源码和配置。
COPY src ./src

# 打包后端服务，容器镜像构建阶段不运行测试。
RUN mvn -B -DskipTests package

# 运行阶段只保留 JRE 和应用 Jar。
FROM eclipse-temurin:17-jre

# WORKDIR 指定应用运行目录。
WORKDIR /app

# SERVER_PORT 指定 Spring Boot 监听端口，默认与 Compose 配置一致。
ENV SERVER_PORT=8080

# 复制构建阶段生成的可执行 Jar。
COPY --from=build /workspace/target/cooperation-backend-0.0.1-SNAPSHOT.jar /app/app.jar

# EXPOSE 声明默认服务端口。
EXPOSE 8080

# ENTRYPOINT 启动 Spring Boot 应用。
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
