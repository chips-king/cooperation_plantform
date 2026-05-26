# syntax=docker/dockerfile:1

# 前端镜像使用 Node 运行 Vite 预览服务。
FROM node:20-alpine

# WORKDIR 指定前端工程目录。
WORKDIR /app

# VITE_API_BASE_URL 在构建阶段写入静态产物。
ARG VITE_API_BASE_URL=http://localhost:8080

# ENV 将构建参数暴露给 Vite。
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

# 先复制依赖清单以复用 npm install 缓存。
COPY package.json package-lock.json ./

# npm ci 按锁文件安装依赖，保证构建结果可复现。
RUN npm ci

# 复制前端源码。
COPY . .

# 构建生产静态资源。
RUN npm run build

# EXPOSE 声明 Vite 预览端口。
EXPOSE 5173

# CMD 启动 Vite 预览服务并监听容器网络。
CMD ["npm", "exec", "vite", "--", "preview", "--host", "0.0.0.0", "--port", "5173"]
