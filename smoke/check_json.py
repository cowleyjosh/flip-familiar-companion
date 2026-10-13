"""Validate that Java emitted parseable events and snapshots in all smoke folders."""
import glob
import json
import os
import tempfile

paths = glob.glob(os.path.join(os.environ["GE_JOURNAL_TEST_ROOT"], "ge-regression-*", "events.jsonl"))
assert paths, "No Java regression outputs found"
count = 0
for path in paths:
    with open(path, encoding="utf-8") as f:
        for line in f:
            event = json.loads(line)
            assert event["schema_version"] == 1
            assert event["account"] == 'Account"A'
            assert 0 <= event["offer"]["slot"] < 8
            assert event["delta_quantity"] >= 0
            assert event["delta_gp"] >= 0
            count += 1
    with open(os.path.join(os.path.dirname(path), "current_offers.json"), encoding="utf-8") as f:
        snapshot = json.load(f)
    assert len(snapshot["offers"]) == 8
    assert [offer["slot"] for offer in snapshot["offers"]] == list(range(8))
print(f"GE Trade Journal JSON validation PASSED ({len(paths)} datasets, {count} events)")
