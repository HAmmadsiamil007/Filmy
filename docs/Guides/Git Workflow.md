---
tags: [guides, git, workflow]
status: active
updated: 2026-10-20
---

# Git Workflow

> [!failure] Both bugs below were caught seconds before committing
> Each would have shipped silently. They exist because someone assumed a rule
> instead of verifying it.

---

## Bug 1 — `.gitignore` has no inline comments

```gitignore
*.tsbuildinfo   # tsc cache        ← WRONG
android/local.properties   # sdk path   ← WRONG
```

Everything after the `#` becomes **part of the pattern**, so neither line
matched anything. The TypeScript build cache got committed; the Android SDK
path would have leaked a machine-specific path to GitHub.

```gitignore
# tsc cache
*.tsbuildinfo

# machine-specific sdk.dir= path — never commit
android/local.properties
```

**Verify with:**

```powershell
git check-ignore -v <path>    # exit 0 = ignored, exit 1 = NOT ignored
```

---

## Bug 2 — unanchored directory patterns match at any depth

```gitignore
moviebox/      ← ALSO matched android/app/src/main/java/com/moviebox/app/
/moviebox/     ← repo root only
```

The stray-vault rule silently swallowed the **entire Android source tree**.

**The symptom** — a dry-run listing 27 files with zero `.kt` files:

```powershell
git add --dry-run android
# no .kt entries → STOP, something is wrong
```

Had the commit run, `android/` would have shipped with **no source code at all**.

---

## Rule: dry-run new directories first

```powershell
git add --dry-run <new-dir>        # inspect
git add <new-dir>                  # only after it looks right
git status --short                  # always, before committing
```

Never `git add -A` blindly.

---

## Rule: gitignore does not affect tracked files

Adding `*.tsbuildinfo` did nothing for the already-committed file. First:

```powershell
git rm --cached web/tsconfig.tsbuildinfo
```

---

## Commit style

Conventional Commits with a scope:

```
feat(android): add buffer-driven adaptive quality
fix(backend): stop proxying non-allowlisted hosts (SSRF)
test(backend): cover is_proxyable lookalike rejection
docs: record ADR-005 remote self-hosted backend
chore: untrack the tsc build cache
```

**One logical concern per commit.** The 2026-10-20 work landed as nine
commits rather than one blob so the history reads as a plan:

| Commit | Scope |
| --- | --- |
| `fix(backend)` | the five verified `api.py` fixes |
| `test(backend)` | 129-test suite |
| `feat(web)` | React client |
| `feat(desktop)` | Tauri shell + icons |
| `feat(android)` | Kotlin client |
| `docs` | vault + memory graph |
| `chore` ×3 | gitignore / build-cache fixes |

---

## What must never be committed

| Path | Why |
| --- | --- |
| `*.tsbuildinfo` | `tsc -b` cache, changes on every type-check |
| `web/dist/`, `node_modules/` | build output |
| `android/**/build/`, `android/.gradle/` | Gradle output |
| `android/local.properties` | machine-specific `sdk.dir=` |
| `*.apk`, `*.aab`, `*.jks`, `*.keystore` | signed artifacts |
| `/moviebox/` | stray local Obsidian vault, ownership unconfirmed |
| `.venv/`, `__pycache__/` | Python |

---

## Current state

- Local `main` is **9 ahead of `origin/main`** — **not pushed** (decision, 2026-10-20).
- `origin` = `github.com/walterwhite-69/Moviebox-API`.

> [!warning] Before pushing
> The repository is already public, but these commits **add** a vault that
> documents piracy and adult-content legal exposure (`docs/Risks and Compliance.md`,
> `mem:legal_risks`). Decide deliberately before making that visible.

---

## Related

- [[Reference/Project Files]] — `.gitignore` structure
- [[Guides/Verification]] — what must be green before committing
- `mem:project/git_hygiene` — agent-readable copy
