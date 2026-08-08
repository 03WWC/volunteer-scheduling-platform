# 管理后台与小程序基础工程实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 生成可运行、可继续接入后端接口的管理后台与 uni-app 小程序基础工程。

**Architecture:** 两个客户端独立构建并统一通过网关访问后端。请求响应解析、登录态和业务 API 封装集中管理，页面只负责展示与交互。

**Tech Stack:** Vue 3、Vite、TypeScript、Element Plus、Vue Router、Pinia、Axios、uni-app、Vitest。

## Global Constraints

- 管理后台目录固定为 `frontend-admin/`，小程序目录固定为 `miniapp/`。
- 开发环境 API 默认地址为 `http://localhost:8080`。
- 页面文本使用中文，样式适合运营后台和志愿者移动端。
- 不修改或回滚现有后端文件。

---

### Task 1: 管理后台工程与请求层

**Files:**
- Create: `frontend-admin/package.json`
- Create: `frontend-admin/vite.config.ts`
- Create: `frontend-admin/src/api/result.ts`
- Create: `frontend-admin/src/api/request.ts`
- Test: `frontend-admin/src/api/result.test.ts`

**Interfaces:**
- Produces: `unwrapApiResult<T>(result: ApiResult<T>): T`
- Produces: `request<T>(config: AxiosRequestConfig): Promise<T>`

- [x] **Step 1: 写请求结果解析失败测试**
- [x] **Step 2: 安装依赖并运行测试，确认因实现缺失而失败**
- [x] **Step 3: 实现请求结果解析和 Axios 客户端**
- [x] **Step 4: 运行测试，确认通过**

### Task 2: 管理后台布局与业务页面

**Files:**
- Create: `frontend-admin/src/main.ts`
- Create: `frontend-admin/src/router/index.ts`
- Create: `frontend-admin/src/layouts/AdminLayout.vue`
- Create: `frontend-admin/src/views/LoginView.vue`
- Create: `frontend-admin/src/views/DashboardView.vue`
- Create: `frontend-admin/src/views/ModuleWorkbench.vue`
- Create: `frontend-admin/src/styles/global.css`

**Interfaces:**
- Consumes: `request<T>()`
- Produces: 可访问的登录、仪表盘和业务模块路由

- [x] **Step 1: 建立应用入口、路由和登录状态**
- [x] **Step 2: 实现后台导航、顶栏和响应式布局**
- [x] **Step 3: 实现仪表盘和通用业务工作台**
- [x] **Step 4: 执行类型检查和生产构建**

### Task 3: 小程序工程与请求层

**Files:**
- Create: `miniapp/package.json`
- Create: `miniapp/vite.config.ts`
- Create: `miniapp/src/api/result.ts`
- Create: `miniapp/src/api/request.ts`
- Test: `miniapp/src/api/result.test.ts`

**Interfaces:**
- Produces: `unwrapApiResult<T>(result: ApiResult<T>): T`
- Produces: `request<T>(options: UniApp.RequestOptions): Promise<T>`

- [x] **Step 1: 写请求结果解析失败测试**
- [x] **Step 2: 安装依赖并运行测试，确认因实现缺失而失败**
- [x] **Step 3: 实现请求结果解析和 uni.request 客户端**
- [x] **Step 4: 运行测试，确认通过**

### Task 4: 小程序页面与业务接口

**Files:**
- Create: `miniapp/src/pages.json`
- Create: `miniapp/src/main.ts`
- Create: `miniapp/src/pages/login/index.vue`
- Create: `miniapp/src/pages/home/index.vue`
- Create: `miniapp/src/pages/activity/list.vue`
- Create: `miniapp/src/pages/schedule/list.vue`
- Create: `miniapp/src/pages/checkin/index.vue`
- Create: `miniapp/src/pages/message/list.vue`
- Create: `miniapp/src/pages/income/list.vue`
- Create: `miniapp/src/pages/mine/index.vue`

**Interfaces:**
- Consumes: `request<T>()`
- Produces: 登录、活动、排班、签到、消息、收入和个人中心页面

- [x] **Step 1: 建立页面注册和 TabBar**
- [x] **Step 2: 实现登录、首页和核心列表页面**
- [x] **Step 3: 实现扫码签到、消息、收入和个人中心**
- [x] **Step 4: 执行测试、类型检查和 H5 生产构建**

### Task 5: 联调说明与整体验证

**Files:**
- Create: `frontend-admin/README.md`
- Create: `miniapp/README.md`

**Interfaces:**
- Produces: 两个客户端的启动、构建和 API 地址配置说明

- [x] **Step 1: 补充启动和环境配置说明**
- [x] **Step 2: 运行两个项目的全部测试和构建**
- [x] **Step 3: 启动管理后台并确认本地服务可访问**
