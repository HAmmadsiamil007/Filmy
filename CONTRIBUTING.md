# Contributing

Thanks for taking the time to contribute.

This project has two codebases (`web/`, `android/`), one backend (`api.py`), and a
documentation vault (`docs/`). Everything below exists so changes land **verified**.

---

## 1. Setup

```powershell
# backend
python -m venv .venv
.venv\Scripts\pip.exe install -r requirements.txt

# tests (test-only dependency)
.venv\Scripts\pip.exe install -r requirements-dev.txt

# frontend
cd web; npm install

# run it
.venv\Scripts\uvicorn.exe api:app --host 127.0.0.1 --port 8000
cd web; npx vite --port 5173
```

Full command reference: [`docs/Reference/Commands.md`](<docs/Reference/Commands.md>).

---

## 2. Verify before you claim "done"

| Change | Required check |
|--------|----------------|
| Backend | `.\.venv\Scripts\pytest.exe tests -q` → exit 0 |
| Backend import | `.\.venv\Scripts\python.exe -c "import api; print('IMPORT OK')"` |
| Proxy / `Range` | live `206` + `Content-Range` request — **not covered by unit tests** |
| Frontend | `npx tsc -b` **and** `npm run build` → both exit 0 |
| Docs | `python scripts\check_wikilinks.py` → exit 0 |
| Android | static sweeps now; `.\gradlew.bat assembleDebug` when a toolchain exists |

Report results as observed, never as assumed. If you could not run a check,
say which one and why.

---

## 3. Pull requests

1. Branch from `main`.
2. One logical concern per PR. Split unrelated work.
3. Add or update tests for any behaviour change.
4. Ensure every check above is green (CI runs tests, type-check, build, link check).
5. Update `docs/` **and** `.serena/memories/` if behaviour changed — see
   [`docs/Guides/Verification.md`](<docs/Guides/Verification.md>) § Memory sync.
6. Describe **why**, not just what.

### Commit messages

[Conventional Commits](https://www.conventionalcommits.org/):

```
feat(android): add buffer-driven adaptive quality
fix(backend): stop proxying non-allowlisted hosts (SSRF)
test(backend): cover is_proxyable lookalike rejection
docs: record ADR-005 remote self-hosted backend
chore: untrack the tsc build cache
```

---

## 4. Code standards

The binding list is [`rules.md`](rules.md) §1 (invariants) and §5 (hard-won
lessons). Highlights:

- **Never branch on raw `subject_type`** — use `classify_subject_type()`.
- **All upstream calls go through `fetch_with_backoff()`.**
- **Never widen `PROXY_ALLOWED_HOST_SUFFIXES` casually** — it is the SSRF boundary.
- **Never derive a playback URL from `window.location.origin`** — the base URL is
  runtime-configurable.
- **Keep web and Android contracts in step** (`api.ts` ↔ `Api.kt`).
- **Look up library APIs before using them**; several guessed symbols shipped.

`git add --dry-run` before `git add` on a new directory — see `rules.md` §5.

---

## 5. Frontend style

Tokens are shared between `web/src/styles.css` and
`android/.../ui/theme/Color.kt`. If you add or change a token, change **both** or
record the gap in [`design.md`](design.md) §8.

- Dark by default, one accent (`#E50914`), surfaces for depth rather than shadows.
- Focus is always visible (`2px solid var(--accent-2)`).
- Poster titles clamp to 2 lines.

---

## 6. Backend style

- Keep `api.py` organized as: config → helpers → signing → engine → HTML → routes.
- Prefer small pure helpers — they are what the 129 tests actually cover.
- Metadata TTL 900 s, stream TTL 120 s, suggest TTL 600 s. Do not lengthen the
  stream TTL; signed URLs expire.
- Never cache `None` (`cache_get` uses `None` as its miss sentinel).

---

## 7. Documentation style

- Files in `docs/` are notes: frontmatter `tags/status/updated`, one idea each.
- Link with `[[wikilinks]]`; `scripts/check_wikilinks.py` enforces resolvability.
- `docs/` and `.serena/memories/` are mirrors — update both together.

---

## 8. Reporting bugs

Open an issue including:

- What you did, what you expected, what happened.
- Environment: OS, Python/Node/Gradle versions, which client.
- Whether the backend was local or remote, and the `include_adult` value if relevant.

For security issues, see [`SECURITY.md`](SECURITY.md) — **do not open a public issue.**

---

## 9. Code of conduct

Be respectful, patient, and professional. Assume good intent; argue about the
code, not the person. Harassment or bad-faith participation is not tolerated.
