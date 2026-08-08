# Unified Auth Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add real JWT-based login for the admin app and miniapp.

**Architecture:** `common-core` provides JWT utilities, `user-service` signs auth sessions, and `gateway` validates tokens at the edge. Frontend clients store server-issued tokens.

**Tech Stack:** Java 17, Spring Boot, Spring Cloud Gateway, MyBatis, Vue 3, Pinia, uni-app.

## Global Constraints

- Keep admin credentials configuration-backed in this stage.
- Use JDK crypto APIs for JWT, no extra JWT library.
- Do not change existing database tables for this step.
- Keep public route configuration in `platform.security.public-paths`.

---

### Task 1: Common JWT Utility

**Files:**
- Create: `common-core/src/main/java/com/volunteer/platform/common/auth/JwtProperties.java`
- Create: `common-core/src/main/java/com/volunteer/platform/common/auth/JwtTokenService.java`
- Test: `common-core/src/test/java/com/volunteer/platform/common/auth/JwtTokenServiceTest.java`

**Interfaces:**
- Produces: `JwtTokenService.issue(AuthSubject subject, Duration ttl)`
- Produces: `JwtTokenService.parse(String token)`

- [x] Write failing tests for issue, parse, tamper rejection, expiry rejection.
- [x] Run common-core tests and verify the new tests fail because classes are missing.
- [x] Implement the minimal JWT utility.
- [ ] Run common-core tests and verify they pass. Blocked by local Maven test classpath issue: testCompile cannot see module target classes.

### Task 2: User Auth Sessions

**Files:**
- Create: `user-service/user-service-server/src/main/java/com/volunteer/platform/user/config/AdminAuthProperties.java`
- Create: `user-service/user-service-server/src/main/java/com/volunteer/platform/user/dto/AdminLoginDTO.java`
- Create: `user-service/user-service-server/src/main/java/com/volunteer/platform/user/vo/AuthSessionVO.java`
- Modify: `user-service/user-service-server/src/main/java/com/volunteer/platform/user/service/UserService.java`
- Modify: `user-service/user-service-server/src/main/java/com/volunteer/platform/user/service/impl/UserServiceImpl.java`
- Modify: `user-service/user-service-server/src/main/java/com/volunteer/platform/user/controller/UserController.java`
- Test: `user-service/user-service-server/src/test/java/com/volunteer/platform/user/service/UserServiceImplTest.java`

**Interfaces:**
- Consumes: `JwtTokenService.issue(...)`
- Produces: `UserService.adminLogin(AdminLoginDTO dto): AuthSessionVO`
- Produces: `UserService.wechatLogin(WechatLoginDTO dto): AuthSessionVO`

- [x] Write failing tests for admin login success/failure and WeChat token issuance.
- [x] Run user-service tests and verify the new tests fail.
- [x] Implement session VO, DTO, config, controller, and service changes.
- [ ] Run user-service tests and verify they pass. Blocked by local Maven test classpath issue: testCompile cannot see module target classes.

### Task 3: Gateway Token Validation

**Files:**
- Modify: `gateway/src/main/java/com/volunteer/platform/gateway/config/SecurityProperties.java`
- Modify: `gateway/src/main/java/com/volunteer/platform/gateway/filter/JwtAuthGatewayFilter.java`
- Test: `gateway/src/test/java/com/volunteer/platform/gateway/filter/JwtAuthGatewayFilterTest.java`

**Interfaces:**
- Consumes: `JwtTokenService.parse(String token)`
- Produces: Valid requests carry `X-User-Id`, `X-User-Type`, `X-User-Name`.

- [x] Write failing tests for invalid token rejection and identity header forwarding.
- [x] Run gateway tests and verify they fail.
- [x] Implement token validation and header mutation.
- [ ] Run gateway tests and verify they pass. Blocked by local Maven test classpath issue: testCompile cannot see module target classes.

### Task 4: Client Login Integration

**Files:**
- Modify: `frontend-admin/src/api/modules.ts`
- Modify: `frontend-admin/src/stores/auth.ts`
- Modify: `frontend-admin/src/views/LoginView.vue`
- Modify: `miniapp/src/api/modules.ts`
- Modify: `miniapp/src/stores/user.ts`
- Modify: `miniapp/src/pages/login/index.vue`

**Interfaces:**
- Consumes: `/auth/admin/login`
- Consumes: `/user/wechat/login`
- Stores: `admin-token`, `volunteer-token`.

- [x] Add client API functions and auth session types.
- [x] Change admin login to call backend and store token.
- [x] Change miniapp login to store backend token.
- [x] Run typecheck/build tests for both clients.

### Task 5: Config And Verification

**Files:**
- Modify: `gateway/src/main/resources/application.yml`
- Modify: `user-service/user-service-server/src/main/resources/application.yml`
- Modify: `frontend-admin/README.md`
- Modify: `miniapp/README.md`

**Interfaces:**
- Configures: JWT secret and admin credentials.

- [x] Add matching JWT config to gateway and user-service.
- [x] Keep `/auth/**` public and protected APIs guarded.
- [ ] Run backend module tests and frontend builds. Backend main compile and frontend builds pass; backend testCompile is blocked by local Maven test classpath issue.
- [x] Record any residual limitations.
