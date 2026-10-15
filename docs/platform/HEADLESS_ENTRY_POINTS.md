# Codelab headless entry points (module-contributed tasks)

**Status:** implemented. The contract, discovery, dispatcher, and the migration of the movie runners
are live; see §4. §1–§3 record the problem, rationale, and applicability that motivated the design.

Modules need a first-class way to expose **headless entry points** — operator/dev-invoked units of
work such as an import, a feed/seed, a backfill, a diagnostic, or an admin/migration command. This
document states the problem that motivates a shared abstraction, argues why an annotated task
contract plus a single dispatcher is the right fit for this repository, and draws the line around the
kinds of work that do and do not belong under that abstraction.

---

## 1. The problem

### 1.1 Only the megalith owns an application context

`codelab-movies`, `codelab-orders`, etc. are `java-library` modules: no Spring Boot plugin, no
`main`, and no independent knowledge of infrastructure. They contribute types and `@AutoConfiguration`
and declare their module descriptor through the `CodelabModuleProvider` Spring-factory SPI; the
megalith discovers them (`CodelabModuleRegistrar`), wires their per-module persistence and messaging,
and applies their autoconfiguration.

The direct consequence for this design: **any "run this job" capability a module exposes necessarily
executes inside the megalith's `ApplicationContext`**, with every module's infrastructure already
live — DataSource, EntityManagerFactory, transaction managers, and Pulsar producers/consumers. There
is no such thing as a module running in its own context, and no way to give it one without violating
the library invariant. "Headless" here therefore means *no web server and a one-shot run*, **not**
*an isolated context*.

### 1.2 The prior mechanism

*Before this design*, each entry point was a `CommandLineRunner` bean exported from the owning
module's autoconfiguration and gated by a module-scoped boolean property:

```java
// MoviesAutoConfiguration (prior)
@Bean
@ConditionalOnProperty(name = "movies.feed", havingValue = "true")
public MovieFeedRunner movieFeedRunner(...) { ... }

@Bean
@ConditionalOnProperty(name = "movies.import", havingValue = "true")
public MovieImportRunner movieImportRunner(...) { ... }
```

Invocation was `bootRun --args='--movies.feed=true'`, plus
`--spring.main.web-application-type=none` and manual exit handling to make it a one-shot.

That was adequate for one or two jobs and was the right first step for the movie importer. It is not a
framework. §4 describes what replaced it.

### 1.3 Why it does not generalize

1. **Property-space pollution.** N modules × M jobs becomes N×M loosely-named booleans. Nothing
   namespaces or validates them; a typo silently does nothing, and there is no central list.
2. **No discovery.** To learn that a job exists, and what selects it, you read source or docs. There
   is no `--list`-style introspection.
3. **No "exactly one" semantics.** Multiple `CommandLineRunner` beans all fire when several flags are
   set; their order is bean-registration order, which is incidental.
4. **Combinatorial ambiguity.** `--movies.feed=true --movies.import=true` runs both. There is no
   notion of *selecting* a job — only of *enabling* beans.
5. **Per-job boilerplate and re-earned guarantees.** Every job re-implements gating, and the promise
   "a normal boot runs no job" must be re-established for each bean and each module.
6. **Lifecycle coupling.** A `CommandLineRunner` can only be exercised by booting the whole context,
   so jobs are awkward to unit-test in isolation.
7. **Duplicated invocation ergonomics.** Every call site repeats
   `--spring.main.web-application-type=none` and exit-code plumbing.
8. **No shared metadata or contract.** There is no name, description, argument handling, or exit-code
   convention to build tooling or a runbook on.

---

## 2. Why the proposed solution fits

The proposal: a `CodelabTask` contract plus a `@CodelabTaskSpec` annotation in `codelab-commons`,
tasks declared by modules (and discovered by the megalith through the same per-module package scan
already used for topics and subscriptions), and a **single dispatcher** in the megalith driven by one
selector, `--codelab.task=<name>` (with `--codelab.task=list` for discovery). A normal boot runs no
task; a selected task runs exactly one, forces a headless one-shot, and returns its exit code.

Each property of the solution answers a specific failure from §1.3:

- **It reuses the repository's existing extension idiom.** `@CodelabTopic` / `@CodelabSubscription` /
  `@CodelabJobSubscription` are all *annotated contracts in `codelab-commons` + module implementation
  + per-module discovery/wiring in the megalith*. A task SPI is the same shape, so it introduces no new
  mental model — it names the one that is already there. (§1.3: 1)
- **One selector replaces N booleans.** `--codelab.task=<name>` is a single, namespaced handle; unknown
  names fail fast with the valid list. (§1.3: 1)
- **`list` is discovery.** The dispatcher can enumerate every task with its name and description,
  which is exactly the metadata §1.3.8 calls for. (§1.3: 2, 8)
- **The dispatcher runs the named task and only that task.** This makes "exactly one" the default and
  removes the multi-runner ordering/ambiguity problems. (§1.3: 3, 4)
- **"A normal boot runs no task" is one rule, not one rule per bean.** Tasks are inert declarations;
  nothing executes unless selected. (§1.3: 5)
- **It encodes the §1.1 boundary explicitly rather than by accident.** A task is a module-owned
  collaborator surfaced to the application lifecycle; the megalith remains the sole provider of
  context and infrastructure. The SPI makes "modules never own context" a structural fact.
- **One-shot and exit handling are centralized.** The dispatcher supplies the
  `web-application-type=none` and `SpringApplication.exit(...)` behavior once. (§1.3: 7)
- **Tasks are plain objects.** They can be constructed and invoked in a unit test without starting a
  context. (§1.3: 6)

The alternatives are worse here:

- **Keep the boolean gates.** Fine for a single cheap job; degrades along every axis above as jobs
  multiply. It is the status quo this design is meant to outgrow.
- **Give each module its own Boot application / `main`.** Contradicts the module-is-a-library
  invariant and would duplicate the infra wiring the megalith exists to centralize.
- **Add a second `@SpringBootApplication` per job in the megalith.** Leaks module concerns into the
  app module, does not scale, and reintroduces the multi-main ambiguity the `springBoot { mainClass }`
  pin was added to paper over.
- **Trigger jobs over REST.** Wrong trust and tooling model for operator/dev one-shots, and it forces
  the web server up — the opposite of "headless."

---

## 3. Applicability: what fits and what does not

The abstraction is deliberately narrow. It is for **a finite set of named, manually-invoked,
one-at-a-time units of work that run against the application context.**

### 3.1 Fits

- A discrete, nameable unit of work invoked by an operator or developer (not by the request path).
- A finite, known set per module, where selecting exactly one is meaningful.
- One-shot or bounded-duration; the process is expected to finish and exit.
- Legitimately uses live module collaborators — repositories, services, Pulsar producers, the
  filesystem.
- Typical shapes: directory imports, feed/seed loaders, backfills, diagnostics and reports,
  admin or migration commands, cache warmers.

### 3.2 Does not fit — and what to use instead

| Kind of work | Why not a task | Use instead |
|---|---|---|
| Continuous stream/resource consumption | It never "finishes"; it is a subscription, not a run | `CodelabJobConsumer` / `CodelabEventConsumer` (Pulsar) |
| Recurring / scheduled work | A task is invoked once on demand | Spring `@Scheduled` / Quartz |
| Work triggered by an HTTP request or user action | Belongs on the request path, server up | REST controller, or publish an event |
| Work needing an isolated context / own classpath | Impossible for a module (library, no context) | A separate application, not a module task |
| A single trivial job with no siblings | The SPI's cost is not repaid yet | The existing `@ConditionalOnProperty` gate is fine |
| Multi-step pipelines with dependencies, fan-out, or retries | Out of scope for a flat task registry | Compose tasks, or model it on the job bus |

---

## 4. The implemented design

### 4.1 Contract and annotation (`codelab-commons`)

`com.codelab.common.spring.task`:

```java
@FunctionalInterface
public interface CodelabTask {
  int run() throws Exception;   // 0 = success; non-zero = failure
}

@Target(TYPE) @Retention(RUNTIME) @Documented
public @interface CodelabTaskSpec {
  String value();               // global name, e.g. "movies.feed"
  String description() default "";
}
```

`run()` returns the exit code rather than throwing on failure, so a task can report a non-zero,
non-error outcome (partial import, "nothing to do"). A thrown exception is treated as failure by the
dispatcher. Task-specific arguments are read from the context's `ApplicationArguments` (inject it via
the constructor) — the contract intentionally stays argument-free.

### 4.2 Discovery and registration (`codelab-megalith`)

`CodelabModuleRegistrar` already loops the discovered modules to register JPA and Pulsar beans; that
loop now also calls `CodelabTaskRegistryUtils.registerTasks(module, context)`. For each module the
registry scans `module.basePackage()` with the same `CodelabPackageScanner` used for consumers
(`scan(basePackage, CodelabTask.class, includeInterfaces=false, includeConcreteClasses=true)`),
validates `@CodelabTaskSpec`, and registers each implementation as a **lazy** singleton
`genericBeanDefinition` (bean name from `CodelabBeanNaming.deriveBeanName(module, SimpleName)`, e.g.
`movies.MovieFeedTask`, with a `codelab.task.module` attribute). Registration is definition-only —
**no task is instantiated at boot**.

After the loop, `registerCatalog(...)` collapses every module's descriptors into a single
`CodelabTaskCatalog` (bean `codelabTaskCatalog`), failing fast if two modules claim the same task name.
Because the catalog is a normal bean, it depends on none of the tasks and never triggers them.

### 4.3 The dispatcher (`CodelabTaskRunner`)

A single `CommandLineRunner` (bean declared in `MegalithConfiguration`) owns selection:

| Invocation | Behavior |
|---|---|
| (no `--codelab.task`) | Normal web boot. Dispatcher returns immediately; nothing runs. |
| `--codelab.task=list` | Prints name, module, and description for every registered task. |
| `--codelab.task=<name>` | Resolves that one task from the bean factory and runs it. |
| `--codelab.task=<unknown>` | Logs an error with the valid names; exit code 2. |
| task throws | Logs the failure; exit code 1. |
| task returns N | Exit code N. |

The selected task is resolved **lazily** from the `ListableBeanFactory` by bean name, so only the
chosen task is ever instantiated — the heavy ones cost nothing on a normal boot. One selector with
one active task means no multi-runner ordering or enabled-set ambiguity.

### 4.4 Headless boot and exit (`MegalithApplication`)

```java
boolean headless = CodelabTaskRunner.isRequested(args);   // parses --codelab.task=
if (headless) application.setWebApplicationType(WebApplicationType.NONE);

ConfigurableApplicationContext context = application.run(args);

if (headless) {
  System.exit(SpringApplication.exit(
      context, () -> context.getBean(CodelabTaskRunner.class).getExitCode()));
}
```

The web-application-type and exit plumbing live here once, not at every call site. A normal boot is
untouched: the selector's absence means the context stays a web app and `main` returns normally.

### 4.5 Migration of the movie runners

- `MovieFeedRunner` → **`MovieFeedTask`** (`@CodelabTaskSpec("movies.feed")`): publishes movie ids from
  `${movies.directory}`; `movies.feed.limit` bounds the run.
- `MovieImportRunner` → **`MovieImportTask`** (`@CodelabTaskSpec("movies.import")`): parses the
  directory in-process and prints a genre histogram.

Both now `implements CodelabTask`; their two `@Bean` methods were removed from `MoviesAutoConfiguration`
(leaving the `@Import`s and `movieDeadLetterPolicy`), so the module no longer advertises runner beans.
Invocation is:

```bash
./gradlew bootRun --args='--codelab.task=movies.feed'
./gradlew bootRun --args='--codelab.task=movies.import'
./gradlew bootRun --args='--codelab.task=list'
```

### 4.6 Testing

A task is now a plain object, so it is unit-tested by constructing it directly and calling `run()`
(see `MovieFeedTaskTest`) — no application context required. This is the §1.3.6 coupling resolved: the
registrar path adds a `WebApplicationContext` only when integration coverage of discovery is wanted.

---

*§1–§3 are the durable framing; §4 describes the current implementation and the two migrated tasks.*
