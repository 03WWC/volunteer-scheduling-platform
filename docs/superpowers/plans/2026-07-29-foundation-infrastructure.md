# Foundation Infrastructure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a tested Spring Cloud Alibaba foundation in which Gateway discovers `user-service` through Nacos and forwards `/api/users/ping`, with cloud-hosted Docker Compose infrastructure.

**Architecture:** A Maven reactor owns a framework-neutral `common-core`, a WebFlux Gateway, and a servlet-based User Service. Nacos provides registration and discovery; MySQL, Redis, Nacos, and RocketMQ run through Docker Compose on the designated cloud development server. AI services are intentionally deferred, while their future service names and boundaries remain reserved by the approved design.

**Tech Stack:** Java 17, Maven 3.6.3, Spring Boot 3.5.0, Spring Cloud 2025.0.0, Spring Cloud Alibaba 2025.0.0.0, Nacos 3.0.3, MySQL 8.4, Redis 7.4, RocketMQ 5.3.1, JUnit 5, AssertJ, MockMvc, Reactor Test, Docker Compose

## Global Constraints

- Use Java 17 from `C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot`.
- Use Maven executable `D:\program\maven-3.6.3\apache-maven-3.6.3\bin\mvn.cmd`.
- Keep Spring Boot at `3.5.0`, Spring Cloud at `2025.0.0`, and Spring Cloud Alibaba at `2025.0.0.0`.
- Never commit `.env`, `环境.txt`, API keys, passwords, public server addresses, logs, or build outputs.
- Correct DeepSeek configuration to base URL `https://api.deepseek.com` and model `deepseek-v4-pro`.
- Do not create `ai-scheduler` in this milestone.
- Run Maven tests locally; run Docker Compose checks and container acceptance on the designated cloud development server.
- Before Task 4, obtain the cloud server connection method and deployment directory from the user; do not assume or invent SSH credentials.
- Every implementation task follows red-green-refactor and ends with a focused commit.

---

### Task 1: Repository Hygiene and Maven Reactor

**Files:**
- Create: `.gitignore`
- Create: `.gitattributes`
- Create: `.env.example`
- Create: `pom.xml`
- Create: `common/common-core/pom.xml`
- Modify: `.env` only to correct the DeepSeek URL and model; keep the file untracked
- Track: `docs/志愿者智能调度平台-产品需求文档(PRD).md`
- Track: `docs/志愿者智能调度平台-技术方案设计文档.md`
- Track: `docs/志愿者智能调度平台-开发任务拆解清单.md`

**Interfaces:**
- Produces: Maven modules `common/common-core`, `gateway`, and `user-service`
- Produces: shared group ID `com.volunteer.platform`
- Produces: environment variable contract consumed by Compose and both applications

- [ ] **Step 1: Prove the build descriptor is absent**

Run:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot'
$projectMaven = 'D:\program\maven-3.6.3\apache-maven-3.6.3\bin\mvn.cmd'
& $projectMaven -q help:effective-pom
```

Expected: FAIL because the root `pom.xml` does not exist.

- [ ] **Step 2: Add repository safety rules**

Create `.gitignore` with:

```gitignore
.env
环境.txt
target/
*.log
.idea/
*.iml
.vscode/
.DS_Store
Thumbs.db
```

Create `.gitattributes` with:

```gitattributes
* text=auto
*.java text eol=lf
*.xml text eol=lf
*.yml text eol=lf
*.yaml text eol=lf
*.md text eol=lf
*.ps1 text eol=crlf
```

Create `.env.example` with non-secret defaults:

```dotenv
NACOS_SERVER_ADDR=127.0.0.1:8848
NACOS_USERNAME=nacos
NACOS_PASSWORD=nacos
NACOS_AUTH_TOKEN=Vm9sdW50ZWVyU2NoZWR1bGluZ1BsYXRmb3JtRGV2VG9rZW4=
NACOS_AUTH_IDENTITY_KEY=volunteer-dev-key
NACOS_AUTH_IDENTITY_VALUE=volunteer-dev-value
DEV_MYSQL_ROOT_PASSWORD=volunteer-dev-root
DEV_REDIS_PASSWORD=volunteer-dev-redis
DEEPSEEK_API_KEY=replace-with-your-api-key
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_MODEL=deepseek-v4-pro
```

Update only these entries in the untracked `.env`:

```dotenv
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_MODEL=deepseek-v4-pro
```

- [ ] **Step 3: Create the Maven reactor**

Create root `pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.0</version>
        <relativePath/>
    </parent>

    <groupId>com.volunteer.platform</groupId>
    <artifactId>volunteer-platform</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <modules>
        <module>common/common-core</module>
        <module>gateway</module>
        <module>user-service</module>
    </modules>

    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2025.0.0</spring-cloud.version>
        <spring-cloud-alibaba.version>2025.0.0.0</spring-cloud-alibaba.version>
        <maven.compiler.release>17</maven.compiler.release>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                <version>${spring-cloud-alibaba.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.14.0</version>
                    <configuration>
                        <release>${maven.compiler.release}</release>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

Create `common/common-core/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.volunteer.platform</groupId>
        <artifactId>volunteer-platform</artifactId>
        <version>0.1.0-SNAPSHOT</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>
    <artifactId>common-core</artifactId>
</project>
```

- [ ] **Step 4: Add temporary child POMs so the reactor can be validated**

Create `gateway/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.volunteer.platform</groupId>
        <artifactId>volunteer-platform</artifactId>
        <version>0.1.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
    <artifactId>gateway</artifactId>
</project>
```

Create `user-service/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.volunteer.platform</groupId>
        <artifactId>volunteer-platform</artifactId>
        <version>0.1.0-SNAPSHOT</version>
        <relativePath>../pom.xml</relativePath>
    </parent>
    <artifactId>user-service</artifactId>
</project>
```

Dependencies are added in Tasks 2 and 3.

- [ ] **Step 5: Verify the effective build and secret exclusions**

Run:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot'
$projectMaven = 'D:\program\maven-3.6.3\apache-maven-3.6.3\bin\mvn.cmd'
& $projectMaven -q help:effective-pom
git check-ignore -v -- .env 环境.txt
git diff --check
```

Expected: Maven exits 0; both secret files are ignored; `git diff --check` reports no errors.

- [ ] **Step 6: Commit the foundation**

```powershell
git add .gitignore .gitattributes .env.example pom.xml common gateway user-service docs/*.md
git commit -m "build: initialize Maven service foundation"
```

Confirm with `git status --short` that `.env` and `环境.txt` are absent from staged and untracked output.

---

### Task 2: User Service Ping Endpoint and Nacos Registration

**Files:**
- Modify: `user-service/pom.xml`
- Create: `user-service/src/main/java/com/volunteer/platform/user/UserServiceApplication.java`
- Create: `user-service/src/main/java/com/volunteer/platform/user/api/PingController.java`
- Create: `user-service/src/main/java/com/volunteer/platform/user/api/PingResponse.java`
- Create: `user-service/src/main/resources/application.yml`
- Create: `user-service/src/test/java/com/volunteer/platform/user/api/PingControllerTest.java`
- Create: `user-service/src/test/resources/application-test.yml`

**Interfaces:**
- Produces: `GET /ping`
- Produces: JSON `{"service":"user-service","status":"UP"}`
- Registers: Nacos service name `user-service`

- [ ] **Step 1: Add the failing web slice test**

Create `PingControllerTest.java`:

```java
package com.volunteer.platform.user.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PingController.class)
class PingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsServiceStatus() throws Exception {
        mockMvc.perform(get("/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("user-service"))
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
```

- [ ] **Step 2: Run the test and confirm RED**

Run:

```powershell
& $projectMaven -pl user-service -Dtest=PingControllerTest test
```

Expected: FAIL because `PingController` does not exist.

- [ ] **Step 3: Add User Service dependencies**

Replace `user-service/pom.xml` with a child POM containing:

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

Also configure `spring-boot-maven-plugin` in the child build.

- [ ] **Step 4: Implement the minimal endpoint**

Create `PingResponse.java`:

```java
package com.volunteer.platform.user.api;

public record PingResponse(String service, String status) {
}
```

Create `PingController.java`:

```java
package com.volunteer.platform.user.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

    @GetMapping("/ping")
    public PingResponse ping() {
        return new PingResponse("user-service", "UP");
    }
}
```

Create `UserServiceApplication.java`:

```java
package com.volunteer.platform.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

- [ ] **Step 5: Configure runtime and isolated tests**

Create `application.yml`:

```yaml
server:
  port: ${SERVER_PORT:8081}

spring:
  application:
    name: user-service
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_ADDR:127.0.0.1:8848}
        username: ${NACOS_USERNAME:nacos}
        password: ${NACOS_PASSWORD:nacos}
        health-indicator:
          enabled: true

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: never
```

Create `application-test.yml`:

```yaml
spring:
  cloud:
    nacos:
      discovery:
        enabled: false
        register-enabled: false
```

- [ ] **Step 6: Run tests and confirm GREEN**

```powershell
& $projectMaven -pl user-service test
```

Expected: PASS with zero failures and zero errors.

- [ ] **Step 7: Commit**

```powershell
git add user-service
git commit -m "feat: add discoverable user service"
```

---

### Task 3: Gateway Route and Prefix Rewriting

**Files:**
- Modify: `gateway/pom.xml`
- Create: `gateway/src/main/java/com/volunteer/platform/gateway/GatewayApplication.java`
- Create: `gateway/src/main/java/com/volunteer/platform/gateway/config/GatewayRoutesConfiguration.java`
- Create: `gateway/src/main/resources/application.yml`
- Create: `gateway/src/test/java/com/volunteer/platform/gateway/config/GatewayRoutesConfigurationTest.java`
- Create: `gateway/src/test/resources/application-test.yml`

**Interfaces:**
- Consumes: Nacos service name `user-service`
- Produces: external route `GET /api/users/ping`
- Rewrites: `/api/users/ping` to `/ping`

- [ ] **Step 1: Add the failing route test**

Create `GatewayRoutesConfigurationTest.java`:

```java
package com.volunteer.platform.gateway.config;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
class GatewayRoutesConfigurationTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void routesUserApiAndStripsPublicPrefix() {
        Route route = routeLocator.getRoutes()
                .filter(candidate -> candidate.getId().equals("user-service"))
                .blockFirst();

        assertThat(route).isNotNull();
        assertThat(route.getUri()).isEqualTo(URI.create("lb://user-service"));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/users/ping").build());
        assertThat(route.getPredicate().apply(exchange)).isTrue();

        AtomicReference<String> forwardedPath = new AtomicReference<>();
        route.getFilters().get(0).filter(exchange, filteredExchange -> {
            ServerHttpRequest request = filteredExchange.getRequest();
            forwardedPath.set(request.getPath().value());
            return Mono.empty();
        }).block();

        assertThat(forwardedPath).hasValue("/ping");
    }
}
```

- [ ] **Step 2: Run the test and confirm RED**

```powershell
& $projectMaven -pl gateway -Dtest=GatewayRoutesConfigurationTest test
```

Expected: FAIL because the Gateway application and route do not exist.

- [ ] **Step 3: Add Gateway dependencies**

Add these dependencies to `gateway/pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway-server-webflux</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-loadbalancer</artifactId>
    </dependency>
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>io.projectreactor</groupId>
        <artifactId>reactor-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

Also configure `spring-boot-maven-plugin`.

- [ ] **Step 4: Implement the Gateway and explicit route**

Create `GatewayApplication.java`:

```java
package com.volunteer.platform.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

Create `GatewayRoutesConfiguration.java`:

```java
package com.volunteer.platform.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfiguration {

    @Bean
    RouteLocator platformRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("user-service", route -> route
                        .path("/api/users/**")
                        .filters(filters -> filters.stripPrefix(2))
                        .uri("lb://user-service"))
                .build();
    }
}
```

- [ ] **Step 5: Add runtime and test configuration**

Create `gateway/src/main/resources/application.yml`:

```yaml
server:
  port: ${SERVER_PORT:8080}

spring:
  application:
    name: gateway
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_ADDR:127.0.0.1:8848}
        username: ${NACOS_USERNAME:nacos}
        password: ${NACOS_PASSWORD:nacos}
        health-indicator:
          enabled: true

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: never
```

Create `gateway/src/test/resources/application-test.yml`:

```yaml
spring:
  cloud:
    nacos:
      discovery:
        enabled: false
        register-enabled: false
```

- [ ] **Step 6: Run module and reactor tests**

```powershell
& $projectMaven -pl gateway test
& $projectMaven verify
```

Expected: both commands exit 0 with zero failures and zero errors.

- [ ] **Step 7: Commit**

```powershell
git add gateway
git commit -m "feat: route user API through gateway"
```

---

### Task 4: Cloud Development Infrastructure

**Files:**
- Create: `deploy/docker-compose.yml`
- Create: `deploy/rocketmq/broker.conf`
- Modify: `.env.example`

**Interfaces:**
- Produces: Nacos on loopback ports 8080, 8848, and 9848
- Produces: MySQL on loopback port 3306
- Produces: Redis on loopback port 6379
- Produces: RocketMQ NameServer on loopback port 9876 and Broker on 10911

- [ ] **Step 1: Confirm Compose configuration is absent**

On the cloud development server, run:

```bash
docker compose -f deploy/docker-compose.yml config
```

Expected: FAIL because `deploy/docker-compose.yml` does not exist.

- [ ] **Step 2: Add RocketMQ broker configuration**

Create `deploy/rocketmq/broker.conf`:

```properties
brokerClusterName=DefaultCluster
brokerName=broker-a
brokerId=0
deleteWhen=04
fileReservedTime=48
brokerRole=ASYNC_MASTER
flushDiskType=ASYNC_FLUSH
namesrvAddr=rocketmq-namesrv:9876
autoCreateTopicEnable=true
brokerIP1=127.0.0.1
```

- [ ] **Step 3: Add Docker Compose services**

Create `deploy/docker-compose.yml` with:

```yaml
name: volunteer-platform

services:
  mysql:
    image: mysql:8.4
    environment:
      MYSQL_ROOT_PASSWORD: ${DEV_MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: volunteer_platform
    ports:
      - "127.0.0.1:3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-p${DEV_MYSQL_ROOT_PASSWORD}"]
      interval: 10s
      timeout: 5s
      retries: 12

  redis:
    image: redis:7.4
    command: ["redis-server", "--requirepass", "${DEV_REDIS_PASSWORD}"]
    ports:
      - "127.0.0.1:6379:6379"
    volumes:
      - redis-data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "${DEV_REDIS_PASSWORD}", "ping"]
      interval: 10s
      timeout: 5s
      retries: 12

  nacos:
    image: nacos/nacos-server:v3.0.3
    environment:
      MODE: standalone
      NACOS_AUTH_ENABLE: "true"
      NACOS_AUTH_TOKEN: ${NACOS_AUTH_TOKEN}
      NACOS_AUTH_IDENTITY_KEY: ${NACOS_AUTH_IDENTITY_KEY}
      NACOS_AUTH_IDENTITY_VALUE: ${NACOS_AUTH_IDENTITY_VALUE}
    ports:
      - "127.0.0.1:8080:8080"
      - "127.0.0.1:8848:8848"
      - "127.0.0.1:9848:9848"
    healthcheck:
      test: ["CMD-SHELL", "curl -fsS http://localhost:8080/nacos/actuator/health | grep -q UP"]
      interval: 15s
      timeout: 10s
      retries: 20

  rocketmq-namesrv:
    image: apache/rocketmq:5.3.1
    command: sh mqnamesrv
    ports:
      - "127.0.0.1:9876:9876"
    volumes:
      - rocketmq-namesrv-logs:/home/rocketmq/logs
    healthcheck:
      test: ["CMD-SHELL", "sh mqadmin getNamesrvConfig -n localhost:9876 >/dev/null 2>&1"]
      interval: 10s
      timeout: 5s
      retries: 12

  rocketmq-broker:
    image: apache/rocketmq:5.3.1
    command: sh mqbroker -c /home/rocketmq/conf/broker.conf
    depends_on:
      rocketmq-namesrv:
        condition: service_healthy
    ports:
      - "127.0.0.1:10911:10911"
    volumes:
      - ./rocketmq/broker.conf:/home/rocketmq/conf/broker.conf:ro
      - rocketmq-broker-logs:/home/rocketmq/logs
      - rocketmq-broker-store:/home/rocketmq/store

volumes:
  mysql-data:
  redis-data:
  rocketmq-namesrv-logs:
  rocketmq-broker-logs:
  rocketmq-broker-store:
```

- [ ] **Step 4: Validate and start on the cloud server**

Create a server-local `.env` from `.env.example`, replace only the development passwords and Nacos authentication values, and keep it outside Git.

Run:

```bash
docker compose -f deploy/docker-compose.yml config
docker compose -f deploy/docker-compose.yml up -d
docker compose -f deploy/docker-compose.yml ps
```

Expected: config exits 0; MySQL, Redis, Nacos, and RocketMQ NameServer reach `healthy`; Broker remains running.

- [ ] **Step 5: Verify the infrastructure endpoints**

```bash
curl -fsS http://127.0.0.1:8080/nacos/actuator/health
docker compose -f deploy/docker-compose.yml exec -T mysql mysqladmin ping -h localhost -p"$DEV_MYSQL_ROOT_PASSWORD"
docker compose -f deploy/docker-compose.yml exec -T redis redis-cli -a "$DEV_REDIS_PASSWORD" ping
docker compose -f deploy/docker-compose.yml exec -T rocketmq-namesrv sh mqadmin clusterList -n localhost:9876
```

Expected: Nacos reports `UP`, MySQL reports alive, Redis returns `PONG`, and RocketMQ lists `DefaultCluster`.

- [ ] **Step 6: Commit**

```bash
git add deploy .env.example
git commit -m "infra: add cloud development services"
```

---

### Task 5: Operational Documentation and End-to-End Acceptance

**Files:**
- Create: `README.md`
- Create: `scripts/smoke-test.ps1`

**Interfaces:**
- Consumes: Gateway `http://localhost:8080`
- Produces: repeatable build, startup, and smoke-test commands

- [ ] **Step 1: Add a failing smoke-test invocation**

Run:

```powershell
& .\scripts\smoke-test.ps1
```

Expected: FAIL because the script does not exist.

- [ ] **Step 2: Create the smoke test**

Create `scripts/smoke-test.ps1`:

```powershell
param(
    [string]$GatewayBaseUrl = 'http://127.0.0.1:8080'
)

$ErrorActionPreference = 'Stop'
$gatewayHealth = Invoke-RestMethod "$GatewayBaseUrl/actuator/health"
if ($gatewayHealth.status -ne 'UP') {
    throw "Gateway health is $($gatewayHealth.status)"
}

$ping = Invoke-RestMethod "$GatewayBaseUrl/api/users/ping"
if ($ping.service -ne 'user-service' -or $ping.status -ne 'UP') {
    throw "Unexpected ping response"
}

Write-Output 'SMOKE_TEST=PASS'
```

- [ ] **Step 3: Write the operating guide**

Create `README.md` with exact sections for:

- prerequisites and the confirmed Java/Maven paths;
- environment setup using `.env.example`;
- local Maven build command;
- cloud Docker Compose deployment commands;
- User Service and Gateway startup commands;
- Nacos registration verification;
- smoke-test invocation;
- safe shutdown using `docker compose down`;
- explicit warning that `docker compose down -v` deletes development data;
- AI roadmap linking to the approved design document.

Use these application startup commands:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot'
$projectMaven = 'D:\program\maven-3.6.3\apache-maven-3.6.3\bin\mvn.cmd'
& $projectMaven verify
& $projectMaven -pl user-service spring-boot:run
& $projectMaven -pl gateway spring-boot:run
```

When the apps run on the cloud server, use the packaged JARs and the server-local Nacos address:

```bash
NACOS_SERVER_ADDR=127.0.0.1:8848 java -jar user-service/target/user-service-0.1.0-SNAPSHOT.jar
NACOS_SERVER_ADDR=127.0.0.1:8848 java -jar gateway/target/gateway-0.1.0-SNAPSHOT.jar
```

- [ ] **Step 4: Run full verification**

Local:

```powershell
& $projectMaven verify
git check-ignore -v -- .env 环境.txt
git diff --check
```

Cloud server:

```bash
docker compose -f deploy/docker-compose.yml config
docker compose -f deploy/docker-compose.yml ps
curl -fsS http://127.0.0.1:8080/api/users/ping
```

Expected: Maven has zero failures/errors; secrets are ignored; Compose is valid; required containers are healthy/running; Gateway returns `{"service":"user-service","status":"UP"}`.

- [ ] **Step 5: Commit**

```powershell
git add README.md scripts
git commit -m "docs: add foundation operating guide"
```

- [ ] **Step 6: Final repository audit**

```powershell
git status --short
git log --oneline --decorate -8
git ls-files .env 环境.txt
```

Expected: working tree contains no unintended changes; the log contains focused task commits; the final command prints nothing.
