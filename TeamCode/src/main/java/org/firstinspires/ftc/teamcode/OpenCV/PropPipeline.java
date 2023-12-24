package org.firstinspires.ftc.teamcode.OpenCV;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.opencv.core.*;
import org.opencv.core.Core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.*;
import org.opencv.objdetect.*;

/**
 * GripPipeline class.
 *
 * <p>An OpenCV pipeline generated by GRIP.
 *
 * @author GRIP
 */
@Config
public class PropPipeline extends OpenCVPipeline {

    //Outputs
    private Mat rgbThresholdOutput = new Mat();
    private Mat cvErodeOutput = new Mat();
    private ArrayList<MatOfPoint> filterContoursOutput = new ArrayList<MatOfPoint>();
    private ArrayList<MatOfPoint> findContoursOutput = new ArrayList<>();
    private double sum_x;
    private double sum_y;
    public static double redMax = 25;
    public static double redMin = 0;
    public static double blueMax = 60;
    public static double blueMin = 0;
    public static double greenMax = 60;
    public static double greenMin = 0;
    public static double minAreaLeft = 1800;
    public static double minAreaCenter = 1700;

    /*
    ROBOT CONSTANTS:
    public static double redMax = 50;
    public static double redMin = 0;
    public static double blueMax = 150;
    public static double blueMin = 0;
    public static double greenMax = 150;
    public static double greenMin = 0;
    public static double minAreaLeft = 500;
    public static double minAreaCenter = 300;
     */
    private String position;
    private int findContourNum;
    private int filterContourNum;
    private double x;
    private double y;
    private double x_pos_split = 150;

    /**
     * This is the primary method that runs the entire pipeline and updates the outputs.
     */
    @Override
    public Mat processFrame(Mat input) {
        // Step RGB_Threshold0:
        Mat rgbThresholdInput = input;
        /*
        double[] rgbThresholdRed = {0, 255};
        double[] rgbThresholdGreen = {0, 255};
        double[] rgbThresholdBlue = {0, 255};

         */

        double[] rgbThresholdRed = {redMin, redMax};
        double[] rgbThresholdGreen = {blueMin, blueMax};
        double[] rgbThresholdBlue = {greenMin, greenMax};
        rgbThreshold(rgbThresholdInput, rgbThresholdRed, rgbThresholdGreen, rgbThresholdBlue, rgbThresholdOutput);

        // Step CV_erode0:
        Mat cvErodeSrc = rgbThresholdOutput;
        Mat cvErodeKernel = new Mat();
        Point cvErodeAnchor = new Point(-1, -1);
        double cvErodeIterations = 1.0;
        int cvErodeBordertype = Core.BORDER_CONSTANT;
        Scalar cvErodeBordervalue = new Scalar(-1);
        cvErode(cvErodeSrc, cvErodeKernel, cvErodeAnchor, cvErodeIterations, cvErodeBordertype, cvErodeBordervalue, cvErodeOutput);

        //Step Find_Contours
        Mat findContoursInput = cvErodeOutput;
        boolean findContoursExternalOnly = false;
        findContours(findContoursInput, findContoursExternalOnly, findContoursOutput);


        // Step Filter_Contours0:
        ArrayList<MatOfPoint> filterContoursContours = findContoursOutput;
        double filterContoursMinPerimeter = 0;
        double filterContoursMinWidth = 0;
        double filterContoursMaxWidth = 1000;
        double filterContoursMinHeight = 0.0;
        double filterContoursMaxHeight = 1000;
        double[] filterContoursSolidity = {0, 100};
        double filterContoursMaxVertices = 1000000;
        double filterContoursMinVertices = 0;
        double filterContoursMinRatio = 0;
        double filterContoursMaxRatio = 1000;
        /*
        filterContours(filterContoursContours, minAreaLeft, filterContoursMinPerimeter, filterContoursMinWidth, filterContoursMaxWidth, filterContoursMinHeight, filterContoursMaxHeight, filterContoursSolidity, filterContoursMaxVertices, filterContoursMinVertices, filterContoursMinRatio, filterContoursMaxRatio, filterContoursOutput);
        if(filterContourNum != 0){
            if(x >= x_pos_split){
                position = "CENTER";
            } else if(x < x_pos_split){
                position = "LEFT";
            }
        } else{
            position = "CENTER";
        }*/

        filterContours(filterContoursContours, minAreaCenter, filterContoursMinPerimeter, filterContoursMinWidth, filterContoursMaxWidth, filterContoursMinHeight, filterContoursMaxHeight, filterContoursSolidity, filterContoursMaxVertices, filterContoursMinVertices, filterContoursMinRatio, filterContoursMaxRatio, filterContoursOutput);
        if(filterContourNum == 0){
            filterContours(filterContoursContours, minAreaLeft, filterContoursMinPerimeter, filterContoursMinWidth, filterContoursMaxWidth, filterContoursMinHeight, filterContoursMaxHeight, filterContoursSolidity, filterContoursMaxVertices, filterContoursMinVertices, filterContoursMinRatio, filterContoursMaxRatio, filterContoursOutput);
            if(filterContourNum == 0){
                position = "RIGHT";
            } else{
                position = "CENTER";
            }
        } else{
            position = "LEFT";
        }

        return cvErodeOutput;

    }

    /**
     * This method is a generated getter for the output of a RGB_Threshold.
     * @return Mat output from RGB_Threshold.
     */
    public Mat rgbThresholdOutput() {
        return rgbThresholdOutput;
    }

    /**
     * This method is a generated getter for the output of a CV_erode.
     * @return Mat output from CV_erode.
     */
    public Mat cvErodeOutput() {
        return cvErodeOutput;
    }

    /**
     * This method is a generated getter for the output of a Filter_Contours.
     * @return ArrayList<MatOfPoint> output from Filter_Contours.
     */
    public ArrayList<MatOfPoint> filterContoursOutput() {
        return filterContoursOutput;
    }


    /**
     * Segment an image based on color ranges.
     * @param input The image on which to perform the RGB threshold.
     * @param red The min and max red.
     * @param green The min and max green.
     * @param blue The min and max blue.
     */
    private void rgbThreshold(Mat input, double[] red, double[] green, double[] blue,
                              Mat out) {
        Imgproc.cvtColor(input, out, Imgproc.COLOR_BGR2RGB);
        Core.inRange(out, new Scalar(red[0], green[0], blue[0]),
                new Scalar(red[1], green[1], blue[1]), out);
    }

    /**
     * Expands area of lower value in an image.
     * @param src the Image to erode.
     * @param kernel the kernel for erosion.
     * @param anchor the center of the kernel.
     * @param iterations the number of times to perform the erosion.
     * @param borderType pixel extrapolation method.
     * @param borderValue value to be used for a constant border.
     * @param dst Output Image.
     */
    private void cvErode(Mat src, Mat kernel, Point anchor, double iterations,
                         int borderType, Scalar borderValue, Mat dst) {
        if (kernel == null) {
            kernel = new Mat();
        }
        if (anchor == null) {
            anchor = new Point(-1,-1);
        }
        if (borderValue == null) {
            borderValue = new Scalar(-1);
        }
        Imgproc.erode(src, dst, kernel, anchor, (int)iterations, borderType, borderValue);
    }

    private void findContours(Mat input, boolean externalOnly,
                              List<MatOfPoint> contours) {
        Mat hierarchy = new Mat();
        contours.clear();
        int mode;
        if (externalOnly) {
            mode = Imgproc.RETR_EXTERNAL;
        }
        else {
            mode = Imgproc.RETR_LIST;
        }
        int method = Imgproc.CHAIN_APPROX_SIMPLE;
        Imgproc.findContours(input, contours, hierarchy, mode, method);
        findContourNum = findContoursOutput.size();
    }


    /**
     * Filters out contours that do not meet certain criteria.
     * @param inputContours is the input list of contours
     * @param output is the the output list of contours
     * @param minArea is the minimum area of a contour that will be kept
     * @param minPerimeter is the minimum perimeter of a contour that will be kept
     * @param minWidth minimum width of a contour
     * @param maxWidth maximum width
     * @param minHeight minimum height
     * @param maxHeight maximimum height
     * @param minVertexCount minimum vertex Count of the contours
     * @param maxVertexCount maximum vertex Count
     * @param minRatio minimum ratio of width to height
     * @param maxRatio maximum ratio of width to height
     */
    private void filterContours(List<MatOfPoint> inputContours, double minArea,
                                double minPerimeter, double minWidth, double maxWidth, double minHeight, double
                                        maxHeight, double[] solidity, double maxVertexCount, double minVertexCount, double
                                        minRatio, double maxRatio, List<MatOfPoint> output) {
        final MatOfInt hull = new MatOfInt();
        output.clear();
        System.out.println("INPUT CONTOURS:");
        System.out.println(inputContours.size());
        //operation
        for (int i = 0; i < inputContours.size(); i++) {
            final MatOfPoint contour = inputContours.get(i);
            final Rect bb = Imgproc.boundingRect(contour);
            if (bb.width < minWidth || bb.width > maxWidth) continue;
            if (bb.height < minHeight || bb.height > maxHeight) continue;
            final double area = Imgproc.contourArea(contour);
            if (area < minArea) continue;
            if (Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true) < minPerimeter) continue;
            Imgproc.convexHull(contour, hull);
            MatOfPoint mopHull = new MatOfPoint();
            mopHull.create((int) hull.size().height, 1, CvType.CV_32SC2);
            for (int j = 0; j < hull.size().height; j++) {
                int index = (int)hull.get(j, 0)[0];
                double[] point = new double[] { contour.get(index, 0)[0], contour.get(index, 0)[1]};
                mopHull.put(j, 0, point);
            }
            final double solid = 100 * area / Imgproc.contourArea(mopHull);
            if (solid < solidity[0] || solid > solidity[1]) continue;
            if (contour.rows() < minVertexCount || contour.rows() > maxVertexCount)	continue;
            final double ratio = bb.width / (double)bb.height;
            if (ratio < minRatio || ratio > maxRatio) continue;
            output.add(contour);
            System.out.println("X AND Y:");
            System.out.println(contour.toArray()[0].x);
            System.out.println(contour.toArray()[0].y);
            x = contour.toArray()[0].x;
            y = contour.toArray()[0].y;
        }
        System.out.println("OUTPUT CONTOURS:");
        System.out.println(output.size());
        filterContourNum = output.size();
    }

    public double getX(){
        return x;
    }
    public double getY(){
        return y;
    }
    public void resetSums(){
        sum_x = 0;
        sum_y = 0;
    }
    public int getFindContourNum(){
        return findContourNum;
    }
    public int getFilterContourNum(){
        return filterContourNum;
    }
    public String getPosition(){
        return position;
    }
}

