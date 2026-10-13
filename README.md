# GE Trade Journal

GE Trade Journal is a RuneLite plugin that records Grand Exchange orders and fills in local JSON files.

## Features

- Tracks all eight GE slots, including buy and sell orders, partial fills, completed orders, cancellations, and cleared slots.
- Records observed changes with timestamps, quantities, and GP amounts.
- Keeps a current offer snapshot and an event history for each character.
- Baselines existing offers when recording starts so earlier fills are not reported as new trades.

## Installation

**Plugin Hub release pending.** Once approved, open RuneLite's Plugin Hub, search for **GE Trade Journal**, and install it. **Record GE trades** is enabled by default.

## Saved files

Files are stored in a separate folder for each character:

- **Windows:** `%USERPROFILE%\.runelite\ge-trade-journal\<character>\`
- **macOS/Linux:** `~/.runelite/ge-trade-journal/<character>/`

| File | Contents |
| --- | --- |
| `current_offers.json` | Latest snapshot of all eight GE slots |
| `events.jsonl` | Append-only log of observed offer changes and fills |

All records stay on your computer. See [Privacy](PRIVACY.md).

## Important notes

RuneLite must be running and logged in to observe trades. Activity missed while the client was closed cannot be fully reconstructed. Order IDs are generated locally and may change between sessions. GP values reflect observed GE offers, not net profit after tax.

## Development

Requires Java 21 and Gradle 8.x:

```bash
gradle --no-daemon clean compileJava compileTestJava
bash smoke/run_offline.sh
```

Licensed under [BSD 2-Clause](LICENSE).
