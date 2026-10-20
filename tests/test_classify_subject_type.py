"""`classify_subject_type()` — the subject-type taxonomy.

Pins the mapping that replaced the original
`"Movie" if subjectType == 1 else "Series"` one-liner, which mislabelled
types 6/7/8 as plain "Series". Clients branch on the returned label to decide
whether to fetch episodes, so a regression here silently breaks Detail screens.

Taxonomy: 1 Movie · 2 Series · 3/6 Anime · 7 ShortDrama · 8 Promo.
"""
import pytest

from api import classify_subject_type


class TestKnownTypes:
    @pytest.mark.parametrize(
        ("raw", "expected"),
        [
            (1, "Movie"),
            (2, "Series"),
            (3, "Anime"),
            (6, "Anime"),
            (7, "ShortDrama"),
            (8, "Promo"),
        ],
    )
    def test_canonical_mapping(self, raw, expected):
        assert classify_subject_type(raw) == expected

    @pytest.mark.parametrize(
        ("raw", "expected"),
        [
            ("1", "Movie"),   # numeric strings arrive from JSON
            ("6", "Anime"),
            ("7", "ShortDrama"),
            (1.0, "Movie"),   # int() accepts floats
            ("8", "Promo"),
        ],
    )
    def test_coercible_inputs(self, raw, expected):
        assert classify_subject_type(raw) == expected

    def test_type_6_is_anime_not_series(self):
        # The specific bug this function exists to fix.
        assert classify_subject_type(6) != "Series"
        assert classify_subject_type(6) == "Anime"

    def test_type_7_is_shortdrama_not_series(self):
        assert classify_subject_type(7) == "ShortDrama"

    def test_type_8_is_promo_not_series(self):
        assert classify_subject_type(8) == "Promo"


class TestFallbacks:
    @pytest.mark.parametrize("raw", ["abc", "", None, [], "2.5", "Movie", object()])
    def test_unparseable_returns_unknown(self, raw):
        assert classify_subject_type(raw) == "Unknown"

    def test_zero_returns_unknown(self):
        # Not in SUBJECT_TYPE_LABELS, and 0 is falsy -> "Unknown".
        assert classify_subject_type(0) == "Unknown"

    def test_unmapped_nonzero_defaults_to_series(self):
        # Documented quirk: an unmapped positive type falls back to "Series"
        # rather than "Unknown". Pinned so nobody "fixes" it silently and
        # breaks clients that rely on non-Movie => fetch episodes.
        assert classify_subject_type(99) == "Series"
        assert classify_subject_type(-1) == "Series"


def test_every_mapped_label_is_nonempty_string():
    from api import SUBJECT_TYPE_LABELS

    for n, label in SUBJECT_TYPE_LABELS.items():
        assert isinstance(n, int)
        assert isinstance(label, str) and label, f"label for {n} is empty"
