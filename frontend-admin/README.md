# 志愿者智能调度平台管理端

## 启动

```bash
pnpm install
pnpm dev
```

开发地址默认为 `http://localhost:5173`，后端网关默认为 `http://localhost:8080`。

## 环境配置

- 开发环境：修改 `.env.development` 中的 `VITE_API_BASE_URL`。
- 生产环境：修改 `.env.production` 中的 `VITE_API_BASE_URL`。

## 校验

```bash
pnpm test
pnpm build
```
