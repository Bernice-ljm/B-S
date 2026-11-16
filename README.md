# 企业审批系统

本仓库用于 BS 体系软件设计课程的企业审批系统项目。

## 目录结构

- `code/`：后端、前端、Docker Compose 编排文件和本地环境变量模板。
- `docs/`：课程任务书、设计文档、图稿及文档处理过程文件。

## 环境要求

- Java 21
- Node.js 22 或更高版本
- npm 10 或更高版本
- MySQL 8.x，或可运行 Docker Compose 的 Docker 环境

## 后端

在 Windows PowerShell 中启动：

```powershell
cd code/backend
.\mvnw.cmd spring-boot:run
```

在 Linux、macOS 或 WSL 中启动：

```bash
cd code/backend
./mvnw spring-boot:run
```

公开健康检查地址为 `http://localhost:8080/actuator/health`。认证功能完成前，其他后端路由默认拒绝访问。

运行后端构建和测试：

```powershell
cd code/backend
.\mvnw.cmd verify
```

## 前端

```powershell
cd code/frontend
npm install
npm run dev
```

Vite 开发服务器会将 `/actuator` 和 `/api` 请求代理到后端。生产构建命令如下：

```powershell
npm run build
```

## 容器化启动

先根据 `code/.env.example` 创建本地 `code/.env`，并设置数据库密码。随后在 `code/` 目录启动 MySQL 和后端：

```powershell
cd code
docker compose up --build
```

MySQL 数据保存在 `mysql-data` 卷中，附件保存在独立的 `attachment-volume` 卷中。后端启动时由 Flyway 自动执行 `code/backend/src/main/resources/db/migration/` 下的迁移脚本。

## 当前进度

项目已完成前后端工程骨架、MySQL 初始表结构、Flyway 配置和 Docker Compose 编排。认证、申请、审批、采购、差旅、报销及自定义流程等业务功能尚未实现。
