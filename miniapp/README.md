# 志愿者智能调度平台小程序

## 启动

```bash
pnpm install
pnpm dev:h5
```

微信小程序开发：

```bash
pnpm dev:mp-weixin
```

构建产物位于 `dist/dev/mp-weixin`，使用微信开发者工具打开该目录，并在 `src/manifest.json` 中填写真实 AppID。

## 环境配置

- 开发环境：修改 `.env.development` 中的 `VITE_API_BASE_URL`。
- 生产环境：修改 `.env.production` 中的 `VITE_API_BASE_URL`。
- 正式微信登录需要后端增加 `code` 换取 `openid` 的接口；当前基础工程直接调用现有 `/user/wechat/login`。

## 校验

```bash
pnpm test
pnpm build:h5
pnpm build:mp-weixin
```
