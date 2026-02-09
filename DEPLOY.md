# 部署指南

本指南将协助你使用 Docker 和 Docker Compose 快速部署 Codehub Backend。

## 前置要求

- [Docker](https://www.docker.com/products/docker-desktop/) (建议安装 Docker Desktop)
- [Git](https://git-scm.com/)

## 快速开始

### 1. 配置环境变量

复制 `.env.example` 文件为 `.env`，并填入你的配置：

```bash
cp .env.example .env
```

打开 `.env` 文件，修改 `CODEHUB_DASH_SCOPE_KEY` 为你真实的阿里云 DashScope API Key。

### 2. 启动服务

使用 Docker Compose 一键构建并启动所有服务（应用 + 数据库）：

```bash
docker-compose up -d --build
```

- `--build`: 强制重新构建镜像
- `-d`: 后台运行

### 3. 验证部署

- **API 服务**: 访问 `http://localhost:8125`
- **接口文档**: 访问 `http://localhost:8125/doc.html` (Knife4j) 或 `http://localhost:8125/swagger-ui.html`
- **数据库**: 端口 `5432`，用户 `postgres`，密码 `password`，数据库 `codehub_vector`

## 常用命令

- **查看日志**:
  ```bash
  docker-compose logs -f
  ```
- **停止服务**:
  ```bash
  docker-compose down
  ```
- **清理所有数据 (慎用)**:
  ```bash
  docker-compose down -v
  ```

## 故障排查

1. **数据库连接失败**:
   - 检查 `docker-compose logs db` 确保数据库已正常启动。
   - 首次启动可能需要一点时间初始化数据库。

2. **AI 功能不可用**:
   - 检查 `.env` 文件中的 `CODEHUB_DASH_SCOPE_KEY` 是否正确。
   - 检查容器日志 `docker-compose logs app` 是否有报错。

3. **MCP 客户端未启用**:
   - 为了保证容器化部署的兼容性，默认在 Docker 中禁用了 MCP 客户端（因为涉及到本地 Node.js 路径）。
   - 如需启用，请参考 `docker-compose.yml` 中的注释进行配置。
