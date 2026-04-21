package ionic.robotTester;

import java.util.ArrayList;
import java.util.List;

import static edu.wpi.first.units.Units.Seconds;
import edu.wpi.first.units.measure.Time;
import edu.wpi.first.wpilibj.Alert;
import edu.wpi.first.wpilibj.Alert.AlertType;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.button.RobotModeTriggers;

/**
 * Central manager responsible for registering, selecting, and executing
 * {@link TestOption} instances.
 *
 * <p>
 * Unlike {@link TestOption}, which only defines test logic and metadata,
 * {@code RobotTester} is responsible for runtime behavior including:
 * <ul>
 * <li>Scheduling test execution via {@link CommandScheduler}</li>
 * <li>Evaluating failure conditions</li>
 * <li>Enforcing timeouts</li>
 * <li>Managing alerts and UI feedback</li>
 * <li>Publishing test selection to {@link SmartDashboard}</li>
 * </ul>
 * </p>
 *
 * <p>
 * This class acts as the execution engine for the testing framework,
 * while {@link TestOption} remains a pure description of test behavior.
 * </p>
 */
public class RobotTester {

  /**
   * Controls when and how the tester UI is published to {@link SmartDashboard}.
   */
  public enum VerbosityMode {
    /**
     * UI is only published when the robot enters test mode.
     */
    kOnlyInTestMode,

    /**
     * UI is not automatically published; caller must manage it manually.
     */
    kManual,

    /**
     * UI is always published immediately.
     */
    kAlways
  }

  private final SendableChooser<TestOption> m_testChooser = new SendableChooser<>();
  private final List<TestOption> m_testOptions = new ArrayList<>();

  private final Alert m_runningTestAlert =
      new Alert("Running Test: None", AlertType.kInfo);

  private TestOption m_currentTestOption;

  /**
   * Creates a new {@code RobotTester}.
   *
   * <p>
   * Depending on {@link VerbosityMode}, this will automatically publish or
   * defer publishing of the test selection UI.
   * </p>
   *
   * @param mode Controls SmartDashboard integration behavior.
   */
  public RobotTester(VerbosityMode mode) {

    m_testChooser.onChange(option -> {
      m_currentTestOption = option;
      SmartDashboard.putString("RobotTester/Name", getCurrentName());
      SmartDashboard.putString("RobotTester/Notes", getCurrentNotes());
    });

    if (mode == VerbosityMode.kOnlyInTestMode) {
      RobotModeTriggers.test().onTrue(
          Commands.runOnce(() -> {
            SmartDashboard.putData("RobotTester/Chooser", m_testChooser);
            SmartDashboard.putData(
                "RobotTester/Run Current",
                new InstantCommand(this::runCurrentOption));
          })
      );
    } else if (mode == VerbosityMode.kAlways) {
      SmartDashboard.putData("RobotTester/Chooser", m_testChooser);
      SmartDashboard.putData(
          "RobotTester/Run Current",
          new InstantCommand(this::runCurrentOption));
    }
  }

  /**
   * Registers a new {@link TestOption} and adds it to the selection menu.
   *
   * @param option Test definition to register.
   */
  public void addTestOption(TestOption option) {
    m_testOptions.add(option);
    m_testChooser.addOption(option.getTestName(), option);
  }

  /**
   * Returns all registered test options.
   *
   * @return List of available tests.
   */
  public List<TestOption> getM_testOptions() {
    return m_testOptions;
  }

  /**
   * Returns the currently selected test option.
   *
   * @return Selected test, or {@code null} if none selected.
   */
  public TestOption getCurrentOption() {
    return m_currentTestOption;
  }

  /**
   * Returns the name of the currently selected test.
   *
   * @return Test name, or "None" if no test is selected.
   */
  public String getCurrentName() {
    return m_currentTestOption != null
        ? m_currentTestOption.getTestName()
        : "None";
  }

  /**
   * Returns notes associated with the currently selected test.
   *
   * @return Notes string or empty string if none selected.
   */
  public String getCurrentNotes() {
    return m_currentTestOption != null
        ? m_currentTestOption.getNotes()
        : "";
  }

  /**
   * Returns a command that runs the currently selected test option.
   *
   * <p>
   * Intended for use in dashboard bindings or manual triggers.
   * </p>
   *
   * @return Command that executes the selected test.
   */
  public Command getRunCurrentOptionCommand() {
    return new InstantCommand(this::runCurrentOption);
  }

  /**
   * Executes the currently selected {@link TestOption}, if one exists.
   *
   * <p>
   * This method:
   * <ul>
   * <li>Updates the running test alert</li>
   * <li>Wraps the test in a runtime {@link Command}</li>
   * <li>Schedules execution via {@link CommandScheduler}</li>
   * </ul>
   * </p>
   */
  public void runCurrentOption() {
    if (m_currentTestOption == null) {
      return;
    }

    m_runningTestAlert.setText(
        "Running Test: "
            + m_currentTestOption.getTestName()
            + "\n"
            + m_currentTestOption.getNotes());
    m_runningTestAlert.set(true);

    CommandScheduler.getInstance()
        .schedule(createCommand(m_currentTestOption));
  }

  /**
   * Creates a runtime wrapper {@link Command} for executing a {@link TestOption}.
   *
   * <p>
   * This wrapper is responsible for:
   * <ul>
   * <li>Executing lifecycle methods of the test</li>
   * <li>Evaluating failure conditions</li>
   * <li>Enforcing timeout rules</li>
   * <li>Triggering alerts and reporting results</li>
   * </ul>
   * </p>
   *
   * @param option Test definition to execute.
   * @return Wrapped command used by the scheduler.
   */
  private Command createCommand(TestOption option) {
    return new Command() {

      private boolean failed = false;
      private Time startTime = Seconds.of(0);

      private final Alert errorAlert =
          new Alert(option.getTestName(), AlertType.kError);
      private final Alert infoAlert =
          new Alert(option.getTestName(), AlertType.kInfo);

      @Override
      public void initialize() {
        failed = false;
        startTime = Seconds.of(Timer.getFPGATimestamp());

        errorAlert.set(false);
        infoAlert.set(false);

        option.reset();
        option.initialize();
      }

      @Override
      public void execute() {
        option.execute();

        for (var check : option.getFailChecks()) {
          if (check.condition.getAsBoolean()) {
            fail(check.message);
            break;
          }
        }

        option.getTimeout().ifPresent(t -> {
          double elapsed = Timer.getFPGATimestamp() - startTime.in(Seconds);
          if (elapsed >= t.in(Seconds)) {
            fail("Timed out after " + t.in(Seconds) + "s");
          }
        });
      }

      @Override
      public void end(boolean interrupted) {
        option.end(interrupted);
        option.getCleanup().run();

        if (!failed && !interrupted) {
          infoAlert.setText(option.getTestName() + ": Succeeded");
          infoAlert.set(true);
        }

        m_runningTestAlert.set(false);
      }

      @Override
      public boolean isFinished() {
        return failed || option.isFinished();
      }

      private void fail(String message) {
        if (!failed) {
          failed = true;

          errorAlert.setText(option.getTestName() + ": " + message);
          errorAlert.set(true);
          infoAlert.set(false);
        }
      }
    };
  }
}