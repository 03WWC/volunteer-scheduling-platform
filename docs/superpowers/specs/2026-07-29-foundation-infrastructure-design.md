# 志愿者智能调度平台基础架构设计

## 1. 目标与范围

本设计覆盖开发任务清单的第一阶段“基础架构搭建”，交付一个可构建、可启动、可验证的微服务工程基线。

本阶段包含：

- 初始化 Git 仓库与 Maven 多模块工程。
- 建立 `common-core`、`gateway`、`user-service` 三个模块。
- 使用 Nacos 完成服务注册与发现。
- 通过 Gateway 将 `/api/users/**` 转发至 `user-service`。
- 使用 Docker Compose 提供 Nacos、MySQL、Redis、RocketMQ 本地开发环境。
- 建立统一的基础配置、日志、健康检查和自动化测试。
- 提供脱敏的环境变量模板和启动说明。

本阶段不包含用户注册、登录、JWT、RBAC、数据库表或其他业务功能。这些内容属于后续用户系统阶段。

## 2. 技术基线

| 组件 | 版本或约束 |
|------|------------|
| Java | 21 |
| Maven | 3.9+ |
| Spring Boot | 3.5.0 |
| Spring Cloud | 2025.0.0 |
| Spring Cloud Alibaba | 2025.0.0.0 |
| Nacos | 3.0.3 |
| RocketMQ | 5.3.1 |
| MySQL | 8.4 |
| Redis | 7.4 |
| Docker Compose | Compose Specification |

该组合采用 Spring Cloud Alibaba 官方 2025.0.x 兼容矩阵。依赖版本统一由根 Maven 工程的 BOM 管理，子模块不得自行覆盖 Spring 生态组件版本。

## 3. 工程结构

```text
volunteer-scheduling-platform/
├── pom.xml
├── .gitignore
├── .env.example
├── README.md
├── common/
│   └── common-core/
│       ├── pom.xml
│       └── src/
├── gateway/
│   ├── pom.xml
│   └── src/
├── user-service/
│   ├── pom.xml
│   └── src/
├── deploy/
│   └── docker-compose.yml
└── docs/
```

各单元职责：

- 根 `pom.xml`：聚合模块、统一 Java 版本、BOM、插件和测试约束。
- `common-core`：仅存放可跨服务复用且与 Web 框架无关的基础类型；本阶段不预置业务模型。
- `gateway`：服务发现、动态路由、跨服务入口和基础可观测性。
- `user-service`：作为第一个真实业务服务骨架，提供探活接口，并为下一阶段用户系统直接复用。
- `deploy/docker-compose.yml`：只负责本地基础设施，不构建或运行应用模块。

## 4. 运行架构

```text
客户端
  │ GET /api/users/ping
  ▼
Gateway :8080
  │ Nacos 服务发现 + lb://user-service
  ▼
User Service :8081
  │
  └── GET /ping

Nacos :8848 / :9848
MySQL :3306
Redis :6379
RocketMQ NameServer :9876
RocketMQ Broker :10911
```

Gateway 使用 Spring Cloud Gateway Server WebFlux。路由规则由应用配置明确声明，目标使用 `lb://user-service`；不启用“发现所有服务并自动暴露”的 locator 模式，避免内部服务意外形成公开路由。

## 5. 配置设计

应用配置采用“默认值适用于本地、环境变量可覆盖”的原则：

- `NACOS_SERVER_ADDR`：Nacos 地址，默认 `127.0.0.1:8848`。
- `NACOS_USERNAME`、`NACOS_PASSWORD`：Nacos 客户端认证。
- `SERVER_PORT`：服务监听端口。
- `SPRING_PROFILES_ACTIVE`：环境配置选择，默认 `local`。

仓库仅提交 `.env.example`，其中不包含真实密码、API Key 或公网服务地址。已有 `.env` 必须被 `.gitignore` 排除。

测试配置使用 `test` Profile，关闭 Nacos 注册与远程配置读取，使测试不依赖本机容器或网络。

## 6. 服务接口与数据流

### 6.1 User Service 探活接口

```http
GET /ping
```

成功响应：

```json
{
  "service": "user-service",
  "status": "UP"
}
```

该接口只用于验证服务骨架和网关链路，不代表用户系统业务 API。

### 6.2 网关转发

```text
GET /api/users/ping
  -> Path=/api/users/**
  -> StripPrefix=2
  -> lb://user-service/ping
```

请求链路为：

1. 客户端访问 Gateway。
2. Gateway 根据 Nacos 中的实例列表选择健康的 `user-service`。
3. Gateway 去掉 `/api/users` 前缀并转发 `/ping`。
4. User Service 返回 JSON。
5. Gateway 原样返回 HTTP 状态和响应体。

## 7. 健康检查与错误处理

- Gateway 和 User Service 均启用 Spring Boot Actuator。
- `/actuator/health` 对本机开放，不公开环境变量、Bean、配置详情等敏感端点。
- Docker Compose 为 MySQL、Redis、Nacos、RocketMQ 配置健康检查；存在依赖的容器必须等待其依赖健康后启动。
- 找不到路由时返回 HTTP 404。
- `user-service` 无可用实例时，Gateway 返回 HTTP 503。
- 应用配置缺失或依赖版本冲突时应启动失败，不使用静默降级隐藏配置错误。
- 本阶段不引入熔断、重试和自定义错误协议，避免在没有真实业务流量前过度设计。

## 8. 本地基础设施与安全

- Nacos 使用单机模式，仅用于本地开发；开启认证并从环境变量读取认证材料。
- MySQL、Redis 和 RocketMQ 使用固定镜像版本，不使用 `latest`。
- 数据卷使用具名卷，`docker compose down` 默认保留数据。
- 服务端口只绑定开发机需要访问的端口。
- 生产环境不得复用本地 Compose 配置；生产 Nacos 必须采用集群、内部网络和独立密钥。
- 真实 `.env`、日志、构建产物和 IDE 文件不得提交。

## 9. 测试与验收

### 9.1 自动化测试

- `common-core`：模块可独立编译，禁止引入 Web 或数据库依赖。
- `user-service`：验证 `/ping` 返回 HTTP 200 和规定的 JSON。
- `gateway`：验证路由配置能够匹配 `/api/users/**`，并正确移除两段路径前缀。
- 所有应用：使用 `test` Profile 完成 Spring Context 启动测试。
- 根工程执行 `mvn verify` 必须全部通过。

### 9.2 配置验证

- `docker compose config` 必须成功。
- `.env.example` 包含 Compose 和应用启动所需的全部变量。
- 仓库忽略规则必须确认真实 `.env` 不会进入版本控制。

### 9.3 端到端验收

1. Docker Compose 中所有基础设施达到 healthy 状态。
2. User Service 启动并成功注册到 Nacos。
3. Gateway 启动并能发现 User Service。
4. `GET http://localhost:8080/api/users/ping` 返回 HTTP 200。
5. 停止 User Service 后，同一请求返回 HTTP 503。
6. `mvn verify` 与 `docker compose config` 均成功。

## 10. 交付边界

本阶段完成后，下一阶段可直接在 `user-service` 中实现注册、登录、JWT 和 RBAC，而无需重新搭建微服务基础设施。其他业务服务应在各自开发阶段按同一约定增加，不在本阶段一次性创建空模块。

## 11. 设计依据

- Spring Cloud Alibaba 2025.x 版本兼容关系：<https://sca.aliyun.com/en/docs/2025.x/overview/version-explain/>
- Spring Cloud Gateway 参考文档：<https://docs.spring.io/spring-cloud-gateway/reference/>
- Nacos Docker 快速开始：<https://nacos.io/en/docs/latest/quickstart/quick-start-docker/>
