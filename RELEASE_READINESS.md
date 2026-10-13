# Phase 1–2 verification / publication gate

- Local Java 11-compatible core compilation and regression smoke: PASS (26 assertions).
- Python JSON validation: PASS (seven independent datasets; 23 records).
- Local Java plugin compilation **against RuneLite SDK**: NOT YET VERIFIED because Maven/Gradle hosts are inaccessible in the audit environment. GitHub Actions does the full check on every push.
- Live RuneLite client validation: NOT YET VERIFIED. Do not submit the Plugin Hub manifest until a real-client buy/sell/cancel/restart test has passed.
- Plugin is standalone. No HTTP client, URL, Discord integration, bot configuration or GE automation.
- Initial open offers are baselined; prior fills and first-seen terminal offers never become fabricated fill events.
- On disabling/re-enabling recording, the next session baselines current offers to avoid counting unattended fills.
- The `order_id` is generated locally and changes after a restart or offer replacement; it is not an official game order ID.
- Non-atomicity limitation: a filesystem write failure between append and snapshot may leave a partial/incomplete import state. Importing software must deduplicate by event_id and treat gaps conservatively.
