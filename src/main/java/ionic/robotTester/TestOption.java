package ionic.robotTester;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import edu.wpi.first.units.measure.Time;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;

/**
 * Pure definition of a test case.
 *
 * <p>
 * A {@code TestOption} represents the logic and metadata of a test without
 * any execution responsibility. It does not schedule itself or manage timing,
 * alerts, or lifecycle execution. Those responsibilities are handled externally
 * (typically by {@code RobotTester}).
 * </p>
 *
 * <p>
 * This class is intentionally framework-agnostic and acts as a description of
 * test behavior rather than a runtime controller.
 * </p>
 */
public class TestOption {

  /**
   * List of conditional failure checks evaluated by the test runner.
   */
  public final List<FailCheck> failChecks = new ArrayList<>();

  /**
   * Human-readable notes describing the purpose or behavior of the test.
   */
  public final List<String> notes = new ArrayList<>();

  private String displayName;

  private boolean failed = false;
  private String failMessage = "";

  private Optional<Time> timeout = Optional.empty();

  private Runnable cleanup = () -> {};

  /**
   * Creates a new test definition with the given name.
   *
   * @param name Initial display name of the test.
   */
  public TestOption(String name) {
    this.displayName = name;
  }

  /**
   * Sets or updates the display name of the test.
   *
   * @param name New test name.
   * @return This instance for chaining.
   */
  public TestOption withName(String name) {
    this.displayName = name;
    return this;
  }

  /**
   * Adds a conditional failure check evaluated during execution.
   *
   * <p>
   * If the condition evaluates to true, the test should be marked as failed
   * by the external runner.
   * </p>
   *
   * @param condition Condition that triggers failure when true.
   * @param message   Message describing the failure reason.
   * @return This instance for chaining.
   */
  public TestOption failIf(BooleanSupplier condition, String message) {
    failChecks.add(new FailCheck(condition, message));
    return this;
  }

  /**
   * Sets an optional timeout for the test.
   *
   * <p>
   * The timeout itself is not enforced here; it is only stored for use by the
   * test runner.
   * </p>
   *
   * @param time Maximum allowed test duration.
   * @return This instance for chaining.
   */
  public TestOption withTimeout(Time time) {
    timeout = Optional.ofNullable(time);
    return this;
  }

  /**
   * Adds a human-readable note about this test.
   *
   * @param note Text description or context for the test.
   * @return This instance for chaining.
   */
  public TestOption addNote(String note) {
    notes.add(note);
    return this;
  }

  /**
   * Sets a cleanup action to run after the test completes.
   *
   * @param cleanup Runnable cleanup logic.
   * @return This instance for chaining.
   */
  public TestOption withCleanup(Runnable cleanup) {
    this.cleanup = cleanup;
    return this;
  }

  /**
   * Sets a cleanup action using a WPILib {@link Command}.
   *
   * <p>
   * The command is scheduled when cleanup is executed.
   * </p>
   *
   * @param cmd Command to schedule on cleanup.
   * @return This instance for chaining.
   */
  public TestOption withCleanup(Command cmd) {
    this.cleanup = () -> CommandScheduler.getInstance().schedule(cmd);
    return this;
  }

  /**
   * Returns all notes joined into a single string.
   *
   * @return Combined note text.
   */
  public String getNotes() {
    return String.join("\n", notes);
  }

  /**
   * Gets the display name of the test.
   *
   * @return Test name.
   */
  public String getTestName() {
    return displayName;
  }

  /**
   * Returns the configured timeout, if any.
   *
   * @return Optional timeout duration.
   */
  public Optional<Time> getTimeout() {
    return timeout;
  }

  /**
   * Indicates whether the test has been marked as failed.
   *
   * @return True if the test failed.
   */
  public boolean hasFailed() {
    return failed;
  }

  /**
   * Returns the failure message, if the test has failed.
   *
   * @return Failure message or empty string if none.
   */
  public String getFailMessage() {
    return failMessage;
  }

  /**
   * Returns all configured failure checks.
   *
   * @return List of failure conditions.
   */
  public List<FailCheck> getFailChecks() {
    return failChecks;
  }

  /**
   * Returns the cleanup action associated with this test.
   *
   * @return Cleanup runnable.
   */
  public Runnable getCleanup() {
    return cleanup;
  }

  /**
   * Called when the test is initialized.
   *
   * <p>
   * Intended to be overridden in subclasses if behavior is needed.
   * </p>
   */
  public void initialize() {}

  /**
   * Called repeatedly during test execution.
   *
   * <p>
   * Intended to be overridden in subclasses.
   * </p>
   */
  public void execute() {}

  /**
   * Called when the test ends.
   *
   * @param interrupted Whether the test was interrupted externally.
   */
  public void end(boolean interrupted) {}

  /**
   * Determines whether the test has completed.
   *
   * @return True if the test should finish.
   */
  public boolean isFinished() {
    return false;
  }

  /**
   * Marks the test as failed with the given message.
   *
   * <p>
   * Only the first failure message is stored.
   * </p>
   *
   * @param message Reason for failure.
   */
  public final void fail(String message) {
    if (!failed) {
      failed = true;
      failMessage = message;
    }
  }

  /**
   * Resets the failure state of this test.
   */
  public void reset() {
    failed = false;
    failMessage = "";
  }

  /**
   * Represents a single conditional failure rule.
   */
  public static class FailCheck {
    /**
     * Condition that triggers a failure when true.
     */
    public final BooleanSupplier condition;

    /**
     * Message associated with this failure condition.
     */
    public final String message;

    /**
     * Creates a new failure check.
     *
     * @param condition Condition to evaluate.
     * @param message   Failure message if triggered.
     */
    public FailCheck(BooleanSupplier condition, String message) {
      this.condition = condition;
      this.message = message;
    }
  }

  /**
   * Wraps a WPILib {@link Command} as a {@link TestOption}.
   *
   * <p>
   * This allows existing commands to be used as tests by delegating lifecycle
   * methods.
   * </p>
   *
   * @param cmd Command to wrap.
   * @return A TestOption delegating to the command.
   * @throws IllegalArgumentException if cmd is null.
   */
  public static TestOption wrap(Command cmd) {
    if (cmd == null) {
      throw new IllegalArgumentException("Wrapped command cannot be null");
    }

    return new TestOption(cmd.getName() + " Test") {
      @Override
      public void initialize() {
        cmd.initialize();
      }

      @Override
      public void execute() {
        cmd.execute();
      }

      @Override
      public void end(boolean interrupted) {
        cmd.end(interrupted);
      }

      @Override
      public boolean isFinished() {
        return cmd.isFinished();
      }
    };
  }
}