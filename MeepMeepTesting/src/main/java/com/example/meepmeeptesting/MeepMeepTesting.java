package com.example.meepmeeptesting;

import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.geometry.Vector2d;
import com.noahbres.meepmeep.MeepMeep;
import com.noahbres.meepmeep.core.colorscheme.scheme.ColorSchemeBlueDark;
import com.noahbres.meepmeep.roadrunner.DefaultBotBuilder;
import com.noahbres.meepmeep.roadrunner.entity.RoadRunnerBotEntity;

public class MeepMeepTesting {
    public static void main(String[] args) {
        MeepMeep meepMeep = new MeepMeep(800);

        double forwardDistance = 27;
        double turnAngleSpike = -75;

        RoadRunnerBotEntity myBot = new DefaultBotBuilder(meepMeep)
                // Set bot constraints: maxVel, maxAccel, maxAngVel, maxAngAccel, track width
                .setConstraints(45, 30, Math.toRadians(180), Math.toRadians(180), 14)
                .followTrajectorySequence(drive ->
                        drive.trajectorySequenceBuilder(new Pose2d(-36, 62, Math.toRadians(270)))
/*                                .UNSTABLE_addTemporalMarkerOffset(0, () -> {
                                    while (!transferController.extend("BACKDROP")) {
                                        slides.pid(true);
                                        arm.updatePos();
                                    }
                                })*/
                                .setTangent(Math.toRadians(270))
                                .splineToSplineHeading(new Pose2d(-35, 40, Math.toRadians(330)), Math.toRadians(270))
                                .waitSeconds(0.5)
                                .splineToLinearHeading(new Pose2d(-60, 24, Math.toRadians(0)), Math.toRadians(180))
                                .waitSeconds(2)
                                .setTangent(Math.toRadians(0))
                                .splineToConstantHeading(new Vector2d(-36,12), Math.toRadians(0))
                                .splineToConstantHeading(new Vector2d(24, 12), Math.toRadians(0))
                                .splineToConstantHeading(new Vector2d(50, 41), Math.toRadians(0))
                                .build()
                );

        meepMeep.setBackground(MeepMeep.Background.FIELD_CENTERSTAGE_JUICE_DARK)
                .setDarkMode(true)
                .setBackgroundAlpha(0.95f)
                .addEntity(myBot)
                .start();
    }
}