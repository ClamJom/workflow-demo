# Repository Guidelines

## Project Structure & Module Organization

This repository contains a Spring Boot 3 workflow backend and a Vue 3/Vite frontend.

- `src/main/java/com/example/demoworkflow/` contains backend code: `control/`, `services/`, `repository/`, `mapper/`, `vo/`, and workflow runtime logic in `utils/workflow/`.
- `src/main/resources/application.yaml` holds backend configuration such as database and Redis settings.
- `src/test/java/` contains Spring Boot tests.
- `web/` contains the frontend app: `web/src/`, `web/components/`, `web/api/`, and `web/public/`.
- `example/quick_sort.json` is a sample workflow; `imgs/` stores README screenshots.

## Build, Test, and Development Commands

- `./mvnw test` runs backend tests.
- `./mvnw package` builds the Spring Boot application.
- `./mvnw spring-boot:run` starts the backend locally.
- `cd web && npm install` installs frontend dependencies.
- `cd web && npm run dev` starts the Vite dev server.
- `cd web && npm run build` creates the production frontend bundle.

On Windows PowerShell, use `.\mvnw.cmd` instead of `./mvnw`.

## Coding Style & Naming Conventions

Use Java 21 and follow the existing package layout. Java classes use `PascalCase`; methods, fields, and local variables use `camelCase`. Keep workflow node implementations under `utils/workflow/nodes/`, and name new nodes with a `Node` suffix, such as `ListAddNode`.

Frontend files use Vue single-file components with `PascalCase` component names, for example `WorkflowPreview.vue`. Keep workflow type definitions under `web/types/workflow/` and shared helpers under `web/utils/`.

## Testing Guidelines

Backend tests use Spring Boot Test and live under `src/test/java/`. Name test classes with a `Tests` suffix and keep package names aligned with the code under test. Run `./mvnw test` before backend changes. The frontend has no configured test runner, so validate UI changes with `npm run build` and local Vite testing.

## Commit & Pull Request Guidelines

Recent commits use concise Conventional Commit style, such as `feat:comment node`, `fix:abnormal exit`, and `doc:update readme, quick sort example provided`. Continue using prefixes like `feat:`, `fix:`, `doc:`, and `refactor:`.

Pull requests should describe the change, list backend/frontend impact, mention required configuration changes, and include screenshots for workflow UI changes. Link related issues when available and note verification commands.

## Security & Configuration Tips

Do not commit local database credentials, Redis secrets, or generated build output. Update `src/main/resources/application.yaml` for local services, and keep environment-specific values out of shared commits.
