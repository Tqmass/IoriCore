---
name: prettier-always
description: Run Prettier on IoriProxy Java/YAML after code edits. Use after any Java or messages/settings change, before build/deploy.
---

# prettier-always

From repo root:

```bat
npx prettier --write "velocity/src/**/*.{java,yml,yaml}" "bungee/src/**/*.{java,yml,yaml}" "common/src/**/*.{java,yml,yaml}"
```

Then restore Darling spacing: blank line between methods; never blank between every statement.
Do not skip this step.
