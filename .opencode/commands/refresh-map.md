---
description: Regenerate docs/platform/MAP.md from the current codebase
---

Regenerate `docs/platform/MAP.md` so it reflects the current codebase.

Steps:
1. Inspect all `codelab-*` modules, services, controllers, events, jobs, tasks, entities, and topic
   subscriptions.
2. Update module tables, public APIs, messaging topics/events, headless tasks, persistence tables,
   and extension points accordingly.
3. Keep the Markdown format, sections, and refresh footer intact.
4. Write the result to `docs/platform/MAP.md`.

Do not change application code or build files; only regenerate the map.
