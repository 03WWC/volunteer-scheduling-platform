# Foundation Initialization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the initial Maven multi-module foundation for the volunteer scheduling platform.

**Architecture:** The root project manages shared dependency versions. `common-core` provides reusable web, exception, response, and Feign support. `gateway` provides the Spring Cloud Gateway entry point, route configuration, CORS, and a basic JWT presence filter.

**Tech Stack:** Java 17, Maven, Spring Boot 3.3.x, Spring Cloud 2023.0.x, Spring Cloud Alibaba 2023.0.1.x, OpenFeign, Sentinel, Nacos, JUnit 5.

## Global Constraints

- Docker development environment is intentionally skipped for this pass.
- Follow Alibaba Java conventions: 4-space indentation, K&R braces, 120-character max line length, DTO/VO/DO naming conventions.
- Use unified API response shape: `code`, `message`, `data`.
- Controller/business errors should flow through `BusinessException` and `GlobalExceptionHandler`.

---

### Task 1: Maven Skeleton and Common-Core Contracts

**Files:**
- Create: `pom.xml`
- Create: `.editorconfig`
- Create: `common-core/pom.xml`
- Create: `common-core/src/test/java/com/volunteer/platform/common/api/ResultTest.java`
- Create: `common-core/src/test/java/com/volunteer/platform/common/exception/GlobalExceptionHandlerTest.java`
- Create: `common-core/src/main/java/com/volunteer/platform/common/api/Result.java`
- Create: `common-core/src/main/java/com/volunteer/platform/common/exception/BusinessException.java`
- Create: `common-core/src/main/java/com/volunteer/platform/common/exception/GlobalExceptionHandler.java`

**Interfaces:**
- Produces: `Result<T>` with `success`, `fail`, `isSuccess`, `getCode`, `getMessage`, `getData`.
- Produces: `BusinessException(Integer code, String message)`.

- [ ] Write common-core tests first.
- [ ] Run common-core tests and verify they fail because production classes are missing.
- [ ] Implement common-core classes.
- [ ] Run common-core tests and verify they pass.

### Task 2: Feign Shared Configuration

**Files:**
- Create: `common-core/src/test/java/com/volunteer/platform/common/feign/FeignAuthInterceptorTest.java`
- Create: `common-core/src/main/java/com/volunteer/platform/common/feign/FeignConfig.java`
- Create: `common-core/src/main/java/com/volunteer/platform/common/feign/FeignAuthInterceptor.java`

**Interfaces:**
- Produces: `Request.Options` with 3s connect timeout and 10s read timeout.
- Produces: JWT `Authorization` header propagation from the current servlet request.

- [ ] Write Feign interceptor/config tests first.
- [ ] Run tests and verify they fail because production classes are missing.
- [ ] Implement Feign config and interceptor.
- [ ] Run common-core tests and verify they pass.

### Task 3: Gateway Skeleton

**Files:**
- Create: `gateway/pom.xml`
- Create: `gateway/src/test/java/com/volunteer/platform/gateway/filter/JwtAuthGatewayFilterTest.java`
- Create: `gateway/src/main/java/com/volunteer/platform/gateway/GatewayApplication.java`
- Create: `gateway/src/main/java/com/volunteer/platform/gateway/config/CorsConfig.java`
- Create: `gateway/src/main/java/com/volunteer/platform/gateway/config/SecurityProperties.java`
- Create: `gateway/src/main/java/com/volunteer/platform/gateway/filter/JwtAuthGatewayFilter.java`
- Create: `gateway/src/main/resources/application.yml`

**Interfaces:**
- Produces: a Spring Cloud Gateway app named `gateway`.
- Produces: a global filter that allows configured public paths and rejects protected paths without Bearer tokens.

- [ ] Write gateway filter tests first.
- [ ] Run gateway tests and verify they fail because production classes are missing.
- [ ] Implement gateway classes and configuration.
- [ ] Run gateway tests and full Maven test suite.
