# Git hygiene

LESSONS FROM REAL FAILURES (2026-10-20). Both were caught seconds before
committing, by VERIFYING instead of assuming.

## 1. .gitignore has NO inline comments
```gitignore
*.tsbuildinfo   # tsc cache      <- WRONG. The whole tail becomes part of
*.tsbuildinfo                     the PATTERN, which then matches nothing.
android/local.properties   # sdk path   <- same bug, would have leaked a
                                          machine-specific path to git.
```
Fix: comments go on their OWN line above the pattern.
Verified with: git check-ignore -v <path>   (0 = ignored, 1 = not ignored)

## 2. Unanchored directory patterns match at ANY depth
```gitignore
/moviebox/     <- repo root only (correct - stray Obsidian vault)
moviebox/      <- ALSO matched android/app/src/main/java/com/moviebox/app/
                  and silently swallowed the ENTIRE Android source tree.
```
SYMPTOM: git add --dry-run android listed 27 files with ZERO .kt files.
Had I committed, android/ would have shipped with no source at all.
RULE: always git add --dry-run FIRST on a new directory.

## 3. gitignore does not affect TRACKED files
web/tsconfig.tsbuildinfo had been committed; adding *.tsbuildinfo did nothing
until: git rm --cached web/tsconfig.tsbuildinfo

## Commit practice (this repo)
- Conventional Commits: feat/fix/test/docs/chore + scope.
- 9 logical commits for the 2026-10-20 work, not one blob.
- dry-run new dirs; git status --short before git add.
- Never git add -A blindly.
- Build artifacts never committed: *.tsbuildinfo, web/dist, node_modules,
  android/**/build/, android/local.properties, moviebox/.

## State
- local main is 9 ahead of origin/main. NOT PUSHED - user decision 2026-10-20.
- origin = github.com/walterwhite-69/Moviebox-API (likely user's own repo).
- Push consideration: repo already public, but these commits ADD a vault
  documenting piracy/adult-content legal risk. Ask before pushing.