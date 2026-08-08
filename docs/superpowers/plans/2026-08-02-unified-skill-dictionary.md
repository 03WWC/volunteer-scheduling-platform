# Unified Skill Dictionary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace free-text skill entry with a Chinese fixed-choice dictionary and make schedule matching tolerant of whitespace and letter case.

**Architecture:** Each frontend keeps a small typed skill dictionary suited to its own build system, while API payloads retain the existing English-code contract. The schedule service normalizes both sides at comparison time so existing manually entered data remains usable.

**Tech Stack:** Vue 3, TypeScript, Element Plus, uni-app, Vitest, Java 17, Spring Boot, JUnit 5, AssertJ

## Global Constraints

- Do not add a database table or change existing API field names.
- Store canonical uppercase codes; display Chinese names.
- `NONE` is available for positions only.
- Preserve unknown historical values and all existing no-skill aliases.

---

### Task 1: Typed frontend skill dictionaries

**Files:**
- Create: `frontend-admin/src/constants/skills.ts`
- Create: `frontend-admin/src/constants/skills.test.ts`
- Create: `miniapp/src/constants/skills.ts`
- Create: `miniapp/src/constants/skills.test.ts`

**Interfaces:**
- Produces: `POSITION_SKILL_OPTIONS`, `VOLUNTEER_SKILL_OPTIONS`, `SKILL_LEVEL_OPTIONS`, `normalizeSkillCode(value)`, `skillNameOf(value)`, and `skillLevelNameOf(value)`.

- [ ] **Step 1: Write failing dictionary tests**

```ts
expect(normalizeSkillCode(' guide ')).toBe('GUIDE')
expect(skillNameOf('GUIDE')).toBe('秩序引导')
expect(POSITION_SKILL_OPTIONS[0].code).toBe('NONE')
expect(VOLUNTEER_SKILL_OPTIONS.some((item) => item.code === 'NONE')).toBe(false)
```

- [ ] **Step 2: Run tests and verify missing-module failures**

Run: `npm test -- src/constants/skills.test.ts` in each frontend.

- [ ] **Step 3: Implement the typed dictionaries and lookup helpers**

Use the exact codes and names from the design document. Normalize null/blank to an empty string, no-skill aliases to `NONE`, and every other value with `trim().toUpperCase()`.

- [ ] **Step 4: Re-run both dictionary tests**

Expected: both Vitest commands exit 0.

### Task 2: Replace free-text skill entry in the admin and miniapp

**Files:**
- Modify: `frontend-admin/src/views/AreaPositionView.vue`
- Modify: `frontend-admin/src/views/VolunteerManageView.vue`
- Modify: `miniapp/src/pages/mine/skills.vue`

**Interfaces:**
- Consumes: the constants and lookup helpers from Task 1.
- Produces: forms that submit canonical `skillRequirement`, `skillCode`, `skillName`, and `skillLevel` values through the existing APIs.

- [ ] **Step 1: Change the admin position field to a filterable skill select**

Default new positions to `NONE`, show Chinese names in the table, and include both code and Chinese name in keyword filtering.

- [ ] **Step 2: Change the admin volunteer skill fields to one optional skill select**

Derive `skillName` from the selected code and use coded level choices with Chinese labels.

- [ ] **Step 3: Change the miniapp skill form to two native pickers**

Use volunteer-only skills for the first picker, coded levels for the second picker, submit the selected option, and show Chinese names and levels in the list.

- [ ] **Step 4: Build both frontends**

Run: `npm run build` in `frontend-admin` and `npm run build:mp-weixin` in `miniapp`.

Expected: both commands exit 0 with no TypeScript or template compilation errors.

### Task 3: Normalize schedule skill matching

**Files:**
- Modify: `schedule-service/schedule-service-server/src/test/java/com/volunteer/platform/schedule/service/ScheduleServiceImplTest.java`
- Modify: `schedule-service/schedule-service-server/src/main/java/com/volunteer/platform/schedule/service/impl/ScheduleServiceImpl.java`

**Interfaces:**
- Consumes: existing `PositionDTO.skillRequirement` and `UserSkillDTO.skillCode` values.
- Produces: schedule eligibility matching that ignores surrounding whitespace and ASCII letter case.

- [ ] **Step 1: Write a failing auto-schedule regression test**

Create a position with `" guide "`, a signed-up volunteer with `"GUIDE"`, and assert that auto-generation assigns that volunteer.

- [ ] **Step 2: Run the targeted test and verify it fails with insufficient matching volunteers**

Run: `mvn -pl schedule-service/schedule-service-server -am -Dtest=ScheduleServiceImplTest#autoGenerateMatchesSkillCodeIgnoringWhitespaceAndCase -Dsurefire.failIfNoSpecifiedTests=false test`.

- [ ] **Step 3: Implement normalized comparison**

Trim the required code once and compare it to each trimmed user code with `equalsIgnoreCase`.

- [ ] **Step 4: Run the schedule service test suite**

Run: `mvn -pl schedule-service/schedule-service-server -am -DskipITs test`.

Expected: Maven exits 0 with zero test failures.

### Task 4: Final cross-project verification

**Files:**
- Verify all files listed above.

**Interfaces:**
- Consumes: completed Tasks 1-3.
- Produces: build artifacts for the admin frontend, miniapp, and schedule service.

- [ ] **Step 1: Run both frontend test suites**

Run: `npm test` in `frontend-admin`, then `npm test` in `miniapp`.

- [ ] **Step 2: Run both production builds**

Run: `npm run build` in `frontend-admin`, then `npm run build:mp-weixin` in `miniapp`.

- [ ] **Step 3: Run the complete schedule-service Maven tests and package build**

Run: `mvn -pl schedule-service/schedule-service-server -am test` followed by `mvn -pl schedule-service/schedule-service-server -am -DskipTests package`.

- [ ] **Step 4: Review changed files against the three approved requirements**

Confirm every data-entry path uses a fixed choice, payloads use canonical codes, and matching compatibility is covered by a regression test.
