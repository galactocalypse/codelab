package com.codelab.common.spring.task;

/**
 * A module-contributed, headless unit of work invoked on demand through the megalith's task
 * dispatcher ({@code bootRun --args='--codelab.task=<name>'}).
 *
 * <p>Implementations are discovered by the module registrar (they must carry {@link
 * CodelabTaskSpec} and live under the module base package) and are instantiated only when they are
 * the selected task, so a normal web boot never pays for them. A task runs against the megalith's
 * {@code ApplicationContext} — it may use the module's repositories, services, and Pulsar producers
 * — and must be a one-shot, bounded unit of work (see {@code HEADLESS_ENTRY_POINTS.md}).
 *
 * <p>Task-specific arguments are read from the context's {@code ApplicationArguments} (inject it
 * via the task's constructor) rather than from the contract.
 */
@FunctionalInterface
public interface CodelabTask {

  /**
   * Runs the task to completion.
   *
   * @return the process exit code — {@code 0} for success, non-zero for failure
   * @throws Exception if the task fails; the dispatcher reports the failure and exits non-zero
   */
  int run() throws Exception;
}
