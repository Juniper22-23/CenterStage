package org.firstinspires.ftc.teamcode.OpenCV;

import android.util.Size;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.hardware.DcMotorEx;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.VisionProcessor;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

@Autonomous
public class DropoffTest extends LinearOpMode {
    private AprilTagProcessor tagProcessor;
    private VisionPortal visionPortal;
    private DcMotorEx leftFrontMotor;
    private DcMotorEx rightFrontMotor;
    private DcMotorEx rightBackMotor;
    private DcMotorEx leftBackMotor;
    private HWMap hwMap;

    @Override
    public void runOpMode() throws InterruptedException {
        hwMap = new HWMap(telemetry, hardwareMap);

        leftFrontMotor = hwMap.leftFrontMotor;
        rightFrontMotor = hwMap.rightFrontMotor;
        rightBackMotor = hwMap.rightBackMotor;
        leftBackMotor = hwMap.leftBackMotor;

        double range_t = 20.0;
        double bearing_t = 20.0;
        double y_t = 20.0;
        double yaw_t = 20.0;

        tagProcessor = new AprilTagProcessor.Builder()
                .setDrawAxes(true)
                .setDrawCubeProjection(true)
                .setDrawTagID(true)
                .setDrawTagOutline(true)
                .build();

        visionPortal = new VisionPortal.Builder()
                .addProcessor(tagProcessor)
                .setCamera(hardwareMap.get(WebcamName.class, "Webcam 1"))
                .setCameraResolution(new Size(640, 480))
                .build();

        waitForStart();

        /*
        4 things to test:
        1. CV Drive - driving towards AprilTag using CV nav
            - CV Values: Range, y
        2. Distance Sensor Drive - driving towards AprilTag using DS nav
        3. CV Angular Correction - correcting angle based on CV nav
            - CV Values: Bearing, Yaw
        4. Distance Sensor Angular Correction - correcting angle based on DS nav

         */
        drive(0.5);
        while (opModeIsActive()) {
            if (tagProcessor.getDetections().size() > 0) {
                AprilTagDetection tag = tagProcessor.getDetections().get(0);
                drive(0.0);
                tele(tag);
            }
            /*
            if (tagProcessor.getDetections().get(0).ftcPose.range < range_t) {
                AprilTagDetection tag = tagProcessor.getDetections().get(0);
                drive(0.0);
                tele(tag);
            }
             */
        }
    }

    public void drive(double power){
        leftFrontMotor.setPower(power);
        rightFrontMotor.setPower(power);
        rightBackMotor.setPower(power);
        leftBackMotor.setPower(power);
    }

    public void tele(AprilTagDetection tag){
        while(true) {
            if(tagProcessor.getDetections().size() > 0) {
                telemetry.addData("-", tag.id);
                telemetry.addData("x", tag.ftcPose.x);
                telemetry.addData("y", tag.ftcPose.y);
                telemetry.addData("z", tag.ftcPose.z);
                telemetry.addData("roll", tag.ftcPose.roll);
                telemetry.addData("pitch", tag.ftcPose.pitch);
                telemetry.addData("yaw", tag.ftcPose.yaw);
                telemetry.addData("range", tag.ftcPose.range);
                telemetry.addData("bearing", tag.ftcPose.bearing);
                telemetry.addData("elevation", tag.ftcPose.elevation);
                telemetry.addData("-", "outside");
            } else{
                telemetry.clearAll();
            }
            telemetry.update();
        }
    }

    public void alignHeading() {
        final double leftDistance = hwMap.distanceSensorLeft.getDistance(DistanceUnit.INCH);
        final double rightDistance = hwMap.distanceSensorRight.getDistance(DistanceUnit.INCH);

        final double ROBOT_WIDTH = 16.0; // Inches

        final double rawCorrectionFactor = Math.atan2(leftDistance - rightDistance, ROBOT_WIDTH);
        final double correctionFactor = mapValue(rawCorrectionFactor, -Math.PI / 2.0, Math.PI / 2.0, -1.0, 1.0);
        final double kp = 1.0;

        final double y = 1.0;
        final double x = 0.0;
        final double rx = kp * correctionFactor;

        final double denominator = Math.max(Math.abs(y) + Math.abs(x) + Math.abs(rx), 1);

        final double frontLeftPower = (y + x + rx) / denominator;
        final double backLeftPower = (y - x + rx) / denominator;
        final double frontRightPower = (y - x - rx) / denominator;
        final double backRightPower = (y + x - rx) / denominator;

        hwMap.leftFrontMotor.setPower(frontLeftPower);
        hwMap.leftBackMotor.setPower(backLeftPower);
        hwMap.rightFrontMotor.setPower(frontRightPower);
        hwMap.rightBackMotor.setPower(backRightPower);
    }

    public double mapValue(double input, double inputStart, double inputEnd, double outputStart, double outputEnd) {
        return outputStart + (outputEnd - outputStart) / (inputEnd - inputStart) * (input - inputStart);
    }
}