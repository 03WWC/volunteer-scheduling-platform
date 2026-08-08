# Dispatch Workbench Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the admin "智能调度" pages usable by selecting an activity and seeing derived shortage/dispatch context instead of manually entering raw IDs.

**Architecture:** Keep the current backend dispatch endpoints. Refactor `DispatchWorkView.vue` into an activity-driven workbench that loads activities, areas, positions, latest schedule detail, and checkin records, then derives per-position risk and shortage numbers on the client.

**Tech Stack:** Vue 3, TypeScript, Element Plus, existing `activityApi`, `areaApi`, `positionApi`, `scheduleApi`, `locationApi`, and `dispatchApi`.

## Global Constraints

- Do not add new frontend dependencies.
- Reuse current backend APIs first.
- Keep manual dispatch available through advanced fields.
- Validate with frontend tests and production build.

---

### Task 1: Activity-Driven Dispatch Page

**Files:**
- Modify: `frontend-admin/src/views/DispatchWorkView.vue`

**Interfaces:**
- Consumes: `activityApi.page`, `areaApi.list`, `positionApi.list`, `scheduleApi.detail`, `locationApi.checkins`, `dispatchApi.execute`, `dispatchApi.detectShortage`, `dispatchApi.result`
- Produces: computed per-position rows with required, assigned, confirmed, checked-in, shortage, and risk level fields

- [ ] **Step 1: Load context by activity**

Fetch activities on mount. When an activity is selected, load areas, positions, schedule detail with silent error, and checkin records.

- [ ] **Step 2: Derive shortage rows**

For each position, calculate:

```ts
requiredCount = position.needCount || 0
assignedCount = assignments.filter(item => item.positionId === position.id).length
confirmedCount = assignments.filter(item => item.positionId === position.id && item.assignmentStatus === 'CONFIRMED').length
checkedInCount = assignments.filter(item => item.positionId === position.id && checkedInAssignmentIds.has(item.id)).length
scheduleShortage = Math.max(requiredCount - assignedCount, 0)
confirmShortage = Math.max(assignedCount - confirmedCount, 0)
checkinShortage = Math.max(assignedCount - checkedInCount, 0)
```

- [ ] **Step 3: Replace raw forms with guided actions**

Show activity selector, summary metrics, per-position table, one-click dispatch action, and collapsible advanced dispatch fields.

- [ ] **Step 4: Keep query and recommendation panels**

Allow task ID lookup and show selected task recommendations.

- [ ] **Step 5: Verify**

Run:

```bash
pnpm test
pnpm build
```
