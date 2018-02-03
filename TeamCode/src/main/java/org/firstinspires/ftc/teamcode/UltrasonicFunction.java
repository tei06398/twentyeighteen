package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.UltrasonicSensor;

/**
 * This class contains utilities that read from ultrasonic sensors.
 */
public class UltrasonicFunction {
    private SmoothUltrasonic ultrasonicLeft;
    private SmoothUltrasonic ultrasonicRight;
    private SmoothUltrasonic ultrasonicRF;
    private SmoothUltrasonic ultrasonicLF;

    private RobotLog log;

    public UltrasonicFunction (HardwareMap hardwareMap, RobotLog log) {
        this.log = log;

        this.ultrasonicLeft = new SmoothUltrasonic(hardwareMap.ultrasonicSensor.get("ultrasonicLeft"), log.child("L")); //module 2, port 1
        this.ultrasonicRight = new SmoothUltrasonic(hardwareMap.ultrasonicSensor.get("ultrasonicRight"), log.child("R"));//module 2, port 2
        this.ultrasonicLF = new SmoothUltrasonic(hardwareMap.ultrasonicSensor.get("ultrasonicLF"), log.child("LF")); //module 3, port 3
        this.ultrasonicRF = new SmoothUltrasonic(hardwareMap.ultrasonicSensor.get("ultrasonicRF"), log.child("RF")); //module 4, port 4

        hardwareMap.legacyModule.get("Legacy Module 2").enable9v(4, true);
        hardwareMap.legacyModule.get("Legacy Module 2").enable9v(5, true);
    }

    public double getLeft() {
        return ultrasonicLeft.getDistance();
    }

    public double getRight() {
        return ultrasonicRight.getDistance();
    }
    public double getLF() {
        return ultrasonicLF.getDistance();
    }

    public double getRF() {
        return ultrasonicRF.getDistance();
    }

    public void setLeft(double input) {
        ultrasonicLeft.setDistance(input);
    }

    public void setLF(double input) {
        ultrasonicLF.setDistance(input);
    }

    public void setRF(double input) {
        ultrasonicRF.setDistance(input);
    }

    public void setRight(double input) {
        ultrasonicRight.setDistance(input);
    }

    /**
     * Adds utilities to an UltrasonicSensor.
     */
    public static class SmoothUltrasonic {
        private UltrasonicSensor ultrasonicSensor;
        private double distance;
        private NumberLog rawLog;
        private NumberLog readingLog;
        private NumberLog successLog;

        public SmoothUltrasonic (UltrasonicSensor ultrasonicSensor, RobotLog log) {
            this.ultrasonicSensor = ultrasonicSensor;

            rawLog = log.child("raw").getNumberLog();
            readingLog = log.child("readingLog").getNumberLog();
            successLog = log.child("successLog").getNumberLog();

            distance = 255;
            getDistance();
        }

        public double getDistance() {
            double outputValue;
            double sum = 0;
            double successes = 0;
            for (int attempt = 0; attempt < 20; attempt++) {
                outputValue = ultrasonicSensor.getUltrasonicLevel();
                rawLog.addItem(outputValue);
                if (outputValue != 0 && outputValue != 255 && outputValue != 127) {
                    sum += outputValue;
                    successes++;
                }
            }
            successLog.addItem(successes);
            if (successes > 0) {
                distance = sum/successes;
            }
            readingLog.addItem(distance);
            return distance;
        }

        public void setDistance (double input) {
            distance = input;
        }
    }
    
    public void test() {
        printTestData();
        log.getTelemetry().update();
    }

    public void printTestData() {
        log.getTelemetry().addData("Ultrasonic LEFT", getLeft());
        log.getTelemetry().addData("Ultrasonic RIGHT", getRight());
        log.getTelemetry().addData("Ultrasonic LEFT_FRONT", getLF());
        log.getTelemetry().addData("Ultrasonic RIGHT_FRONT", getRF());
    }
}