//EVERYTHING WORKS
package org.firstinspires.ftc.teamcode.Core;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.Core.HWMap;
import org.firstinspires.ftc.teamcode.Core.HWMapFTCLib;
import org.firstinspires.ftc.teamcode.Core.FieldCentricFTCLib;

@TeleOp(name = "Tele-op")
public class Teleop extends LinearOpMode {

    private Controller controller;
    private FieldCentricFTCLib fieldCentricDrive;
    private HWMap hwMap;


    public void runOpMode() {
        telemetry.clear();

        try {
            controller = new Controller(gamepad1);
            hwMap = new HWMap(telemetry, hardwareMap);
            hwMap.getLeftServo().setPosition(0.7);
            hwMap.getRightServo().setPosition(0.0);
            hwMap.getForwardServo().setPosition(0.5);
            fieldCentricDrive = new FieldCentricFTCLib(telemetry, hardwareMap, hwMap);
        } catch (Exception exception) {
            telemetry.addLine("Outside of the while loop:");
            telemetry.addLine(exception.getMessage());
            telemetry.update();
        }

        telemetry.update();
        waitForStart();
        while (opModeIsActive()) {
            controller.readButtons();


            //FIELD-CENTERIC_______________________________________________________________________________
            double gamepadX;
            double gamepadY;
            double gamepadRot;

            if (Math.abs(controller.gamepadX) > 0.01) {
                gamepadX = controller.gamepadX;
            } else {
                gamepadX = 0;
            }
            if (Math.abs(controller.gamepadY) > 0.01) {
                gamepadY = controller.gamepadY;
            } else {
                gamepadY = 0;
            }
            if (Math.abs(controller.gamepadRot) > 0.01) {
                gamepadRot = -controller.gamepadRot;
            } else {
                gamepadRot = 0;
            }

            fieldCentricDrive.drive(gamepadX, gamepadY, gamepadRot, hwMap.readFromIMU());

        }
    }
}