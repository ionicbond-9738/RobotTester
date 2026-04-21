// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import static edu.wpi.first.units.Units.Seconds;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import ionic.robotTester.RobotTester;
import ionic.robotTester.TestOption;

public class RobotContainer {
  public RobotTester m_tester = new RobotTester(RobotTester.VerbosityMode.kAlways);

  public RobotContainer() {
    m_tester.addTestOption(
        TestOption.wrap(new InstantCommand(() -> System.out.println("hello")))
            .withTimeout(Seconds.of(1)));
  }

  public Command getAutonomousCommand() {
    // An example command will be run in autonomous
    return null;
  }
}
