---
tags: [reference, taxonomy]
status: active
---

# Subject Types

> [!bug] Historical bug
> The original code did `"Movie" if subjectType == 1 else "Series"` — collapsing types 6/7/8 into "Series". **Always** use `classify_subject_type()`.

## The real taxonomy

| `subject_type` | Label | Confirmed | Notes |
| --- | --- | --- | --- |
| `1` | **Movie** | ✅ | Streams need only `subject_id` |
| `2` | **Series** | ✅ | Needs `se` + `ep` |
| `3` | **Anime** | ✅ | Same bucket as 6 |
| `6` | **Anime** | ✅ | Animation-leaning series |
| `7` | **ShortDrama** | ✅ | Vertical mini-series; often 1 season, short eps |
| `8` | **Promo** | ✅ | Game teasers / tie-ins |

Anything else → `Series` (or `Unknown` if falsy).

```python
SUBJECT_TYPE_LABELS = {1: "Movie", 2: "Series", 3: "Anime",
                       6: "Anime", 7: "ShortDrama", 8: "Promo"}
```

> [!warning] Client impact
> The response `type` field is the **classified label**, not the raw number. Branch on `d.type === "Movie"` to decide whether to fetch episodes — see [[Architecture/Adaptive Quality]] / the web client.

## Content categories observed

Confirmed present in the catalog:

- 🎬 Movies · 📺 TV series (multi-season, multi-language dubs)
- 🎌 Anime / animation (incl. Hindi-dubbed)
- 🎞️ **Short-drama** (`subject_type=7`) — a real vertical, not an edge case
- 🎭 Drama — pervasive across Bollywood, Korean, Nigerian, Pakistani, Russian, Bengali
- 🎥 Documentary · 📻 Reality / Game-Show / Variety
- 🎵 **Music videos, jukeboxes, album mixes, worship, podcasts, gym mixes** — a large low-quality slice
- 🌐 Multi-language dubs — same title exists as separate `subject_id` per language (e.g. `Breaking Bad` vs `Breaking Bad [Hindi]`)
- 🔞 **Adult** — `Adult`, `AiAdult`, `Erotic`, `Erotic, Hot, Romance` — see below

## Adult content

> [!warning] Unfiltered by default upstream
> Genre strings `Adult` / `AiAdult` / `Erotic` return as **first-page** results for generic queries — `porn`, `xxx`, `adult`, `sex`, `hentai`, even `short film` and `documentary`.
>
> Both `st=1` (movies) **and** `st=7` (short-drama series) adult titles exist.
>
> There is **no age gate, no rating metadata, no consent/provenance metadata**. See [[Decisions/ADR-004 Unfiltered adult content by default]] and [[Risks and Compliance]].

Token match (`is_adult()`): `adult`, `erotic`, `porn`, `xxx`, `hentai`, `18+`.

## Related

→ [[Architecture/Backend]] · [[Reference/API Endpoints]] · [[Guides/Debugging Upstream]]
