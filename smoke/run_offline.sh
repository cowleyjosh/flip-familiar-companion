#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p build/smoke
export GE_JOURNAL_TEST_ROOT="$(mktemp -d)"
trap 'rm -rf "$GE_JOURNAL_TEST_ROOT"' EXIT
javac --release 11 -d build/smoke \
  src/main/java/com/getradejournal/{JsonText,OfferState,JournalStorage,TradeJournal}.java \
  smoke/TestJournalStorage.java \
  smoke/JournalSmoke.java smoke/JournalRegression.java
java -cp build/smoke com.getradejournal.JournalSmoke
java -cp build/smoke com.getradejournal.JournalRegression
python3 smoke/check_json.py
