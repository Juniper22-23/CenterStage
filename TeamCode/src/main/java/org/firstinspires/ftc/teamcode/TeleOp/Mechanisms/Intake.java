package org.firstinspires.ftc.teamcode.TeleOp.Mechanisms;

import com.arcrobotics.ftclib.hardware.motors.Motor;
import com.qualcomm.hardware.rev.RevColorSensorV3;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

public class Intake {

    private static final double EJECT_SPEED = -0.4; // change all static?
    private static final double INTAKE_SPEED = 1.0;
    private final double POWER_EJECT = -0.8;

    private final Telemetry telemetry;

    private final Motor intakeMotor;
    private final RevColorSensorV3 trayLeftCS;
    private final RevColorSensorV3 trayRightCS;
    private static final int LEFT_DISTANCE = 28;
    private static final int RIGHT_DISTANCE = 30;
    private static final int GREEN_THRESHOLD = 300;

    private boolean pixelInLeft;
    private boolean pixelInRight;

    private double intakeVelocity;
    private boolean intakeJammed;
    private final double JAMMED_THRESHOLD = 1600; // check value - 1400 is half of max on test bench

    public Intake(HWMap hwMap, Telemetry telemetry) {
        this.telemetry = telemetry;
        intakeMotor = hwMap.getIntakeMotor();
        trayLeftCS = hwMap.getTrayLeftCS();
        trayRightCS = hwMap.getTrayRightCS();
        intakeJammed = false;

    }

    public void powerEject() {
        intakeMotor.set(POWER_EJECT);
    }

    public void intake() {
        intakeMotor.set(INTAKE_SPEED);
    }

    public void intake(int motorPower){
        intakeMotor.set(motorPower);
    }

    public void eject() {
        intakeMotor.set(EJECT_SPEED);
    }



    public void detectPixels() {
        double csLeftDistance = trayLeftCS.getDistance(DistanceUnit.MM);
        double csRightDistance = trayRightCS.getDistance(DistanceUnit.MM);

        int leftRed = trayLeftCS.red();
        int leftBlue = trayLeftCS.blue();
        int leftGreen = trayLeftCS.green();
        int rightRed = trayRightCS.red();
        int rightBlue = trayRightCS.blue();
        int rightGreen = trayRightCS.green();

        if (csLeftDistance <= LEFT_DISTANCE) {
            checkColor(leftRed, leftGreen, leftBlue);
            pixelInLeft = true;
        } else {
            telemetry.addData("-", "Nothing in the left compartment");
            pixelInLeft = false;
        }
        if (csRightDistance <= RIGHT_DISTANCE) {
            checkColor(rightRed, rightGreen, rightBlue);
            pixelInRight = true;
        } else {
            telemetry.addData("-", "Nothing in right compartment");
            pixelInRight = false;
        }

    }

    private void checkColor(int red, int green, int blue){
        if (green > (red * 2)) {
            telemetry.addData("-", "Green pixel detected in the right compartment");
        } else if ((blue < red) && (blue < green)) {
            telemetry.addData("-", "Yellow pixel detected in the right compartment");
        } else if ((blue > red) && (blue > green)) {
            telemetry.addData("-", "Purple pixel detected in the right compartment");
        } else if (green >= GREEN_THRESHOLD) {
            telemetry.addData("-", "White pixel detected in the right compartment");
        }
    }

    public boolean getPixelInLeft(){
        return pixelInLeft;
    }

    public boolean getPixelInRight(){
        return pixelInRight;
    }

    public double getIntakeVelocity() {
        intakeVelocity = intakeMotor.getCorrectedVelocity();
        return intakeVelocity;
    }

    public boolean intakeJammed() {
        intakeVelocity = intakeMotor.getCorrectedVelocity();
        telemetry.addData("in intake jammed method",1);
        return intakeVelocity <= JAMMED_THRESHOLD; // working

    }
}
