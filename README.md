# GE Trade Journal

A **standalone, local-only** RuneLite Grand Exchange transaction journal. No Discord integration, Flip Familiar functionality, network access, HTTP requests, API keys, trading signals, bank inspection, or automated game actions.

**Status: development candidate, NOT Plugin Hub approved.** The public GitHub repository name may refer to an earlier companion project; this source implements only neutral GE tracking.

## What it records

- Eight GE slot snapshots (`current_offers.json`), including item, side, offer price, quantity, cumulative fill and cumulative GP.
- Observed GE lifecycle events (`events.jsonl`): initial observation, order start, observed buy/sell fill **deltas**, completion, cancellation, replaced/changed order, cleared slot, and ambiguous states requiring review.
- Separate folders for each OSRS character: `~/.runelite/ge-trade-journal/<character>/`.
- Locally generated session, event and observed-order identifiers to help downstream tools reconcile repeated records.

**No bot-specific data or connection required.** Other tools may independently import these local files if the user chooses.

## Limits to understand

- RuneLite must be running and logged in to observe activity. Trades when the client was closed cannot be reconstructed.
- The first snapshot is a **baseline**, not proof of new fills. A completed order seen for the first time is flagged as uncertain rather than generating invented profit.
- `cumulative_gp` and `delta_gp` are raw in-game offer values, not verified realized net profit after GE tax.
- Offer IDs are **not** stable across client restarts. Some extremely rapid create/replace/complete cycles can occur between observations. The plugin will not fabricate missed events.
- A plugin recording toggle resets its in-memory baseline on re-enable, avoiding double-counting unobserved fills.
- Files may contain in-game character names. Do not publish the JSON exports.

## Build & tests

Use Java 21 and Gradle 8.x. The plugin targets Java 11 bytecode and `latest.release` RuneLite.

```bash
gradle --no-daemon clean compileJava compileTestJava
bash smoke/run_offline.sh
```

The GitHub Actions workflow runs these checks on push. The offline smoke tests use the dependency-free journal classes and also verify that the resulting lines parse as valid JSON.

## Installation

Not available from the Plugin Hub yet. Once reviewed and approved, install the **GE Trade Journal** plugin from RuneLite's Plugin Hub. The setting `Record GE trades` is on by default. You do not enter any server URL, Discord ID, or password.

See [PRIVACY.md](PRIVACY.md), [RELEASE_READINESS.md](RELEASE_READINESS.md), and [SUBMISSION.md](SUBMISSION.md).

## License

BSD 2-Clause. Copyright notices for StefanMikael's OSRS Trade Tracker and independent GE Trade Journal contributions are retained in [LICENSE](LICENSE).
