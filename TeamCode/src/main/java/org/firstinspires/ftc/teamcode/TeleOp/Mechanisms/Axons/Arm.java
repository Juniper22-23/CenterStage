package org.firstinspires.ftc.teamcode.TeleOp.Mechanisms.Axons;

import com.acmerobotics.dashboard.config.Config;
import com.arcrobotics.ftclib.controller.PIDController;
import com.arcrobotics.ftclib.hardware.motors.CRServo;
import com.qualcomm.robotcore.hardware.AnalogInput;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.TeleOp.Mechanisms.HWMap;

@Config
public class Arm {
    // Basic PID coefficients
    // a is a feedforward coefficient used to counteract the torque applied to the arm by gravity
    // a * sin(θ) gives the power needed to counteract gravity, θ is distance from vertically pointing down
    // a is used in place of m * g * r as those remain constant, and its easier to tune a single coefficient


    //ROBOT CONSTANTS:
    private static final double P = 0.00518;
    private static final double I = 0.003;
    private static final double D = 0.015;
    private static final double A = 0.07;

/*
    //TEST BENCH CONSTANTS:
    private static final double P = 0.0047;
    private static final double I = 0;
    private static final double D = 0;
    private static final double A = 0;
*/

    protected final AxonClass leftAxon;
    protected final AxonClass rightAxon;
    protected final PIDController pidController;
    protected final CRServo leftServo;
    protected final CRServo rightServo;
    protected final AnalogInput leftEncoder;
    protected final AnalogInput rightEncoder;
    private final Telemetry telemetry;

    public Arm(HWMap hwMap, Telemetry telemetry) {
        this.telemetry = telemetry;
        leftServo = hwMap.getAxonServoLeft();
        rightServo = hwMap.getAxonServoRight();
        leftEncoder = hwMap.getAxonAnalogLeft();
        rightEncoder = hwMap.getAxonAnalogRight();
        leftAxon = new AxonClass(leftServo, leftEncoder, true, true);
        rightAxon = new AxonClass(rightServo, rightEncoder, false, false);
        pidController = new PIDController(P, I, D);
    }

    private static final double INTAKE_POS = 115; // Angle for Intaking pixels
    private final double depositPos = normalizeRadiansTau(INTAKE_POS + 150); // Angle for depositing pixels, is 150 degrees from intake
    private final double SPIKE_POS = 220;
    private static double INTAKE_OFFSET = 60; // Degrees that the intake position is from vertically facing down
    private static final double safeError = 10; // Position can be +- this many degrees from target for safe transfer
    private final double safeIntake = normalizeRadiansTau(INTAKE_POS - safeError); // Safe range to start transfer from intake pos
    private final double safeRange = 150 + (2 * safeError); // the range of the safe values from safeIntake, will end at depositPos + safeError

    private double targetPos = INTAKE_POS;
    private double measuredPos = 0;

    //Temp Vars for testing

    // Finds the smallest distance between 2 angles, input and output in degrees
    protected double angleDelta(double angle1, double angle2) {
        return Math.min(normalizeRadiansTau(angle1 - angle2), 360 - normalizeRadiansTau(angle1 - angle2));
    }

    // Finds the direction of the smallest distance between 2 angles
    protected double angleDeltaSign(double position, double target) {
        return -(Math.signum(normalizeRadiansTau(target - position) - (360 - normalizeRadiansTau(target - position))));
    }

    // Converts angle from degrees to radians
    protected double toRadians(double degrees) {
        return degrees * Math.PI / 180;
    }

    // Takes input angle in degrees, returns that angle in the range of 0-360
    protected double normalizeRadiansTau(double angle) {
        return (angle + 360) % 360;
    }

    public void goToDeposit(String typeOfDeposit) {
        if(typeOfDeposit == "SPIKE"){
            targetPos = SPIKE_POS;
        }else {
            targetPos = depositPos;
        }
    }

    public void goToIntake() {
        targetPos = INTAKE_POS;
    }

    public void goToSpikePos(){ targetPos = SPIKE_POS;}

    public void updatePos() {
        measuredPos = leftAxon.getPos();

        //This is the error between measured position and target position.
        double delta = angleDelta(measuredPos, targetPos);
        double sign = angleDeltaSign(measuredPos, targetPos);
        // Distance between measured and target position * the sign of that distance
        double error = delta * sign;

        // We use zero here because we calculate the error and its direction for the PID loop
        double power = pidController.calculate(0, error);

        // Feedforward using a
        double degreesFromVert = angleDelta(measuredPos, INTAKE_POS) * angleDeltaSign(measuredPos, INTAKE_POS) + INTAKE_OFFSET; // Degrees the arm is away from vertically straight down
        double feedForward = -A * Math.sin(toRadians(degreesFromVert)); // Calculating power
        power += feedForward; // Adding feedforward to power

        //Applying the sign to the power
        power = Math.abs(power) * Math.signum(power);

        // Setting servo powers, one servo should have a true value for inverse when its created so we can set positive powers to both
        leftAxon.setPower(power);
        rightAxon.setPower(power);

        // Telemetry
        /*telemetry.addData("Measured Pos: ", measuredPos);
        telemetry.addData("Target Pos: ", targetPos);
        telemetry.addData("Delta: ", delta);
        telemetry.addData("Sign: ", sign);*/
    }

    //This method returns TRUE if the axons are within the buffered range of the target position or it will return FALSE.
    public boolean axonAtPos(double targetPos, double buffer) {
        return (((targetPos + buffer) >= measuredPos) && ((targetPos - buffer) <= measuredPos));
    }

    public double getIntakePos() {
        return INTAKE_POS;
    }
    public double getDepositPos(String typeOfDeposit) {
        if(typeOfDeposit == "SPIKE") {
            return SPIKE_POS;
        } else {
            return depositPos;
        }
    }
}