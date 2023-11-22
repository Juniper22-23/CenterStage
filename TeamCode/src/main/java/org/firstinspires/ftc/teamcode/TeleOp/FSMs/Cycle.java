package org.firstinspires.ftc.teamcode.TeleOp.FSMs;

import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.arcrobotics.ftclib.gamepad.GamepadKeys;
import com.arcrobotics.ftclib.hardware.motors.Motor;
import com.qualcomm.hardware.rev.RevColorSensorV3;
import com.qualcomm.robotcore.hardware.Servo;
import com.acmerobotics.dashboard.FtcDashboard;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.TeleOp.Mechanisms.Axons.Arm;
import org.firstinspires.ftc.teamcode.TeleOp.Mechanisms.FieldCentricDrive;
import org.firstinspires.ftc.teamcode.TeleOp.Mechanisms.HWMap;
import org.firstinspires.ftc.teamcode.TeleOp.Mechanisms.Slides;
//import org.firstinspires.ftc.teamcode.TeleOp.Mechanisms.TransferController;

public class Cycle {

    public enum CycleFSM {
        start,
        extend,
        retract,
        outtakeLeft,
        outtakeRight
    }

    private final Arm arm;
    private final Slides slides;
    private CycleFSM state = CycleFSM.start;
//    private TransferController transferController;

    private final Motor intakeMotor;
    private final Servo outakeServoLeft;
    private final Servo outakeServoRight;
    private final RevColorSensorV3 colorSensorLeft;
    private final RevColorSensorV3 colorSensorRight;
    private final FieldCentricDrive fieldCentricDrive;
    private boolean pixelInLeft = false;
    private boolean pixelInRight = false;
    private final double leftReleasePixelPos = 0.5;
    private final double rightReleasePixelPos = 0.5;
    private boolean stopRequested = false;
    private boolean toTransfer = false;


    private final GamepadEx gamepad;
    private Telemetry telemetry;

    public Cycle(HWMap hwMap, GamepadEx gamepad, Telemetry telemetry, FieldCentricDrive fieldCentricDrive) {
        this.gamepad = gamepad;
        this.telemetry = telemetry;
        this.fieldCentricDrive = fieldCentricDrive;

        intakeMotor = hwMap.getIntakeMotor();
        outakeServoLeft = hwMap.getOuttakeServoLeft();
        outakeServoRight = hwMap.getOuttakeServoRight();
        colorSensorLeft = hwMap.getTrayLeftCS();
        colorSensorRight = hwMap.getTrayRightCS();

        slides = new Slides(hwMap, telemetry);
        arm = new Arm(hwMap, telemetry);
  //      transferController = new TransferController(arm, slides, telemetry);

        //Forces arm to init to intake pos when targetPos is initialized to outtakePos
        arm.goToIntake();

        this.telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());

        outakeServoLeft.setPosition(leftReleasePixelPos);
        outakeServoRight.setPosition(rightReleasePixelPos);

        telemetry.addData("INIT: ", "Cycle");
        telemetry.update();
    }


    public void loop() {
        while (true) {
            gamepad.readButtons();

            double buffer = 20;
            int height = 50;

            switch (state) {
                case start:
                    //Outtake Left
                    telemetry.addData("in start in cycle", 1);
                    if (gamepad.isDown(GamepadKeys.Button.LEFT_BUMPER)) {
                        telemetry.addData("Left-Bumper pressed in cycle", 1);
                        state = CycleFSM.outtakeLeft;
                    }

                    //Outtake Right
                    if (gamepad.isDown(GamepadKeys.Button.RIGHT_BUMPER)) {
                        telemetry.addData("Left-Bumper in cycle", 1);
                        state = CycleFSM.outtakeRight;
                    }

                    //Extend
                    if (gamepad.isDown(GamepadKeys.Button.Y)) {
                        telemetry.addData("y pressed in cycle", 1);
                        state = CycleFSM.extend;
                    }

                    //Retract
                    if (gamepad.isDown(GamepadKeys.Button.A)) {
                        telemetry.addData("b pressed in cycle", 1);
                        state = CycleFSM.retract;
                    }
                    break;

                case extend:
                  toTransfer = true;
    //                if(transferController.extend()){
      //                  state = CycleFSM.start;
        //            }
          //          break;

            //    case retract:
              //      toTransfer = true;
                //    if(transferController.retract()){
                  //      state = CycleFSM.start;
                    //    toTransfer = false;
                   // }
                    break;

                case outtakeLeft:
                    telemetry.addData("-", "Ready to deposit left");
                    outakeServoLeft.setPosition(leftReleasePixelPos);
                    toTransfer = true;
                    state = CycleFSM.start;
                    break;

                case outtakeRight:
                    telemetry.addData("-", "Ready to deposit right");
                    outakeServoRight.setPosition(rightReleasePixelPos);
                    toTransfer = true;
                    state = CycleFSM.start;
                    break;
            }

            //Intake is out here because DRIVE TEAM wants the intake to run regardless
            intake();

            fieldCentricDrive.drive(gamepad.getLeftX(), gamepad.getLeftY(), gamepad.getRightX(), HWMap.readFromIMU());
            slides.pid();
            arm.updatePos();
            telemetry.update();
        }
    }

    private void intake() {
        if (gamepad.wasJustPressed(GamepadKeys.Button.DPAD_DOWN))
            stopRequested = !stopRequested;

        if (!stopRequested) {
            double ejectSpeed = -0.4;
            double leftGripPixelPos = 0;
            double rightGripPixelPos = 1;
            double intakeSpeed = 1.0;


            if (!toTransfer) {
                detectPixels();
                if (pixelInLeft)
                    outakeServoLeft.setPosition(leftGripPixelPos);
                if (pixelInRight)
                    outakeServoRight.setPosition(rightGripPixelPos);
                if ((!pixelInLeft || !pixelInRight)) {
                    intakeMotor.set(intakeSpeed);
                    telemetry.addData("power: ", intakeSpeed);
                } else {
                    intakeMotor.set(ejectSpeed);
                    telemetry.addData("power: ", ejectSpeed);
                }
            } else {
                intakeMotor.set(ejectSpeed);
                telemetry.addData("power: ", ejectSpeed);
            }
        } else {
            intakeMotor.set(0);
        }


    }

    private void detectPixels() {
        double csLeftDistance = colorSensorLeft.getDistance(DistanceUnit.MM);
        double csRightDistance = colorSensorRight.getDistance(DistanceUnit.MM);
        final int distance = 28;
        final int greenThreshold = 300;
        if (csLeftDistance <= distance) {
            if (colorSensorLeft.green() > (colorSensorLeft.red() * 2)) {
                telemetry.addData("-", "Green pixel detected in the left compartment");
                pixelInLeft = true;
            } else if ((colorSensorLeft.blue() < colorSensorLeft.red()) && (colorSensorLeft.blue() < colorSensorLeft.green())) {
                telemetry.addData("-", "Yellow pixel detected in the left compartment");
                pixelInLeft = true;
            } else if ((colorSensorLeft.blue() > colorSensorLeft.red()) && (colorSensorLeft.blue() > colorSensorLeft.green())) {
                telemetry.addData("-", "Purple pixel detected in the left compartment");
                pixelInLeft = true;
            } else if (colorSensorLeft.green() >= greenThreshold) {
                telemetry.addData("-", "White pixel detected in the left compartment");
                pixelInLeft = true;
            }
        } else {
            telemetry.addData("-", "Nothing in the left compartment");
            pixelInLeft = false;
        }
        if (csRightDistance <= distance) {
            if (colorSensorRight.green() > (colorSensorRight.red() * 2)) {
                telemetry.addData("-", "Green pixel detected in the right compartment");
                pixelInRight = true;
            } else if ((colorSensorRight.blue() < colorSensorRight.red()) && (colorSensorRight.blue() < colorSensorRight.green())) {
                telemetry.addData("-", "Yellow pixel detected in the right compartment");
                pixelInRight = true;
            } else if ((colorSensorRight.blue() > colorSensorRight.red()) && (colorSensorRight.blue() > colorSensorRight.green())) {
                telemetry.addData("-", "Purple pixel detected in the right compartment");
                pixelInRight = true;
            } else if (colorSensorRight.green() >= greenThreshold) {
                telemetry.addData("-", "White pixel detected in the right compartment");
                pixelInRight = true;
            }
        } else {
            telemetry.addData("-", "Nothing in right compartment");
            pixelInRight = false;
        }
    }
}


