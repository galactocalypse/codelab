# Frontend Modular Monolith (Microfrontend-Style) Architecture

This document defines the standard frontend counterpart of the backend modular monolith.
The backend composes `CodelabModule`s into the `codelab-megalith` container by importing
them as Maven libraries and discovering their SPI. The frontend uses the same idea with
npm packages: business modules are built and published independently, and a single
deployable container app composes them at build time under conventional URL roots.

This is **build-time composition**. Modules are not independently deployed in production;
the container is the only deployable artifact. Modules are independently *developed*,
*tested*, and *versioned*.

## Goals and non-goals

### Goals

- Standalone frontend packages for `codelab-orders`, `codelab-movies`, `codelab-sample`, ...
- A single deployable container under `frontend/` (or `frontend/apps/container` in a future migration).
- URL isolation by convention: the `orders` module is mounted at `/orders/*`.
- Adding a module is dependency + configuration only: no routing code changes in the container.
- Shared UI, API client, and routing infrastructure remain container- or commons-owned.

### Non-goals

- No runtime module federation and no independently deployed module bundles.
- No per-module HTML entry point or separate dev server in production.
- No framework-specific state management beyond React and the module's own local state.
- No cross-module direct imports of internal implementation files.

## Backend-to-frontend analogy

| Backend (modular monolith)                       | Frontend (modular monolith)                           |
| ------------------------------------------------ | ----------------------------------------------------- |
| `CodelabModule` record                           | `CodelabFrontendModule` descriptor                    |
| `CodelabModuleProvider` (Spring factory SPI)     | Package default export calling `defineFrontendModule`  |
| `META-INF/spring.factories` discovery            | `codelab.modules.json` + Vite virtual module          |
| `@AutoConfiguration` + `@Import` services        | Module package entry exporting routes/providers        |
| `codelab-<module>` Maven dependency              | `@codelab/module-<name>` npm dependency                |
| Maven Local publish                              | Private npm registry / npm workspace symlinks          |
| `<module>.transactionManager` naming convention   | `/${name}` route root naming convention               |
| `@Transactional(transactionManager=...)`         | Module routes wrapped in an error boundary            |
| `MegalithApplication` container                   | Container Vite/React application                      |

## Proposed repository layout

### Package mode (independent builds, mirrors Maven Local workflow)

```text
codelab-orders/
  frontend/                          # @codelab/module-orders (Vite library)
frontend-commons/                    # @codelab/frontend-commons
frontend-ui/                         # @codelab/ui (shared components)
frontend/                            # @codelab/frontend-container (deployable Vite app)
  codelab.modules.json               # container module list
```

### Workspace mode (recommended for local DX)

For day-to-day development, wrap the projects in an npm workspace so modules are symlinked
instead of published:

```json
{
  "private": true,
  "workspaces": [
    "frontend",
    "frontend-commons",
    "frontend-ui",
    "frontend-modules/*"
  ]
}
```

A module can be developed, tested, and published on its own without workspace root changes.

## Shared contract: `@codelab/frontend-commons`

Every module produces one object of type `CodelabFrontendModule`.

```ts
// frontend-commons/src/module.ts
import type { RouteObject } from "react-router-dom";
import type { ComponentType, ReactNode } from "react";

export interface CodelabFrontendModule {
  /** Stable module identifier, e.g. "orders". Used for routing and diagnostics. */
  name: string;

  /** URL root. Convention: "/" + name. */
  basePath: string;

  /** Display title used by the shell. */
  title: string;

  /** React Router routes, expressed relative to basePath. */
  routes: RouteObject[];

  /** Optional navigation item contributed to the shell header. */
  navigation?: {
    label: string;
    to: string;
  };

  /** Optional providers rendered around this module's routes only. */
  providers?: ComponentType<{ children: ReactNode }>;

  /** Optional lazy route loader. Prefer for large modules. */
  loadRoutes?: () => Promise<RouteObject[]>;
}

export function defineFrontendModule(
  spec: CodelabFrontendModule
): CodelabFrontendModule {
  if (!/^[a-z][a-z0-9-]*$/.test(spec.name)) {
    throw new Error(`Invalid module name: ${spec.name}`);
  }
  if (spec.basePath !== `/${spec.name}`) {
    throw new Error(
      `Module ${spec.name} must use basePath "/${spec.name}" (got ${spec.basePath})`
    );
  }
  return spec;
}
```

A module entry point is small and explicit:

```ts
// @codelab/module-orders/src/index.ts
import OrdersHomePage from "./pages/OrdersHomePage";
import OrderDetailPage from "./pages/OrderDetailPage";
import { defineFrontendModule } from "@codelab/frontend-commons";

const module = defineFrontendModule({
  name: "orders",
  basePath: "/orders",
  title: "Orders",
  routes: [
    { index: true, element: <OrdersHomePage /> },
    { path: ":orderId", element: <OrderDetailPage /> },
  ],
  navigation: { label: "Orders", to: "/orders" },
});

export default module;
```

## Container composition

The container owns the application shell, routing infrastructure, and module discovery.

### Module discovery (config-only)

`frontend/codelab.modules.json` is the frontend equivalent of the backend module registry:

```json
{
  "modules": [
    "@codelab/module-orders",
    "@codelab/module-movies",
    "@codelab/module-sample"
  ]
}
```

A Vite plugin reads this file (and optionally package markers) and generates the virtual module
`virtual:codelab-frontend-modules`:

```ts
// frontend/plugins/codelabFrontendModules.ts (illustrative)
import { readFileSync } from "node:fs";
import type { Plugin } from "vite";

export function codelabFrontendModules(): Plugin {
  return {
    name: "codelab-frontend-modules",
    resolveId(id) {
      if (id === "virtual:codelab-frontend-modules") return id;
    },
    load(id) {
      if (id !== "virtual:codelab-frontend-modules") return;
      const config = JSON.parse(
        readFileSync("codelab.modules.json", "utf8")
      ) as { modules: string[] };
      const imports = config.modules
        .map((m, i) => `import m${i} from "${m}";`)
        .join("\n");
      const arr = config.modules.map((_m, i) => `m${i}`).join(", ");
      return `${imports}\nexport const frontendModules = [${arr}];`;
    },
  };
}
```

The container then imports the registry without per-module code:

```ts
// frontend/src/modules/registry.ts
export { frontendModules as frontendModules } from "virtual:codelab-frontend-modules";
```

When using npm workspaces, dependencies are resolved through normal node_modules symlinks.
In published mode, run `npm install @codelab/module-<name>` before adding the package name to
`codelab.modules.json`.

### Runtime composition

```tsx
// frontend/src/App.tsx
import { BrowserRouter, Link, Route, Routes } from "react-router-dom";
import { frontendModules } from "@/modules/registry";

export default function App() {
  const routes = frontendModules.flatMap((module) =>
    module.routes.map((route) => ({
      ...route,
      path:
        route.index || route.path === undefined
          ? module.basePath
          : `${module.basePath}/${route.path}`.replace(/\/+/g, "/"),
      element: <ModuleBoundary name={module.name}>{route.element}</ModuleBoundary>,
    }))
  );

  const navItems = frontendModules.flatMap((m) =>
    m.navigation ? [m.navigation] : []
  );

  return (
    <BrowserRouter>
      <div className="min-h-screen">
        <header className="border-b">
          <nav className="mx-auto flex max-w-7xl items-center gap-6 px-6 py-4">
            <Link to="/" className="font-semibold">
              Codelab
            </Link>
            {navItems.map((item) => (
              <Link key={item.to} to={item.to} className="text-sm">
                {item.label}
              </Link>
            ))}
          </nav>
        </header>

        <main className="mx-auto max-w-7xl px-6 py-8">
          <Routes>
            <Route path="/" element={<HomePage />} />
            {routes.map((route, index) => (
              <Route key={index} {...route} />
            ))}
            <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  );
}
```

`ModuleBoundary` adds a per-module `ErrorBoundary` and optional providers. A failure inside one
module should not unmount the shell or other modules.

## Module package standard

Each module is a Vite library. It externalizes React and shared packages so the container's
single copy is used.

```ts
// frontend-modules/orders/vite.config.ts
import path from "node:path";
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  build: {
    lib: {
      entry: path.resolve(__dirname, "src/index.ts"),
      formats: ["es"],
      fileName: "index",
    },
    rollupOptions: {
      external: [
        "react",
        "react-dom",
        "react-router-dom",
        "@codelab/frontend-commons",
        "@codelab/ui",
        "@codelab/api",
      ],
    },
  },
});
```

```json
// frontend-modules/orders/package.json
{
  "name": "@codelab/module-orders",
  "version": "1.0.0",
  "type": "module",
  "files": ["dist"],
  "main": "./dist/index.js",
  "types": "./dist/index.d.ts",
  "exports": {
    ".": {
      "types": "./dist/index.d.ts",
      "import": "./dist/index.js"
    }
  },
  "peerDependencies": {
    "react": "^19.0.0",
    "react-dom": "^19.0.0",
    "react-router-dom": "^7.0.0"
  },
  "dependencies": {
    "@codelab/frontend-commons": "^1.0.0",
    "@codelab/ui": "^1.0.0",
    "@codelab/api": "^1.0.0"
  }
}
```

Build, test, and publish commands mirror backend modules:

```bash
cd frontend-modules/orders
npm run build
npm test
npm publish
```

During local workspace development, `npm run dev` in the container sees module sources through
node_modules symlinks and supports HMR.

## Isolation conventions

### URLs and routing

- Module name is lowercase kebab-case (`orders`, `movie-ingest`).
- `basePath` must equal `/${name}`; the shared commons enforces this.
- A module must not register routes outside its `basePath`.
- Deep links follow the module's own convention, for example `/orders/:orderId`.

### API clients

- `@codelab/api` is reserved for genuinely container-wide concepts. Business modules should
  generate and own their own per-module clients, as described in "Contracts and models at
  module level".
- A module may call endpoints owned by another module only through explicitly published module
  APIs or container-wide APIs, never by importing another module's internals.
- Module-owned helpers belong inside the module package under `src/api`.

### Assets and styling

- Shared components come from `@codelab/ui`.
- Tailwind utility classes are allowed in modules. The container's Tailwind configuration must scan module source or built output:
  - Workspace mode: include `../frontend-modules/*/src/**/*.{ts,tsx}`.
  - Published mode: modules export a side-effect CSS file from their entry point.
- Module-specific CSS must use a stable prefix or CSS modules to avoid class collisions.

### State and side effects

- Modules own their own component state and query cache.
- No module may mutate another module's stored state.
- Cross-module communication uses URL navigation and shared API calls, not shared mutable stores.

### Error isolation

- Every module route subtree is rendered behind an error boundary.
- A failing module renders a fallback and keeps the rest of the shell usable.

## Shared style and API generation

The current generated client lives in `frontend/src/generated`. During migration it may remain as
the container-wide `@codelab/api` package, but new business-module APIs should be generated per
module against module-scoped OpenAPI documents:

```text
frontend/src/generated                # transitional container-wide client
frontend-modules/orders/src/generated # preferred module-owned client
```
Under this architecture, container-wide APIs become `@codelab/api`, generated from the megalith
OpenAPI document and consumed by shell code only:

```bash
cd packages/api  # or the current generated entry point
npm run build
npm publish
```

The container keeps its Vite dev proxy for API calls:

```ts
server: {
  proxy: {
    "/api": {
      target: "http://localhost:8080",
      changeOrigin: true,
    },
  },
}
```

## Contracts and models at module level

Business modules remain independently understandable only if their frontend models and API
contracts are owned by the same module boundary. The rule is: **the backend module is the
source of truth; the frontend module owns its generated client and exported view models.**

### 1. Backend: expose a module-scoped OpenAPI contract

Each backend module contributes its controllers to the megalith OpenAPI document. The
megalith exposes both the aggregate document and a module-scoped document:

- aggregate: `http://localhost:8080/api-docs`
- module: `http://localhost:8080/api-docs/<module-name>` (for example `/api-docs/orders`)

Module-scoped documents should be grouped by controller tags such as `orders`, `products`,
and `customers` so generated output is stable even when unrelated modules change.

### 2. Frontend: generate per-module API and model packages

Each frontend module package owns its generated client, not the container. The recommended
location is a generated subdirectory inside the module package:

```text
frontend-modules/orders/
  package.json
  src/
    generated/
      models/
      apis/
      runtime.ts
    api/
      ordersClient.ts        # thin wrappers around generated APIs
    pages/
      OrdersHomePage.tsx
    index.ts
  openapi-generator.yaml     # module-scoped generator config
```

The module's generator configuration points at its own backend document:

```yaml
# frontend-modules/orders/openapi-generator.yaml
generatorName: typescript-fetch
inputSpec: http://localhost:8080/api-docs/orders
outputDir: src/generated

additionalProperties:
  supportsES6: true
  npmName: "@codelab/module-orders-api"
  typescriptThreePlus: true
```

Generated files are checked into the module repository (or published as
`@codelab/module-orders-api`). This keeps a frontend module buildable without booting the
entire megalith.

```json
{
  "scripts": {
    "generate-api": "openapi-generator-cli generate -c openapi-generator.yaml",
    "build": "npm run generate-api && vite build"
  }
}
```

Regenerate the client whenever the backend module's DTOs or controllers change, then bump the
frontend module version so consumers receive the contract change explicitly.

### 3. Module exposes only intentional view models

Generated DTOs are implementation details of the module's API layer. Public components and
pages should consume a smaller, intentional set of frontend view models exported by the
module:

```ts
// @codelab/module-orders/src/models/OrderSummary.ts
export interface OrderSummary {
  id: string;
  status: "draft" | "submitted" | "shipped";
  totalLabel: string;
}
```

Mapping from generated DTO to view model happens inside the module:

```ts
// @codelab/module-orders/src/api/orderMappers.ts
import type { GetOrderResponse } from "../generated";
import type { OrderSummary } from "../models/OrderSummary";

export function toOrderSummary(dto: GetOrderResponse): OrderSummary {
  return {
    id: String(dto.id),
    status: dto.status.toLowerCase() as OrderSummary["status"],
    totalLabel: new Intl.NumberFormat().format(dto.total),
  };
}
```

This prevents backend schema churn from becoming a breaking change in shell-level UI
components and keeps presentation concerns inside the module.

### 4. Shared package strategy for cross-module models

- **Within one module:** use that module's generated client and view models.
- **Across modules:** use `@codelab/ui` for generic presentation and `@codelab/api` only for
  truly shared, container-wide concepts such as health checks or shell metadata.
- **Do not import another module's generated client directly.** Cross-module data flow should
  go through navigation, URLs, or documented REST calls via `@codelab/api`.

### 5. Event and job contracts

Frontend modules do not consume backend events or jobs directly; those are server-side concerns.
When a frontend module needs to trigger event-producing behavior, it calls a REST endpoint
owned by the corresponding backend module. The frontend contract for that action is the REST
DTO generated per module, not the event payload.

### 6. Validation ownership

- Backend DTO validation remains authoritative.
- Frontend modules may use lightweight runtime validation for form inputs, but must not become
  the source of truth for persistence constraints.
- If a form model spans multiple generated DTOs, define a local view model in the module and
  map it explicitly before submission.

## Adding a new frontend module

1. Create `frontend-modules/<name>` from the module template.
2. Implement the module's pages and call `defineFrontendModule` in `src/index.ts`.
3. Add the module to the root workspace (workspace mode) or publish it (package mode).
4. Install it into the container: `npm install @codelab/module-<name>`.
5. Add `"@codelab/module-<name>"` to `frontend/codelab.modules.json`.
6. Rebuild and rerun the container.

No container routing code, navigation code, or TypeScript imports need to change after step 5.
This mirrors the backend rule that a new module only requires a dependency and SPI registration.

## Testing

- Each module has its own Vitest + React Testing Library suite.
- Descriptor validation is tested in `@codelab/frontend-commons`: name, `basePath`, and route invariants.
- The container has integration tests that render the shell with a fake module and assert route mounting.
- End-to-end smoke tests check `/<module>` for every configured module.

## Trade-offs and when not to use this

- This model optimizes for one deployable artifact and simple operations, like the backend megalith.
- It does **not** provide independent deployment of individual module bundles. If a team requires
  runtime independent releases, use Module Federation instead. Federation trades this document's
  simplicity for deploy-time decoupling.
- Build-time composition requires a container rebuild when a module changes. Version pins in
  `package.json` make these updates explicit and reviewable.
- The container's Tailwind scanning must be kept in sync with installed module packages.

## Related documents

- `frontend/README.md` - container build and development commands.
- `codelab-megalith/PROJECT.md` - backend container architecture.
- `docs/platform/MAP.md` (when available) - module inventory and API surface.
