---
name: security-data-optimizer
description: Security, data handling, and performance analysis for IoriProxy. Use for plugin messages, SQL/Redis, rate-limits, alts/IP, staff alerts, and load hotspots.
---

# security-data-optimizer

## Security
- Mark plugin messages handled before trust.
- Never forward client→backend blindly.
- Validate command args; rate-limit abuse vectors.
- No secrets in logs.

## Perf
- Async SQL/HTTP/Redis; no event-loop block.
- Aggregates over per-tick fan-out.
- Ver `proxy-performance-optimization`.
