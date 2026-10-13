# Windows live validation — GE Trade Journal

**Status:** A successful GitHub Actions build is not a live GE test. This test is required before requesting Plugin Hub publication.

This project is standalone and local-only. It does not need Flip Familiar, Discord, API keys, or an HTTP connection.

## 1. Run the development client once

Prerequisites: Windows, **JDK 21**, **Gradle 8.x** on PATH, and the source repository downloaded/extracted or cloned. IntelliJ is optional, not required.

From this folder, double-click **`Run_Windows_Live_Test.bat`**. It checks that Java, `javac`, and Gradle are available, then launches the RuneLite development client with our plugin. Alternatively use a Command Prompt here and run:

```bat
gradle --no-daemon --console=plain run
```

**Jagex Account login:** Development clients do not launch via the ordinary Jagex Launcher. If your character uses a Jagex Account, follow RuneLite's official [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) development guide. It temporarily writes launcher credentials to `%USERPROFILE%\.runelite\credentials.properties`. Treat this file like a login secret: **do not upload/share it**, undo the launcher `--insecure-write-credentials` flag after the test, and delete the credentials file when finished. If you don't want to create that file, do not perform the development-client live test; wait for a reviewed Plugin Hub release.

## 2. Verify local output

Log in, wait about 10 game ticks, then press **Win+R** and open:

```text
%USERPROFILE%\.runelite\ge-trade-journal
```

Open the folder matching your character. It should contain `current_offers.json`. A new `events.jsonl` is created after an observed nonempty offer or event. **Existing GE offers are baselined:** they must not appear as fresh fills.

## 3. Test low-value trades (all actions are manual)

1. Create a small **BUY** offer. Confirm one `OFFER_STARTED` event, then at least one `BUY_FILL` if it fills. Compare `delta_quantity` and `delta_gp` to the offer's changes. If it finishes, expect one `OFFER_COMPLETED`.
2. Create a small **SELL** offer. Check `SELL_FILL` and its actual GP delta; the plugin does **not** calculate GE tax or profitability.
3. Create a low-value order, let part fill, then **cancel** it. Expect one `OFFER_CANCELLED`. An unfilled cancellation must not produce a fill.
4. Reuse the same GE slot, including for the **same item and price**. Expect a new `order_id` for the new order.
5. Logout/login and toggle recording OFF/ON. Initial state must be a baseline; no historical fills may be replayed.

Check **`current_offers.json`** reflects all eight GE slots, including unused slots.

## 4. Validate files

From PowerShell in the character's local folder:

```powershell
Get-Content .\current_offers.json -Raw | ConvertFrom-Json | Out-Null
Get-Content .\events.jsonl | Where-Object { $_.Trim() } | ForEach-Object { $_ | ConvertFrom-Json | Out-Null }
```

Both commands should finish without errors. If there are problems, share the **GitHub Actions/development-client error text** and a redacted example (remove character names and any sensitive details), not any credentials file.

## 5. Test result

Record PASS/FAIL for: initial baseline, BUY fill, SELL fill, partial cancel, new slot order identity, logout/relogin, toggle OFF/ON, valid JSON and all eight slots. Report any discrepancies before submitting the Plugin Hub PR.

Until actual trades are tested, **do not label this plugin production-ready**. No game actions are automated.
