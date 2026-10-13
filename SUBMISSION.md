# Plugin Hub submission (not yet submitted)

1. Wait for the public repository's GitHub Actions **Compile against latest RuneLite API** job to pass, and correct any compatibility errors.
2. Test in a RuneLite development client against a real account: begin buy, partial fill, finish, partial sell, cancel, same slot reused, logout/relogin, plugin off/on. Check valid JSON and no duplicated deltas.
3. Review the privacy policy, BSD license attribution, metadata and data lifecycle.
4. Fork `runelite/plugin-hub`, add exactly one manifest file `plugins/ge-trade-journal` with the final immutable tested commit hash and the public repository URL, then open a PR.
5. Await RuneLite maintainer review. **Do not call it published until that PR is merged**.

`build=standard` in `runelite-plugin.properties` is intentional. No third-party dependencies beyond RuneLite are required.
