# Docker 开发环境说明

## 基础组件

`docker-compose.yml` 提供以下组件：

- MySQL 8.0：端口 `13306`
- Redis 7.2：端口 `6379`
- RabbitMQ 3.13 Management：端口 `5672`，控制台端口 `15672`
- Nacos 2.3.2：端口 `8848`

## 启动基础组件

```bash
docker compose up -d mysql redis rabbitmq nacos
```

MySQL 首次启动会自动执行：

```bash
sql/01-init-schema.sql
```

如果数据库已经存在，并且需要升级消息通知幂等字段，执行：

```bash
sql/02-add-message-notice-event-key.sql
```

## 后端服务镜像

先在宿主机完成 Maven 打包：

```bash
mvn -DskipTests package
```

再按模块构建镜像，例如：

```bash
docker build -t volunteer-gateway:0.0.1 gateway
docker build -t volunteer-activity-service:0.0.1 activity-service/activity-service-server
docker build -t volunteer-user-service:0.0.1 user-service/user-service-server
docker build -t volunteer-schedule-service:0.0.1 schedule-service/schedule-service-server
docker build -t volunteer-dispatch-service:0.0.1 dispatch-service/dispatch-service-server
docker build -t volunteer-location-service:0.0.1 location-service/location-service-server
docker build -t volunteer-settlement-service:0.0.1 settlement-service/settlement-service-server
docker build -t volunteer-message-service:0.0.1 message-service/message-service-server
docker build -t volunteer-ai-scheduler:0.0.1 ai-scheduler/ai-scheduler-server
```

## 组件账号

RabbitMQ 默认使用项目配置中的账号；MySQL 默认 root 密码与当前服务配置保持一致。

生产环境建议改为环境变量或密钥管理，不要继续硬编码在配置文件中。
