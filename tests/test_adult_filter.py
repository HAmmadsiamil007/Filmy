"""`is_adult()` / `strip_adult()` — the adult-content filter.

Server-side filtering lives in ONE place so all four clients share it
(ADR-004: the API default is `include_adult=false`, clients opt IN by passing
true). A regression here silently leaks filtered titles into default results —
or hides titles the product decided to show.

Matching is substring-based on a lowered genre string against
ADULT_GENRE_TOKENS.
"""
import pytest

from api import ADULT_GENRE_TOKENS, is_adult, strip_adult


class TestIsAdult:
    @pytest.mark.parametrize("token", ADULT_GENRE_TOKENS)
    def test_each_token_detected(self, token):
        assert is_adult(f"Drama, {token}") is True

    @pytest.mark.parametrize(
        "genre",
        [
            "Pornography",
            "Adult Video",
            "Erotic Thriller",
            "Hentai",
            "XXX",
            "18+ Only",
            "ADULT",             # case-insensitive
            "HORROR, PORN",      # token inside a longer list
        ],
    )
    def test_realistic_adult_genres(self, genre):
        assert is_adult(genre) is True

    @pytest.mark.parametrize(
        "genre",
        [
            "Drama",
            "Action",
            "Documentary",   # must not trip on incidental substrings
            "Comedy, Romance",
            "Sci-Fi & Fantasy",
        ],
    )
    def test_ordinary_genres_pass_through(self, genre):
        assert is_adult(genre) is False

    @pytest.mark.parametrize("genre", [None, "", "   ", 0, [], {}, 1.5])
    def test_missing_or_non_string_is_not_adult(self, genre):
        # A missing genre must not throw and must not be flagged.
        assert is_adult(genre) is False

    def test_none_genre(self):
        assert is_adult(None) is False


class TestStripAdult:
    def test_drops_items_marked_adult(self):
        items = [
            {"title": "A", "is_adult": False},
            {"title": "B", "is_adult": True},
            {"title": "C", "is_adult": True},
            {"title": "D", "is_adult": False},
        ]
        kept = strip_adult(items)
        assert [it["title"] for it in kept] == ["A", "D"]

    def test_items_without_the_flag_are_kept(self):
        # `it.get("is_adult")` is None -> falsy -> kept. Backward compatible
        # with older payload shapes that never set the key.
        items = [{"title": "A"}, {"title": "B", "is_adult": False}]
        assert len(strip_adult(items)) == 2

    def test_falsy_variants_are_kept(self):
        items = [
            {"title": "a", "is_adult": None},
            {"title": "b", "is_adult": 0},
            {"title": "c", "is_adult": ""},
            {"title": "d", "is_adult": False},
        ]
        assert len(strip_adult(items)) == 4

    def test_truthy_variants_are_dropped(self):
        items = [
            {"title": "a", "is_adult": 1},
            {"title": "b", "is_adult": "yes"},
        ]
        assert strip_adult(items) == []

    def test_empty_input(self):
        assert strip_adult([]) == []

    def test_does_not_mutate_input(self):
        items = [{"title": "A", "is_adult": False}, {"title": "B", "is_adult": True}]
        snapshot = [dict(i) for i in items]
        strip_adult(items)
        assert items == snapshot


def test_tokens_are_lowercased():
    # is_adult lowercases the genre, so tokens must themselves be lowercase
    # or they would never match.
    assert all(tok == tok.lower() for tok in ADULT_GENRE_TOKENS)
