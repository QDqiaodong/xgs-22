# 产业园地下车库照明灯组分区批量划转管理系统

## 项目简介

本系统用于园区物业管理地下车库照明灯组，支持照明灯组建档、单组或批量分区划转、批量操作台账和分区资产导出。

## 技术栈

- 前端：Vue 3、Vite、Element Plus
- 后端：Spring Boot 3.1、JDK 17、MyBatis Plus、Redis
- 数据库：MySQL 8.0
- 部署：Docker Compose

## 端口说明

| 服务 | 地址或端口 |
| --- | --- |
| 前端访问地址 | http://localhost:8232 |
| 后端 API 地址 | http://localhost:8332/api |
| MySQL | 127.0.0.1:3532 |
| Redis | 127.0.0.1:6632 |

端口统一维护在根目录 `.env`，示例配置见 `.env.example`。

## 启动方式

```bash
cd /Users/Admin/Desktop/solo-0601/xgs-0701/xgs-组1/xgs-22
docker compose up -d --build
```

## 单独编译验证

```bash
cd backend
mvn compile -q
```

```bash
cd frontend
npm ci
npm run build
```

## Docker 构建说明

Docker Compose 使用固定端口并绑定 `127.0.0.1`；前端容器内部端口为 8117，外部访问端口由 `.env` 的 `FRONTEND_PORT=8232` 提供。

## 常见问题

- 后端编译失败时先执行 `mvn -version` 检查 JDK，再检查 Lombok、Maven 编译插件和 `pom.xml` 是否被忽略。
- 前端构建失败时优先按实际报错检查 import 路径、导出名、Vite 代理端口和构建期语法。
- 页面中文乱码时检查源码、SQL 初始化脚本、数据库字符集、连接串编码和已有 Docker volume 数据；初始化 SQL 已增加 `SET NAMES utf8mb4;`。
