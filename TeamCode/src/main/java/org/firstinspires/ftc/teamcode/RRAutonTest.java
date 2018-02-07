package org.firstinspires.ftc.teamcode;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.*;

import com.vuforia.PIXEL_FORMAT;
import com.vuforia.Vuforia;
import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.navigation.*;

/**
 * The official autonomous mode.
 */
@Autonomous(name = "RR Test Auton Mode")
public class RRAutonTest extends LinearOpMode {
    protected ColorSensor jewelTipper;

    protected RobotDriving robotDriving;
    protected RobotDriving.Steering steering;

    protected UltrasonicFunction ultrasonicFunction;
    protected GunnerFunction gunnerFunction;
    protected RobotLog log;
    //protected OpticalDistanceSensor leftEOPD;
    //protected OpticalDistanceSensor rightEOPD;

    SharedPreferences sharedPref;
    protected String startPosition; // RED_RELIC, RED_MIDDLE, BLUE_RELIC, BLUE_MIDDLE

    //Required distance from wall at the beginning
    /*protected int wallDistance = 30;
    protected double clockwiseTurnWeight = 0;
    protected double forwardWeight = 0;*/

    protected final double MOVE_SPEED_RATIO = 0.3;
    protected final double TURN_SPEED_RATIO = 0.15;
    protected final int JEWELPUSHER_TIME = 3300;
    protected final int JEWEL_PUSH_TIME = 200;

    //protected VideoCapture camera = null;

    private VuforiaLocalizer vuforia;

    @Override
    public void runOpMode() {
        this.jewelTipper = this.hardwareMap.colorSensor.get("jewelTipper");

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this.hardwareMap.appContext);
        startPosition = sharedPref.getString("auton_start_position", "RED_RELIC");

        //leftEOPD = hardwareMap.opticalDistanceSensor.get("leftEOPD");
        //rightEOPD = hardwareMap.opticalDistanceSensor.get("rightEOPD");

        robotDriving = new RobotDriving(hardwareMap, telemetry);
        steering = robotDriving.getSteering();

        log = RobotLog.getRootInstance(telemetry);
        ultrasonicFunction = new UltrasonicFunction(hardwareMap, log);
        gunnerFunction = new GunnerFunction(hardwareMap, telemetry);

        /* VUFORIA CODE */

        //Tell Vuforia to display video feed
        int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters(cameraMonitorViewId);
        //Disable Video Feed with the following:
        //VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters();


        //Set License Key, tell Vuforia to use back Camera, finalize Vuforia Parameters for Localizer
        parameters.vuforiaLicenseKey = "AUrG/9b/////AAAAGYb5udEIt0W7p8AYwNbs5lUO4Ojghb2IvJN64Q6ZnRvUfbl59Au5c48n2lykKrsfZgYx9m2HpOgdNFmLaxhilDQIc0mmohbk5IjXvKkGGJR4OiqNtqYVDncXZpb/esaPeFTLtkbJFAlEs+oPwcAKoO5FctEuFyEgz1IJc6/MRphweDiXuJ86Rqs81UVeOlNXdr3QtZazJcViHHeWkv5pJUvefJWbjXSZvOZFlITDaTbyPIibQsHsbDg+B0IBMHK+d8fs58anmJ1VJEoQ9Xo0YDpHgosgx996zfSjhAK8YcIxLQdCkFbJtQtkpAl14jz+Yz86QvP8AMtFeMRuIYrPWhQvPJ3VU/Jm1Qmz+p/jbJ8k";
        parameters.cameraDirection = VuforiaLocalizer.CameraDirection.BACK;
        this.vuforia = ClassFactory.createVuforiaLocalizer(parameters);
        Vuforia.setFrameFormat(PIXEL_FORMAT.RGB565, true);

        //Load in the VuMark dataset from assets
        VuforiaTrackables relicTrackables = this.vuforia.loadTrackablesFromAsset("RelicVuMark");
        VuforiaTrackable relicTemplate = relicTrackables.get(0);

        //Wait for OpMode Init Button to be Pressed
        waitForStart();

        steering.setSpeedRatio(MOVE_SPEED_RATIO);

        //Activate the VuMark Dataset as Current Tracked Object
        relicTrackables.activate();

        telemetry.setMsTransmissionInterval(0);

        /* GET PICTOGRAPH */

        char pictograph = readVuMark(relicTemplate);

        telemetry.addData("Pictograph", "" + pictograph);
        telemetry.update();

        if (pictograph == '!') {
            telemetry.addData("Pictograph", "Unreliable");
            //Displays in the event that 3/3 times, the data returned by readVuMark() has been 1L,1C,1R, not allowing for retractRelicSlide logical interpretation.
        } else if (pictograph == 'l') {
            telemetry.addData("Pictograph", "Left");
        } else if (pictograph == 'r') {
            telemetry.addData("Pictograph", "Right");
        } else if (pictograph == 'c') {
            telemetry.addData("Pictograph", "Center");
        } else {
            telemetry.addData("Pictograph", "ERROR");
            //Displays only if the initial value of pictograph remains unchanged, which shouldn't occur.
        }
        telemetry.update();

        knockJewel();
        steering.setSpeedRatio(MOVE_SPEED_RATIO);

        testMoveAongWall(false, 130, 50);

        gunnerFunction.extendAutonGlyphter();
        sleep(1000);
        gunnerFunction.retractAutonGlyphter();
        steering.move(270);
        steering.finishSteering();
        sleep(500);
        steering.stopAllMotors();
    }

    /**
     * Read the pictograph.
     * @return A character representing whether the pictograph is left, right, or center.
     */
    char readVuMark(VuforiaTrackable relicTemplate) {
        RelicRecoveryVuMark vuMark = null;
        int total = 0;
        int left = 0;
        int right = 0;
        int center = 0;
        while (total < 3) {
            vuMark = RelicRecoveryVuMark.from(relicTemplate);
            sleep(100);
            if (vuMark == RelicRecoveryVuMark.LEFT) {
                left++;
                total++;
            } else if (vuMark == RelicRecoveryVuMark.RIGHT) {
                right++;
                total++;
            } else if (vuMark == RelicRecoveryVuMark.CENTER) {
                center++;
                total++;
            } else {
                telemetry.addData("unknown", "");
                telemetry.update();
                // unknown
                total++;
            }

        }
        if (left > right && left > center) {
            return 'l'; //Left is most likely Correct
        } else if (right > left && right > center) {
            return 'r'; //Right is most likely Correct
        } else if (center > right && center > left) {
            return 'c'; //Center is most likely Correct
        }
        return '!';
    }

    /**
     * Move along retractRelicSlide wall.
     */
    public void moveAlongWall(boolean moveRight, boolean senseRight, int sideDistance, int wallDistance) {
        steering.setSpeedRatio(MOVE_SPEED_RATIO);
        double clockwiseTurnWeight = 0;
        double forwardWeight = 0;
        final double MAX_TURN_WEIGHT = 0.2;
        final double MAX_FORWARD_WEIGHT = 0.2;

        //Rate of acceleration
        final double INCREASE_RATE = 0.005;

        //Rate of deceleration
        final double NORMALIZE_RATE = 0.02;
        double distanceLF;
        double distanceRF;
        boolean keepMoving = true;
        if (moveRight && senseRight) ultrasonicFunction.setRight(255);
        else if (moveRight && !senseRight) ultrasonicFunction.setLeft(0);
        else if (!moveRight && senseRight) ultrasonicFunction.setRight(0);
        else if (!moveRight && !senseRight) ultrasonicFunction.setLeft(255);
        while (keepMoving && opModeIsActive()) {
            //Set power in direction of motion
            if (moveRight) {
                steering.moveDegrees(0, 1);
            } else {
                steering.moveDegrees(180, 1);
            }

            //Sense distances to walls
            distanceLF = ultrasonicFunction.getLF();
            distanceRF = ultrasonicFunction.getRF();

            //Determine robot turning off course
            if (distanceLF + 1 < distanceRF) {
                if (clockwiseTurnWeight > 0) {
                    clockwiseTurnWeight = Math.max(clockwiseTurnWeight - NORMALIZE_RATE, -MAX_TURN_WEIGHT);
                } else {
                    clockwiseTurnWeight = Math.max(clockwiseTurnWeight - INCREASE_RATE, -MAX_TURN_WEIGHT);
                }

            } else if (distanceRF + 1 < distanceLF) {
                if (clockwiseTurnWeight > 0) {
                    clockwiseTurnWeight = Math.min(clockwiseTurnWeight + INCREASE_RATE, MAX_TURN_WEIGHT);
                } else {
                    clockwiseTurnWeight = Math.min(clockwiseTurnWeight + NORMALIZE_RATE, MAX_TURN_WEIGHT);
                }
            }

            //Determine robot drifting off course
            if ((distanceLF + distanceRF) / 2 + 1 < wallDistance) {
                if (forwardWeight > 0) {
                    forwardWeight = Math.max(forwardWeight - NORMALIZE_RATE, -MAX_FORWARD_WEIGHT);
                } else {
                    forwardWeight = Math.max(forwardWeight - INCREASE_RATE, -MAX_FORWARD_WEIGHT);
                }
            } else if ((distanceLF + distanceRF) / 2 - 1 > wallDistance) {
                if (forwardWeight > 0) {
                    forwardWeight = Math.min(forwardWeight + INCREASE_RATE, MAX_FORWARD_WEIGHT);
                } else {
                    forwardWeight = Math.min(forwardWeight + NORMALIZE_RATE, MAX_FORWARD_WEIGHT);
                }
            }

            telemetry.addData("Forward weight", forwardWeight);
            telemetry.addData("Clockwise turn weight", clockwiseTurnWeight);
            telemetry.addData("Code RF", distanceRF);
            telemetry.addData("Code LF", distanceLF);
            ultrasonicFunction.printTestData();
            telemetry.update();

            if (forwardWeight > 0) {
                steering.moveDegrees(90, forwardWeight);
            } else {
                steering.moveDegrees(270, -forwardWeight);
            }

            steering.turn(clockwiseTurnWeight);

            steering.finishSteering();

            //determine whether to keep moving
            if (moveRight && senseRight) {
                keepMoving = ultrasonicFunction.getRight() > sideDistance;
            } else if (!moveRight && senseRight) {
                keepMoving = ultrasonicFunction.getRight() < sideDistance;
            } else if (moveRight) {
                keepMoving = ultrasonicFunction.getLeft() < sideDistance;
            } else {
                keepMoving = ultrasonicFunction.getLeft() > sideDistance;
            }
        }
        steering.stopAllMotors();
        alignToWall();
    }

    public void testMoveAongWall(boolean senseRight, int targetSideDistance, int targetWallDistance) {
        double sensorDistanceLF;
        double sensorDistanceRF;
        double sensorDistanceSide;
        double robotClockwiseRotation;
        double frontDistance;
        double sideDistance;
        double robotMovementAngle;
        double robotMovementDistance;
        double clockwiseTurnSpeed;
        double moveSpeed;

        boolean keepMoving = true;
        while (keepMoving && opModeIsActive()) {
            sensorDistanceLF = ultrasonicFunction.getLF();
            sensorDistanceRF = ultrasonicFunction.getRF();
            if (senseRight) { sensorDistanceSide = ultrasonicFunction.getRight(); }
            else { sensorDistanceSide = ultrasonicFunction.getLeft(); }

            robotClockwiseRotation = Math.atan2(sensorDistanceLF - sensorDistanceRF, 20);
            frontDistance = (sensorDistanceLF + sensorDistanceRF) * Math.cos(robotClockwiseRotation) / 2;
            sideDistance = sensorDistanceSide * Math.cos(robotClockwiseRotation);

            if (senseRight) {
                robotMovementAngle = Math.atan2(frontDistance - targetWallDistance, sideDistance - targetSideDistance);
            }
            else {
                robotMovementAngle = Math.atan2(frontDistance - targetWallDistance, targetSideDistance - sideDistance);
            }

            robotMovementDistance = Math.sqrt(Math.pow(frontDistance - targetWallDistance, 2) + Math.pow(sideDistance - targetSideDistance, 2));
            clockwiseTurnSpeed = -robotClockwiseRotation / (5 * Math.sqrt(Math.toRadians(100) + Math.pow(robotClockwiseRotation, 2)));
            moveSpeed = robotMovementDistance / (3 * Math.sqrt(100 + Math.pow(robotMovementDistance, 2)));
            steering.setSpeedRatio(Math.sqrt(Math.pow(clockwiseTurnSpeed, 2) + Math.pow(moveSpeed, 2)));
            steering.turn(clockwiseTurnSpeed);
            steering.moveRadians(robotMovementAngle + robotClockwiseRotation, moveSpeed);
            steering.finishSteering();

            keepMoving = Math.abs(sensorDistanceLF - targetWallDistance) > 1 || Math.abs(sensorDistanceRF - targetWallDistance) > 1 || Math.abs(sensorDistanceSide - targetSideDistance) > 1;

        }
    }

    /**
     * Turn ninety degrees.
     */
    public void turnNinety(boolean isClockwise) {
        steering.setSpeedRatio(TURN_SPEED_RATIO);
        if (isClockwise) {
            //leftDist is the distance detected from ultrasonicLeft in the previous tick
            double leftDist = 255;

            //Turn until left distance begins to increase (meaning that robot has passed the position that it should reach)
            while (ultrasonicFunction.getLeft() <= leftDist) {
                steering.turn(1);
                steering.finishSteering();
                leftDist = ultrasonicFunction.getLeft();
            }
            steering.stopAllMotors();
            //Return to position of minimum left distance
            while (ultrasonicFunction.getLeft() > leftDist) {
                steering.turn(-1);
                steering.finishSteering();
                leftDist = ultrasonicFunction.getLeft();
            }
            alignToWall();
        } else {
            //rightDist is the distance detected from ultrasonicRight in the previous tick
            double rightDist = 255;

            //Turn until right distance begins to increase (meaning that robot has passed the position that it should reach)
            while (ultrasonicFunction.getRight() <= rightDist) {
                steering.turn(-1);
                steering.finishSteering();
                rightDist = ultrasonicFunction.getRight();
            }
            steering.stopAllMotors();
            //Return to position of minimum right distance
            while (ultrasonicFunction.getRight() > rightDist) {
                steering.turn(1);
                steering.finishSteering();
                rightDist = ultrasonicFunction.getRight();
            }
            alignToWall();
        }
    }

    /**
     * Align to retractRelicSlide wall.
     */
    public void alignToWall() {
        steering.setSpeedRatio(TURN_SPEED_RATIO);
        while (Math.abs(ultrasonicFunction.getLF() - ultrasonicFunction.getRF()) >= 1) {
            if (ultrasonicFunction.getLF() < ultrasonicFunction.getRF()) {
                steering.turn(-1);
            } else if (ultrasonicFunction.getRF() < ultrasonicFunction.getLF()) {
                steering.turn(1);
            }
            steering.finishSteering();
            if (ultrasonicFunction.getLF() == ultrasonicFunction.getRF()) {
                steering.stopAllMotors();
                break;
            }
        }
    }

    /**
     * Get closer to the crytobox once the robot is in front of it.
     */
    public void approachCryptobox(boolean senseRight, int goalSideDistance) {
        double clockwiseTurnWeight = 0;
        double rightWeight = 0;
        final double MAX_TURN_WEIGHT = 0.2;
        final double MAX_FORWARD_WEIGHT = 0.2;

        //Rate of acceleration
        final double INCREASE_RATE = 0.005;

        //Rate of deceleration
        final double NORMALIZE_RATE = 0.02;
        double sideDistance;
        double distanceLF;
        double distanceRF;
        boolean keepMoving = true;
        final int wallDistance = 28;

        steering.setSpeedRatio(MOVE_SPEED_RATIO);
        while (ultrasonicFunction.getRF() + ultrasonicFunction.getLF() > wallDistance * 2) {
            telemetry.addData("getRF: ", ultrasonicFunction.getRF());
            telemetry.addData("getLF: ", ultrasonicFunction.getLF());
            telemetry.update();
            distanceLF = ultrasonicFunction.getLF();
            distanceRF = ultrasonicFunction.getRF();
            sideDistance = senseRight ? ultrasonicFunction.getRight() : ultrasonicFunction.getLeft();
            if (distanceLF > distanceRF + 1) {
                if (clockwiseTurnWeight < 0) {
                    clockwiseTurnWeight = Math.min(clockwiseTurnWeight + NORMALIZE_RATE, MAX_TURN_WEIGHT);
                } else {
                    clockwiseTurnWeight = Math.min(clockwiseTurnWeight + INCREASE_RATE, MAX_TURN_WEIGHT);
                }
            }
            else if (distanceRF > distanceLF + 1) {
                if (clockwiseTurnWeight > 0) {
                    clockwiseTurnWeight = Math.max(clockwiseTurnWeight - NORMALIZE_RATE, -MAX_TURN_WEIGHT);
                } else {
                    clockwiseTurnWeight = Math.max(clockwiseTurnWeight - INCREASE_RATE, -MAX_TURN_WEIGHT);
                }
            }

            if (sideDistance > goalSideDistance + 1) {
                if (senseRight) {
                    if (rightWeight < 0) {
                        rightWeight = Math.min(rightWeight + NORMALIZE_RATE, MAX_TURN_WEIGHT);
                    } else {
                        rightWeight = Math.min(rightWeight + INCREASE_RATE, MAX_TURN_WEIGHT);
                    }
                } else {
                    if (rightWeight > 0) {
                        rightWeight = Math.max(rightWeight - NORMALIZE_RATE, -MAX_TURN_WEIGHT);
                    } else {
                        rightWeight = Math.max(rightWeight - INCREASE_RATE, -MAX_TURN_WEIGHT);
                    }
                }
            }
            else if (sideDistance < goalSideDistance - 1) {
                if (!senseRight) {
                    if (rightWeight < 0) {
                        rightWeight = Math.min(rightWeight + NORMALIZE_RATE, MAX_TURN_WEIGHT);
                    } else {
                        rightWeight = Math.min(rightWeight + INCREASE_RATE, MAX_TURN_WEIGHT);
                    }
                } else {
                    if (rightWeight > 0) {
                        rightWeight = Math.max(rightWeight - NORMALIZE_RATE, -MAX_TURN_WEIGHT);
                    } else {
                        rightWeight = Math.max(rightWeight - INCREASE_RATE, -MAX_TURN_WEIGHT);
                    }
                }
            }
            steering.moveDegrees(90, 1);
            steering.turn(clockwiseTurnWeight);
            steering.moveDegrees(0, rightWeight);
            steering.finishSteering();
        }
        steering.stopAllMotors();
    }

    /*public void driveToCryptobox(CrypoboxPosition crypoboxPosition) {
        if (startPosition.equals("RED_RELIC")) {
            moveAlongWall(false, true, 150, 50);
        } else if (startPosition.equals("RED_MIDDLE")) {
            moveAlongWall(false, false, 60, 50);
        } else if (startPosition.equals("BLUE_RELIC")) {
            moveAlongWall(true, false, 150, 50);
        } else {
            moveAlongWall(true, true, 60, 50);
        }
    }*/

    /**
     * Knock the correct jewel down.
     */
    public void knockJewel() {
        telemetry.addData("knockJewel Method called", "");
        telemetry.update();
        steering.setSpeedRatio(MOVE_SPEED_RATIO);
        gunnerFunction.extendJewelPusher();
        sleep(JEWELPUSHER_TIME);
        gunnerFunction.stopJewelPusher();
        double red = 0;
        double blue = 0;
        for (int i = 0; i < 5; i++) {
            red += jewelTipper.red();
            blue += jewelTipper.blue();
            sleep(100);
        }
        telemetry.addData("red", red);
        telemetry.addData("blue", blue);
        telemetry.update();
        boolean isRedTeam = startPosition.equals("RED_RELIC") || startPosition.equals("RED_MIDDLE");

        if(red > blue) {
            if(isRedTeam) {
                pushJewel(JewelPosition.RIGHT);
            }
            else {
                pushJewel(JewelPosition.LEFT);
            }
        }
        else if (red < blue){
            if(isRedTeam){
                pushJewel(JewelPosition.LEFT);
            }
            else{
                pushJewel(JewelPosition.RIGHT);
            }
        } else {
            gunnerFunction.retractJewelPusher();
            sleep(JEWELPUSHER_TIME);
            gunnerFunction.stopJewelPusher();
        }
    }

    /**
     * Push either the left or right jewel down.
     */
    public void pushJewel(JewelPosition jewelPosition) {
        if (jewelPosition == JewelPosition.LEFT) {
            steering.moveDegrees(180);
            steering.finishSteering();
            sleep(JEWEL_PUSH_TIME);
            steering.stopAllMotors();
            gunnerFunction.retractJewelPusher();
            sleep(JEWELPUSHER_TIME);
            gunnerFunction.stopJewelPusher();
            steering.moveDegrees(0);
            steering.finishSteering();
            sleep(JEWEL_PUSH_TIME);
            steering.stopAllMotors();
        } else {
            steering.moveDegrees(0);
            steering.finishSteering();
            sleep(JEWEL_PUSH_TIME);
            steering.stopAllMotors();
            gunnerFunction.retractJewelPusher();
            sleep(JEWELPUSHER_TIME);
            gunnerFunction.stopJewelPusher();
            steering.moveDegrees(180);
            steering.finishSteering();
            sleep(JEWEL_PUSH_TIME);
            steering.stopAllMotors();
        }
    }

    public enum JewelPosition {
        LEFT, RIGHT
    }
}