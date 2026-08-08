# Defense Preparation Guides Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Generate eight role-specific defense preparation DOCX guides and one shared emergency guide from the approved assignment summary and current project source code.

**Architecture:** Extract assignment, layout, and source-code evidence into a task-local content model, then generate all guides with one deterministic DOCX builder. Render every final document to PNG and inspect all pages before delivery.

**Tech Stack:** Bundled Python, python-docx, OOXML helpers, LibreOffice document renderer.

## Global Constraints

- Use `答辩分工报告/小组分工总览.docx` as the assignment authority.
- Mention only modules, pages, classes, APIs, and technologies present in the current repository.
- Each person owns one focused end-to-end explanation and uses honest participation wording.
- Deliver eight personal guides and one shared emergency guide under `答辩分工报告/答辩准备手册`.
- Render and visually inspect every page of every final DOCX.

---

### Task 1: Reference and source evidence

**Files:**
- Read: `答辩分工报告/小组分工总览.docx`
- Read: `docs/志愿者智能调度平台-技术方案设计文档.md`
- Read: backend controllers, services, DAOs, frontend views, miniapp pages, and SQL schema files.
- Create: `.codex-run/defense-guides/artifact.md`
- Create: `.codex-run/defense-guides/source-map.json`

- [ ] Render and audit the assignment reference without changing it.
- [ ] Extract each member's role and assignment.
- [ ] Map each assignment to existing pages, APIs, classes, tables, and a demonstrable call chain.
- [ ] Record layout evidence and source mappings in the task-local files.

### Task 2: Deterministic guide builder

**Files:**
- Create: `.codex-run/defense-guides/build_guides.py`
- Create: `答辩分工报告/答辩准备手册/*.docx`

**Interfaces:**
- Consumes: member assignments and `source-map.json`.
- Produces: nine DOCX files with consistent styles and role-specific content.

- [ ] Define exact A4 page geometry, Chinese typography, heading styles, lists, callouts, tables, header, and footer.
- [ ] Write each personal script, demo route, code walkthrough, 10-15 questions, emergency wording, and checklist.
- [ ] Write the shared system introduction, team demo sequence, public questions, and transfer rules.
- [ ] Generate all nine DOCX files and structurally validate required sections and member names.

### Task 3: Render and visual quality assurance

**Files:**
- Create: `.codex-run/defense-guides/rendered/<document>/page-*.png`
- Modify: `.codex-run/defense-guides/build_guides.py` only if QA finds a layout issue.

- [ ] Render all nine DOCX files with the packaged renderer.
- [ ] Inspect every page for clipping, overlap, broken tables, font issues, and abnormal pagination.
- [ ] Correct any detected defect and rerender affected documents.
- [ ] Verify file count, required content, document readability, and unchanged reference hash.
