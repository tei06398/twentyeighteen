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
@Autonomous(name = "RR Official Auton Mode")
public class RRNewAuton extends LinearOpMode {
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
    protected final double PRECISE_TURN_SPEED_RATIO = 0.3;
    protected final double FAST_TURN_SPEED_RATIO = 0.6;
    protected final int JEWELPUSHER_EXTENSION_TIME = 3500;
    protected final int JEWEL_PUSH_TIME = 600;

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
        gunnerFunction.disablePwm(hardwareMap);

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

        /**
         *
         *
         *
        START ACTUALLY DOING STUFF
         *
         *
         *
         */
        gunnerFunction.enablePwm(hardwareMap);
        steering.setSpeedRatio(MOVE_SPEED_RATIO);

        gunnerFunction.upWinch();
        sleep(250);
        gunnerFunction.stopWinch();

        //Activate the VuMark Dataset as Current Tracked Object
        relicTrackables.activate();

        telemetry.setMsTransmissionInterval(0);
        sleep(500);
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
        sleep(1000);
        gunnerFunction.openGlyphterFully();
        knockJewel();

        if (startPosition.equals("RED_MIDDLE")) {
            int sideDistance;
            if (pictograph == 'l') {
                sideDistance = 88;
            } else if (pictograph == 'r') {
                sideDistance = 49;
            } else {
                sideDistance = 69;
            }
            moveAlongWall(false, 30, 40, 5);
            turnNinety(false);
            moveAlongWall(true, sideDistance, 40, 5);
            moveAlongWall(true, sideDistance, 25, 2);
            sleep(1000);
            alignToWall();
            steering.setSpeedRatio(MOVE_SPEED_RATIO);
            gunnerFunction.extendAutonGlyphter();
            sleep(1500);
            gunnerFunction.retractAutonGlyphter();
            moveTime(270, 500);
            moveTime(90, 700);
            moveTime(270, 300);

        } else if (startPosition.equals("RED_RELIC")) {
            int sideDistance;
            if (pictograph == 'l') {
                sideDistance = 151;
            } else if (pictograph == 'r') {
                sideDistance = 110;
            } else {
                sideDistance = 131;
            }
            moveTime(180, 1000);
            moveAlongWall(true, sideDistance, 40, 5);
            moveAlongWall(true, sideDistance, 25, 2);
            sleep(1000);
            alignToWall();
            steering.setSpeedRatio(MOVE_SPEED_RATIO);
            gunnerFunction.extendAutonGlyphter();
            sleep(1500);
            gunnerFunction.retractAutonGlyphter();
            moveTime(270, 500);
            moveTime(90, 700);
            moveTime(270, 300);

        } else if (startPosition.equals("BLUE_MIDDLE")) {
            int sideDistance;
            if (pictograph == 'l') {
                sideDistance = 58;
            } else if (pictograph == 'r') {
                sideDistance = 98;
            } else {
                sideDistance = 77;
            }
            moveAlongWall(true, 30, 40, 5);
            turnNinety(true);
            moveAlongWall(false, sideDistance, 40, 5);
            moveAlongWall(false, sideDistance, 25, 2);
            sleep(1000);
            alignToWall();
            steering.setSpeedRatio(MOVE_SPEED_RATIO);
            gunnerFunction.extendAutonGlyphter();
            sleep(1500);
            gunnerFunction.retractAutonGlyphter();
            moveTime(270, 500);
            moveTime(90, 700);
            moveTime(270, 300);

        } else {
            int sideDistance;
            if (pictograph == 'l') {
                sideDistance = 115;
            } else if (pictograph == 'r') {
                sideDistance = 154;
            } else {
                sideDistance = 136;
            }
            moveTime(0, 1000);
            moveAlongWall(false, sideDistance, 40, 5);
            moveAlongWall(false, sideDistance, 25, 2);
            sleep(1000);
            alignToWall();
            steering.setSpeedRatio(MOVE_SPEED_RATIO);
            gunnerFunction.extendAutonGlyphter();
            sleep(1500);
            gunnerFunction.retractAutonGlyphter();
            moveTime(270, 500);
            moveTime(90, 700);
            moveTime(270, 300);
        }
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

    public void moveAlongWall(boolean senseRight, int targetSideDistance, int targetWallDistance, int tolerance) {
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
        while (keepMoving && opModeIsActive() && !isStopRequested()) {
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
            clockwiseTurnSpeed = -robotClockwiseRotation / (5 * Math.sqrt(Math.toRadians(20) + Math.pow(robotClockwiseRotation, 2)));
            moveSpeed = robotMovementDistance / (3 * Math.sqrt(30 + Math.pow(robotMovementDistance, 2)));
            steering.setSpeedRatio(Math.sqrt(Math.pow(clockwiseTurnSpeed, 2) + Math.pow(moveSpeed, 2)));
            steering.turn(clockwiseTurnSpeed);
            steering.moveRadians(robotMovementAngle + robotClockwiseRotation, moveSpeed);
            steering.finishSteering();

            keepMoving = Math.abs(sensorDistanceLF - targetWallDistance) > tolerance || Math.abs(sensorDistanceRF - targetWallDistance) > tolerance || Math.abs(sensorDistanceSide - targetSideDistance) > tolerance;
            telemetry.addData("Desired position: ", targetSideDistance + ", " + targetWallDistance);
            telemetry.addData("frontDistance", frontDistance);
            telemetry.addData("sideDistance", sideDistance);
            telemetry.addData("robotMovementAngle", robotMovementAngle * 180 / Math.PI);
            telemetry.addData("robotMovementDistance", robotMovementDistance);
            telemetry.addData("clockwiseTurnSpeed", clockwiseTurnSpeed);
            telemetry.addData("moveSpeed", moveSpeed);
            telemetry.addData("sensorDistanceLF", sensorDistanceLF);
            telemetry.addData("sensorDistanceRF", sensorDistanceRF);
            telemetry.addData("sensorDistanceSide", sensorDistanceSide);
            telemetry.addData("robotClockwiseRotation", robotClockwiseRotation);
            telemetry.update();
        }
        steering.stopAllMotors();
    }

    /**
     * Turn ninety degrees.
     */
    public void turnNinety(boolean isClockwise) {
        steering.setSpeedRatio(FAST_TURN_SPEED_RATIO);
        if (isClockwise) {
            steering.turnClockwise();
        } else {
            steering.turnCounterclockwise();
        }
        steering.finishSteering();
        sleep(1250);
        steering.stopAllMotors();
        alignToWall();
    }

    /**
     * Align to retractRelicSlide wall.
     */
    public void alignToWall() {
        steering.setSpeedRatio(PRECISE_TURN_SPEED_RATIO);
        double lfDist = ultrasonicFunction.getLF();
        double rfDist = ultrasonicFunction.getRF();
        while (Math.abs(lfDist - rfDist) >= 1 && opModeIsActive()) {
            if (lfDist > rfDist) { steering.turnClockwise(); telemetry.addData("Turning clockwise",""); }
            else if (rfDist > lfDist) { steering.turnCounterclockwise(); telemetry.addData("Turning counterclockwise","");}
            steering.finishSteering();
            lfDist = ultrasonicFunction.getLF();
            rfDist = ultrasonicFunction.getRF();
            telemetry.addData("Left Sensor Reading", lfDist);
            telemetry.addData("Right Sensor Reading", rfDist);
            telemetry.update();
        }
        steering.stopAllMotors();
    }

    /**
     * Knock the correct jewel down.
     */
    public void knockJewel() {
        telemetry.addData("knockJewel Method called", "");
        telemetry.update();
        steering.setSpeedRatio(PRECISE_TURN_SPEED_RATIO);
        gunnerFunction.extendJewelPusher();
        sleep(JEWELPUSHER_EXTENSION_TIME);
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
            sleep(JEWELPUSHER_EXTENSION_TIME);
            gunnerFunction.stopJewelPusher();
        }
    }

    /**
     * Push either the left or right jewel down.
     */
    public void pushJewel(JewelPosition jewelPosition) {
        if (jewelPosition == JewelPosition.LEFT) {
            steering.turnCounterclockwise(1);
            steering.move(270, 0.5);
            steering.finishSteering();
            sleep(JEWEL_PUSH_TIME);
            steering.stopAllMotors();
            gunnerFunction.retractJewelPusher();
            sleep(JEWELPUSHER_EXTENSION_TIME);
            gunnerFunction.stopJewelPusher();
            steering.turnClockwise(1);
            steering.move(270, 0.3);
            steering.finishSteering();
            sleep(JEWEL_PUSH_TIME + 250);
            steering.stopAllMotors();
            sleep(1000);
        } else {
            steering.turnClockwise();
            steering.finishSteering();
            sleep(JEWEL_PUSH_TIME);
            steering.stopAllMotors();
            gunnerFunction.retractJewelPusher();
            sleep(JEWELPUSHER_EXTENSION_TIME);
            gunnerFunction.stopJewelPusher();
        }
    }

    public void moveTime(double degrees, long time) {
        steering.moveDegrees(degrees);
        steering.finishSteering();
        sleep(time);
        steering.stopAllMotors();
    }

    public void turnTime(boolean isClockwise, long time) {
        steering.turn(isClockwise);
        steering.finishSteering();
        sleep(time);
        steering.stopAllMotors();
    }

    public enum JewelPosition {
        LEFT, RIGHT
    }
}