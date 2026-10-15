# codelab-sample

Tiny reference module for the megalith. It intentionally mirrors the shape of
`codelab-orders` and `codelab-movies` but stays small enough to read in one
sitting, so agents and contributors can use it as a checklist of "what does a
module need to do, and how".

## What it demonstrates

- **Dynamic data source / persistence unit** - `SampleModuleProvider` declares
  the `sample` database and `samplePU` persistence unit. `SampleServiceImpl`
  activates transactions with `@Transactional(transactionManager = "sample.transactionManager")`.
- **REST controller** - `SampleController` exposes `/api/samples`.
- **Events** - `createSample` publishes a `SampleCreatedEvent` with an explicit
  `BusinessVersion.of("v1")` via `SampleCreatedEventPublisher`; `SampleCreatedEventProcessor`
  consumes `created-samples` and declares supported versions.
- **Jobs** - `submitForProcessing` publishes a `SampleJob` via `SampleJobPublisher`;
  `SampleJobProcessor` consumes `pending-samples` with a `DeadLetterPolicy` and updates
  the row in the `sample` transaction.
- **Headless tasks** - `sample.ping` and `sample.feed` (`CodelabTask` implementations)
  run against the application context via `--codelab.task=...`.

## Invocation

```
./gradlew bootRun --args='--codelab.task=list'
./gradlew bootRun --args='--codelab.task=sample.ping'
./gradlew bootRun --args='--codelab.task=sample.feed'
```

## Build

```
cd codelab-sample
./gradlew spotlessApply check
./gradlew publishToMavenLocal
```
