package org.firstinspires.ftc.teamcode;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
import android.preference.Preference;
import android.preference.PreferenceManager;
import com.qualcomm.ftccommon.FtcRobotControllerService;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.FtcRobotControllerServiceState;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.UltrasonicSensor;

import com.vuforia.Image;
import com.vuforia.PIXEL_FORMAT;
import com.vuforia.Vuforia;
import org.firstinspires.ftc.robotcontroller.internal.FtcRobotControllerActivity;
import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.navigation.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.ByteBuffer;

/**
 * Created 11/13/2017
 */
@Autonomous(name = "RR Official Auton Mode")
public class RRAuton extends LinearOpMode {
    //Declares Motors
    protected DcMotor motorLF = null;
    protected DcMotor motorRF = null;
    protected DcMotor motorLB = null;
    protected DcMotor motorRB = null;
    protected UltrasonicSensor ultrasonicLeft = null;
    protected UltrasonicSensor ultrasonicRight = null;
    protected UltrasonicSensor ultrasonicRF = null;
    protected UltrasonicSensor ultrasonicLF = null;
    protected Servo jewelPusher = null;

    protected RobotDriving robotDriving;
    protected RobotDriving.Steering steering;

    protected UltrasonicFunction ultrasonicFunction;

    SharedPreferences sharedPref;
    protected String startPosition; //RED_RELIC, RED_MIDDLE, BLUE_RELIC, BLUE_MIDDLE
    protected int JEWEL_PUSHER_DOWN = 0;
    protected int JEWEL_PUSHER_UP = 100;

    //Required distance from wall at the beginning
    protected int wallDistance = 30;
    protected double clockwiseTurnWeight = 0;
    protected double forwardWeight = 0;
    protected final double MOVE_SPEED_RATIO = 0.2;
    protected final double TURN_SPEED_RATIO = 0.1;


    //protected VideoCapture camera = null;

    private VuforiaLocalizer vuforia;
    public static String jewelResult = "";

    public static final void setJewelResult(String result) {
        jewelResult = result;
    }

    public static String getJewelResult() {
        return jewelResult;
    }

    char readVuMark(VuforiaTrackable relicTemplate) {
        RelicRecoveryVuMark vuMark = null;
        int total = 0;
        int left = 0;
        int right = 0;
        int center = 0;
        while (total < 3) {
            vuMark = RelicRecoveryVuMark.from(relicTemplate);
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
                telemetry.addData("unknown", "");telemetry.update();
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


    @Override
    public void runOpMode() throws InterruptedException {
        //Sets up Motors
        this.motorLF = this.hardwareMap.dcMotor.get("lfMotor");
        this.motorRF = this.hardwareMap.dcMotor.get("rfMotor");
        this.motorLB = this.hardwareMap.dcMotor.get("lbMotor");
        this.motorRB = this.hardwareMap.dcMotor.get("rbMotor");
        this.ultrasonicLeft = this.hardwareMap.ultrasonicSensor.get("ultrasonicLeft"); //module 2, port 1
        this.ultrasonicRight = this.hardwareMap.ultrasonicSensor.get("ultrasonicRight");//module 2, port 2
        this.ultrasonicLF = this.hardwareMap.ultrasonicSensor.get("ultrasonicLF"); //module 3, port 3
        this.ultrasonicRF = this.hardwareMap.ultrasonicSensor.get("ultrasonicRF"); //module 4, port 4

        this.motorLF.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        this.motorRF.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        this.motorLB.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        this.motorRB.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this.hardwareMap.appContext);
        startPosition = sharedPref.getString("auton_start_position", "RED_RELIC");

        // RobotDriving instantiation
        robotDriving = new RobotDriving(hardwareMap, telemetry);
        steering = robotDriving.getSteering();

        //Ultrasonic function instantiation
        ultrasonicFunction = new UltrasonicFunction(ultrasonicLeft, ultrasonicRight, ultrasonicRF, ultrasonicLF, telemetry);

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

        //Activate the VuMark Dataset as Current Tracked Object
        relicTrackables.activate();

        /* TEST */
        telemetry.setAutoClear(false);
        telemetry.setMsTransmissionInterval(0);

        //Get a semi-reliable reading of the Pictograph
        while (this.opModeIsActive()) {
            int total = 0;
            char pictograph = 'E';
            //while (total < 3) {
            pictograph = readVuMark(relicTemplate);
            //    if (pictograph != '!') {
            //        total = 3;
            //    } else {
            //         total++;
            //    }
            //}
            telemetry.addData("Pictograph", "" + pictograph);
            telemetry.update();

            if (pictograph == '!') {
                telemetry.addData("Pictograph", "Unreliable");
                //Displays in the event that 3/3 times, the data returned by readVuMark() has been 1L,1C,1R, not allowing for a logical interpretation.
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

            //Detect whiffle ball location
            telemetry.update();
        }
    }

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

            //Weird issue with left/right mirroring, that's why clockwiseTurnWeight is negated
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

    public void approachCryptobox() {
        steering.setSpeedRatio(MOVE_SPEED_RATIO);
        final int wallDistance = 28;
        while (ultrasonicFunction.getRF() + ultrasonicFunction.getLF() > wallDistance * 2) {
            telemetry.addData("getRF: ", ultrasonicFunction.getRF());
            telemetry.addData("getLF: ", ultrasonicFunction.getLF());
            telemetry.update();
            steering.moveDegrees(90, 1);
            if (ultrasonicFunction.getLF() > ultrasonicFunction.getRF() + 1) {
                steering.turn(0.1);
            }
            else if (ultrasonicFunction.getRF() > ultrasonicFunction.getLF() + 1) {
                steering.turn(-0.1);
            }
            steering.finishSteering();
        }
        steering.stopAllMotors();
    }

    public void driveToCryptobox(CrypoboxPosition crypoboxPosition) {
        if (startPosition.equals("RED_RELIC")) {
            moveAlongWall(false, true, 150, 50);
        } else if (startPosition.equals("RED_MIDDLE")) {
            moveAlongWall(false, false, 60, 50);
        } else if (startPosition.equals("BLUE_RELIC")) {
            moveAlongWall(true, false, 150, 50);
        } else {
            moveAlongWall(true, true, 60, 50);
        }
    }

    public void knockJewel(JewelPosition jewelPosition) {
        this.jewelPusher.setPosition(JEWEL_PUSHER_DOWN);
        steering.setSpeedRatio(0.3);
        long startTime = System.currentTimeMillis();
        if (jewelPosition == JewelPosition.LEFT) {
            while ((System.currentTimeMillis() - startTime) < 50) {
                steering.turn(false);
                steering.finishSteering();
            }
        } else {
            while ((System.currentTimeMillis() - startTime) < 50) {
                steering.turn(true);
                steering.finishSteering();
            }
        }
        steering.stopAllMotors();
        steering.finishSteering();
        this.jewelPusher.setPosition(JEWEL_PUSHER_UP);
    }

    public enum CrypoboxPosition {
        LEFT, CENTER, RIGHT;
    }

    public enum JewelPosition {
        LEFT, RIGHT;
    }

}