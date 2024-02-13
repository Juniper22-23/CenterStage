package org.firstinspires.ftc.teamcode.Auto;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.acmerobotics.roadrunner.geometry.Vector2d;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.ExposureControl;
import org.firstinspires.ftc.robotcore.external.hardware.camera.controls.GainControl;
import org.firstinspires.ftc.teamcode.Auto.RoadRunner.drive.SampleMecanumDrive;
import org.firstinspires.ftc.teamcode.Auto.RoadRunner.trajectorysequence.TrajectorySequence;
import org.firstinspires.ftc.teamcode.TeleOp.Mechanisms.Axons.Arm;
import org.firstinspires.ftc.teamcode.TeleOp.Mechanisms.FieldCentricDrive;
import org.firstinspires.ftc.teamcode.TeleOp.Mechanisms.Gripper;
import org.firstinspires.ftc.teamcode.TeleOp.Mechanisms.HWMap;
import org.firstinspires.ftc.teamcode.TeleOp.Mechanisms.Odometry;
import org.firstinspires.ftc.teamcode.TeleOp.Mechanisms.Slides;
import org.firstinspires.ftc.teamcode.TeleOp.Mechanisms.TransferController;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagPoseFtc;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

import java.util.concurrent.TimeUnit;


@Autonomous(name = "Blue Left 4:15PM")
@Config
public class Blue_Left extends LinearOpMode {

    /**
     * Auto Constant Variables:
     **/
    public static double startX = 12.0; // Start pos X
    public static double startY = 65.0; // Start pos Y
    public static double startHeading = Math.toRadians(270);

    /**
     * Robot Tuning Variables
     **/
    public static double startXOff = 0.0; // Start pos X offset
    public static double startYOff = 0.0; // Start pos Y offset

    //TODO: Field Tuning Variables:
    public static double autoDelay = 0.0; //Delay before auto starts

    //Declare Mechanisms
    public static SampleMecanumDrive drive;
    public static TransferController transferController;
    public static Arm arm;
    public static Slides slides;
    public static Gripper gripper;
    private FieldCentricDrive fieldCentricDrive;
    private Detector detector;
    private Odometry odometry;

    //Variables for spike mark
    private String propPosition;
    public static double dropPosition;
    public static double dropPositionCompensationX;
    public static double dropPositionCompensationY;
    public static double turnAngleSpike;
    public static double aprilTagReadingPosition;
    public static double aprilTagAngleCompensation;
    public static double backDistance;
    public static double leftCompensation;
    private final int camOffset = 5; //Tune
    // xCorrection, yCorrection, and yawCorrection are relative to the camera CV, not RoadRunner
    private double xCorrection;
    private double yCorrection;
    private double yawCorrection;
    private final double CV_TARGET_Y_DISTANCE = 4.0;
    private final double CV_CAMERA_TO_BACKDROP_DIST = 17.0;

    // For every 100% error in pose, there is 100% motor power;
    private final double CV_CORRECTION_SPEED = 1.0;

    private final double CV_TRANSLATION_WEIGHT = 0.5;
    private final double CV_HEADING_WEIGHT = 10.0;

    private final double CV_VERTICAL_TO_BACKDROP_TIME = 4.0;
    double lastRange = 0.0;
    private CVRelocalizer cvRelocalizer;

    private final PowerVector ZERO_VECTOR = new PowerVector(0.0, 0.0, 0.0, 0.0);

    private PowerVector MAX_VECTOR = new PowerVector(1.0, 1.0, 1.0, 1.0);
    private PowerVector FORWARD_VECTOR = new PowerVector(1.0, 1.0, 1.0, 1.0);
    private PowerVector TURN_VECTOR = new PowerVector(-1.0, -1.0, 1.0, 1.0);

    private int id;
    private static final double P = 0.035, I = 0, D = 0;
    AprilTagProcessor tagProcessor;
    VisionPortal visionPortal;

    @Override
    public void runOpMode() {

        //Initialize Mechanisms
        HWMap hwMap = new HWMap(hardwareMap);
        drive = new SampleMecanumDrive(hardwareMap);
        arm = new Arm(hwMap, telemetry);
        slides = new Slides(hwMap, telemetry);
        transferController = new TransferController(arm, slides);
        gripper = new Gripper(hwMap);
        //detector = new Detector(hardwareMap, telemetry);
        fieldCentricDrive = new FieldCentricDrive(hwMap, telemetry);
        odometry = new Odometry(hwMap);
        //cvRelocalizer = new CVRelocalizer(hardwareMap);
        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
        //CODE STARTS HERE
        propPosition = "CENTER";
        gripper.gripLeft();
        gripper.gripRight();
        odometry.extendOdo();
        sleep(2000);


        tagProcessor = AprilTagProcessor.easyCreateWithDefaults();
        visionPortal = VisionPortal.easyCreateWithDefaults(hardwareMap.get(WebcamName.class, "Webcam 1"), tagProcessor);

        // Exposure stuffs
        // Wait for the camera to be open
        if (visionPortal.getCameraState() != VisionPortal.CameraState.STREAMING) {
            telemetry.addData("Camera", "Waiting");
            telemetry.update();
            while (!isStopRequested() && (visionPortal.getCameraState() != VisionPortal.CameraState.STREAMING)) {
                sleep(20);
            }
            telemetry.addData("Camera", "Ready");
            telemetry.update();
        }
        ExposureControl exposureControl = visionPortal.getCameraControl(ExposureControl.class);
        if (exposureControl.getMode() != ExposureControl.Mode.Manual) {
            exposureControl.setMode(ExposureControl.Mode.Manual);
            sleep(50);
        }
        exposureControl.setExposure((long)1, TimeUnit.MILLISECONDS);
        sleep(20);

        GainControl gainControl = visionPortal.getCameraControl(GainControl.class);
        gainControl.setGain(255);
        sleep(20);

        while (!isStarted() && !isStopRequested()) {
            /*
            detector.detect();
            telemetry.addData("-", "INIT DONE");
            telemetry.addData("POSITION: ", detector.getPosition());
            telemetry.addData("FILTER CONTOUR NUM: ", detector.getFilterContourNum());
            telemetry.addData("x", detector.getX());
            telemetry.addData("y", detector.getY());
            telemetry.addData("contour areas: ", detector.getContourAreas());
            telemetry.update();
             */
        }

        //propPosition = detector.getPosition();
        if (propPosition == "CENTER") {
            dropPosition = 36.5;
            dropPositionCompensationX = 0.001;
            dropPositionCompensationY = 3;
            turnAngleSpike = 0;
            aprilTagReadingPosition = 20 - camOffset;
            backDistance = 3;
            id = 2;

        } else if (propPosition == "LEFT") {
            dropPosition = 40;
            dropPositionCompensationX = 1;
            dropPositionCompensationY = 2;
            turnAngleSpike = 60;
            aprilTagReadingPosition = 14 - camOffset;
            leftCompensation = 1;
            id = 1;
        } else {
            dropPosition = 40;
            dropPositionCompensationX = -1;
            dropPositionCompensationY = 2;
            turnAngleSpike = -75;
            aprilTagReadingPosition = 26 - camOffset;
            id = 3;
        }

        startX += startXOff;
        startY += startYOff;
        drive.setPoseEstimate(new Pose2d(startX, startY, startHeading));
        drive.setExternalHeading(startHeading);
        TrajectorySequence trajectory = drive.trajectorySequenceBuilder(new Pose2d(startX, startY, startHeading))

                //Extension for Spike Mark Delivery
                .UNSTABLE_addTemporalMarkerOffset(0, () -> {
                    while (!transferController.extend("SPIKE")) {
                        slides.pid(true);
                        arm.updatePos();
                    }
                })
                .waitSeconds(1)

                //Approach to Spike Mark
                .lineToConstantHeading(new Vector2d(startX+1, dropPosition))

                //Spike Mark Compensation and Delivery
                .lineToLinearHeading(new Pose2d(startX + dropPositionCompensationX, dropPosition + dropPositionCompensationY, startHeading + Math.toRadians(turnAngleSpike)))
                .UNSTABLE_addTemporalMarkerOffset(0, () -> {
                    gripper.releaseLeft();
                })
                .waitSeconds(0.5)
                .UNSTABLE_addTemporalMarkerOffset(0, () -> {
                    while (!transferController.retract()) {
                        slides.pid(true);
                        arm.updatePos();
                    }
                })
                .waitSeconds(2)
                .lineToLinearHeading(new Pose2d(startX+1, dropPosition+backDistance, startHeading))

                //Reset to Original Position
                .lineToConstantHeading(new Vector2d(startX+leftCompensation, 60))

                // Align with backdrop horizontally
                .turn(Math.toRadians(90))
                .lineToConstantHeading(new Vector2d(startX + 28, 60))
                .strafeRight(aprilTagReadingPosition)
                .waitSeconds(2)

                // Extend Arm
                .UNSTABLE_addTemporalMarkerOffset(0, () ->{
                    gripper.gripRight();
                    telemetry.addLine("Gripping right");
                    telemetry.update();
                })
                .waitSeconds(1)
                .UNSTABLE_addTemporalMarkerOffset(0, () ->{
                    while (!transferController.extend("BACKDROP")) {
                        slides.pid(true);
                        arm.updatePos();
                        telemetry.addLine("Extending arm");
                        telemetry.update();
                    }
                })
                .waitSeconds(1)

                .UNSTABLE_addTemporalMarkerOffset(0, () -> {
                    ElapsedTime timer = new ElapsedTime();
                    timer.reset();

                    while (timer.time() < CV_VERTICAL_TO_BACKDROP_TIME) {
                        telemetry.addLine("Running CV Heading Correction");
                        telemetry.addData("ID: ", id);
                        AprilTagPoseFtc ftcPose = getFtcPose(id);

                        if (ftcPose == null) {
                            telemetry.addLine("April Tag was not detected.");
                            telemetry.update();
                            setMotorPowersDestructured(new PowerVector(0, 0, 0, 0));
                            continue;
                        }

                        PowerVector motorPowers = ZERO_VECTOR
                                .add(headingAlignment(ftcPose).scale(CV_HEADING_WEIGHT * (1.0 - (timer.time() / CV_VERTICAL_TO_BACKDROP_TIME))))
                                .limit(CV_CORRECTION_SPEED);

                        telemetry.addData("timer.time()", timer.time());
                        telemetry.addData("lastRange", lastRange);
                        telemetry.addData("motorPowers", motorPowers);

                        setMotorPowersDestructured(motorPowers);
                        telemetry.update();
                    }
                })
                .waitSeconds(1)

                // Align with backdrop vertically with CV
                .UNSTABLE_addTemporalMarkerOffset(0, () -> {
                    ElapsedTime timer = new ElapsedTime();
                    timer.reset();

                    while (timer.time() < CV_VERTICAL_TO_BACKDROP_TIME) {
                        telemetry.addLine("Running CV Vertical Correction");
                        telemetry.addData("ID: ", id);
                        AprilTagPoseFtc ftcPose = getFtcPose(id);

                        if (ftcPose == null) {
                            telemetry.addLine("April Tag was not detected.");
                            telemetry.update();
                            setMotorPowersDestructured(new PowerVector(0, 0, 0, 0));
                            continue;
                        }

                        PowerVector motorPowers = ZERO_VECTOR
                                .add(headingAlignment(ftcPose).scale(CV_HEADING_WEIGHT / 5 * (1.0 - (timer.time() / CV_VERTICAL_TO_BACKDROP_TIME))))
                                .add(verticalAlignment(ftcPose).scale(CV_TRANSLATION_WEIGHT))
                                .limit(CV_CORRECTION_SPEED);

                        telemetry.addData("timer.time()", timer.time());
                        telemetry.addData("lastRange", lastRange);
                        telemetry.addData("motorPowers", motorPowers);

                        setMotorPowersDestructured(motorPowers);
                        telemetry.update();
                    }
                })
                .waitSeconds(1)
                .UNSTABLE_addTemporalMarkerOffset(0, () ->{
                    gripper.releaseRight();
                    telemetry.addData("lastRange", lastRange);
                    telemetry.addLine("Releasing right motor");
                    telemetry.update();
                })
                .waitSeconds(1)

                /*//Reset for TeleOp
                .UNSTABLE_addTemporalMarkerOffset(0, () ->{
                    while (!transferController.retract()) {
                        slides.pid(true);
                        arm.updatePos();
                    }
                })
                .UNSTABLE_addTemporalMarkerOffset(0, ()->{
                    odometry.retractOdo();
                })
                .waitSeconds(1)*/
                .build();
        drive.followTrajectorySequenceAsync(trajectory);

        while (opModeIsActive()) {
//            telemetry.addData("IMU", HWMap.readFromIMU());
//            telemetry.addData("April Tag Detects: ", tagProcessor.getDetections().size());
//            telemetry.update();
            drive.update();
            slides.pid(true);
            arm.updatePos();
        }
    }

    private void setMotorPowersDestructured(PowerVector vector)  {
        drive.setMotorPowers(
                vector.getLeftFrontPower(),
                vector.getLeftBackPower(),
                vector.getRightBackPower(),
                vector.getRightFrontPower()
        );
    }

    private PowerVector verticalAlignment(AprilTagPoseFtc ftcPose) {
        lastRange = ftcPose.range;
        telemetry.addData("ftcPose.range", ftcPose.range);

        // Uses percent error formula
        double yError = (CV_TARGET_Y_DISTANCE - ftcPose.range) / (CV_CAMERA_TO_BACKDROP_DIST - CV_TARGET_Y_DISTANCE);
        telemetry.addData("yError", yError);

        return FORWARD_VECTOR.scale(-yError).limit(1.0);
    }

    private PowerVector headingAlignment(AprilTagPoseFtc ftcPose) {
        double headingError = ftcPose.yaw / 180.0;
        telemetry.addData("headingError", headingError);

        return TURN_VECTOR.scale(-headingError).limit(1.0);
    }

    public AprilTagPoseFtc getFtcPose(int id) {
        AprilTagDetection aprilTag;
        for(AprilTagDetection tag : tagProcessor.getDetections()){
            if(tag.id == id){
                aprilTag = tag;
                return aprilTag.ftcPose;
            }
        }
        return null;
    }
}