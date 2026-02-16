# WebDashboard

Ktor + React (Vite + shadcn/ui) dashboard for editing bot configuration.

## Auto start with bot

`Core` now depends on `WebDashboard`, so starting the bot also starts dashboard in the same process.
Default bind:
- Host: `127.0.0.1`
- Port: `21100`

Optional env:
- `XSBOT_DASHBOARD_ENABLED=false` to disable dashboard startup
- `XSBOT_DASHBOARD_HOST=<host>`
- `XSBOT_DASHBOARD_PORT=<port>`

## Runtime behavior

- Plugin switch toggles (`enabled`) are applied immediately and trigger plugin reload.
- Core settings are multi-field edits and require clicking Save.
- Plugin YAML editor writes directly to `plugins/<PluginName>/config.yaml` and reloads that plugin.
- Writes use rollback-on-failure to prevent partial apply.

## Local frontend (manual npm)

```bash
cd Web-Dashboard/frontend
npm install
npm run dev
```

Vite dev server proxies `/api` to `http://127.0.0.1:21100` by default.
Override target with:

```bash
VITE_DASHBOARD_API_TARGET=http://127.0.0.1:21100 npm run dev
```

## Build frontend manually, then use Gradle

```bash
cd Web-Dashboard/frontend
npm install
npm run build
cd ../..
./gradlew :WebDashboard:build -Pfrontend.skipNpm=true
```

`-Pfrontend.skipNpm=true` tells Gradle to skip npm execution and use existing `frontend/dist`.

## Fully automatic build (Gradle runs npm)

```bash
./gradlew :WebDashboard:build
```

If Gradle cannot find npm from your IDE/daemon environment, set npm explicitly:

```bash
./gradlew :WebDashboard:build -Pfrontend.npmExecutable=/opt/homebrew/bin/npm
```

Common macOS paths:
- `/opt/homebrew/bin/npm` (Apple Silicon Homebrew)
- `/usr/local/bin/npm` (Intel Homebrew)
- `$HOME/.nvm/versions/node/<version>/bin/npm` (nvm)

## Run dashboard server

```bash
XSBOT_DASHBOARD_HOST=127.0.0.1 XSBOT_DASHBOARD_PORT=21100 \
./gradlew :WebDashboard:run -Pfrontend.skipNpm=true
```
