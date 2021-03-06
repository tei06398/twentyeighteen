package org.firstinspires.ftc.teamcode;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.preference.PreferenceManager;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.UltrasonicSensor;

import com.vuforia.Image;
import com.vuforia.PIXEL_FORMAT;
import com.vuforia.Vuforia;
import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.navigation.*;

import java.nio.ByteBuffer;

/**
 * Created 11/13/2017
 */
@Deprecated
//@Autonomous(name = "Deprecated Auton Mode")
public class RRAutonDeprecated extends LinearOpMode {
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
        ultrasonicFunction = new UltrasonicFunction(hardwareMap, new RobotLog("NiskyRobot", telemetry));

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

        //Get retractRelicSlide semi-reliable reading of the Pictograph
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

            //Detect whiffle ball location
            telemetry.update();

            vuforia.setFrameQueueCapacity(10);
            VuforiaLocalizer.CloseableFrame frame = vuforia.getFrameQueue().take();

            long numImages = frame.getNumImages();
            Image rgb = null;
            for (int i = 0; i < numImages; i++) {
                if (frame.getImage(i).getFormat() == PIXEL_FORMAT.RGB565) {
                    rgb = frame.getImage(i);
                    break;
                }
            }
            /* TEST */
            telemetry.addData("Testing: ", "Image spot 1");
            telemetry.addData("RGB: ", rgb);
            telemetry.update();

            // https://developer.vuforia.com/forum/android/how-transform-camera-image-androidgraphicsbitmap
            ByteBuffer pixels = rgb.getPixels();
            telemetry.addData("Pixels: ", pixels);telemetry.update();
            byte[] pixelArray = new byte[pixels.remaining()];
            telemetry.addData("Pixel array: ", pixelArray);telemetry.update();
            telemetry.addData("Pixel length: ", pixelArray.length);telemetry.update();
            pixels.get(pixelArray, 0, pixelArray.length);
            telemetry.addData("Pixel array2: ", pixelArray);telemetry.update();
            telemetry.addData("Pixel length2: ", pixelArray.length);telemetry.update();

            //BitmapFactory.Options opts = new BitmapFactory.Options();
            //opts.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bmp = BitmapFactory.decodeByteArray(pixelArray, 0, pixelArray.length, null);
            telemetry.addData("bmp: ", bmp);telemetry.update();

            /*rgb is now the Image object that weve used in the video*/
            //Bitmap bmp = Bitmap.createBitmap(rgb.getWidth(), rgb.getHeight(), Bitmap.Config.RGB_565);
            frame.close();
            int width = bmp.getWidth();
            int height = bmp.getHeight();

            /* TEST */
            telemetry.addData("Testing: ", "Image spot 2");
            telemetry.update();

            double redWeight = 0;
            double blueWeight = 0;

            double red;
            double blue;
            double green;
            double weight;

            for (int row = 0; row < 2 * height / 3; row += 10) {
                for (int col = 0; col < width / 2; col += 10) {
                    red = Color.red(bmp.getPixel(col, row));
                    blue = Color.blue(bmp.getPixel(col, row));
                    green = Color.green(bmp.getPixel(col, row));
                    telemetry.addData("red:", red);
                    telemetry.addData("green: ", green);
                    telemetry.addData("blue: ", blue);
                    telemetry.update();
                    if (red > blue && red > green) {
                        weight = 1 / (Math.pow(row - 3 * height / 4, 2) + Math.pow(col - 2 * width / 3, 2) + 1);
                        redWeight += red * weight;
                    } else if (blue > red && blue > green) {
                        weight = 1 / (Math.pow(row - 3 * height / 4, 2) + Math.pow(col - 2 * width / 3, 2) + 1);
                        blueWeight += blue * weight;
                    }
                }
            }
            /* TEST */
            telemetry.addData("blue weight: ", blueWeight);
            telemetry.addData("red weight: ", redWeight);
            telemetry.update();
            if (redWeight > blueWeight) {
                setJewelResult("red");
                telemetry.addData("Testing: ", "RED JEWEL DETECTED");
                telemetry.update();
                sleep(30000);
            } else {
                setJewelResult("blue");
                telemetry.addData("Testing: ", "BLUE JEWEL DETECTED");
                telemetry.update();
                sleep(30000);
            }
            /* TEST */
            telemetry.addData("Testing: ", "Image spot 4");
            telemetry.update();

            //TODO: Replace the Telemetry with actual boops when we have the booper -Seth
            if (getJewelResult().equals("red")) {
                //RED
                if (startPosition.equals("RED_RELIC") || startPosition.equals("RED_MIDDLE")) {
                    //thing.knockright();
                    telemetry.addData("Knock: ", "RIGHT");
                } else {
                    telemetry.addData("Knock: ", "LEFT");
                }
            } else if (getJewelResult().equals("blue")) {
                if (startPosition.equals("BLUE_RELIC") || startPosition.equals("BLUE_MIDDLE")) {
                    telemetry.addData("Knock: ", "RIGHT");
                } else {
                    telemetry.addData("Knock: ", "LEFT");
                }
            } else {
                telemetry.addData("Knock: ", "NaN - Björk 404: Ball Not Found");
                telemetry.update();
            }
            telemetry.update();

            knockJewel(JewelPosition.LEFT);
            driveToCryptobox(CrypoboxPosition.LEFT);
        }
    }

    public void moveAlongWall(boolean moveRight, boolean senseRight, int sideDistance, int wallDistance) {
        double distanceLF;
        double distanceRF;
        boolean keepMoving = true;
        while (keepMoving) {

            //Set power in direction of motion
            if (moveRight) {
                steering.moveDegrees(0, 1);
            } else {
                steering.moveDegrees(180, 1);
            }

            //Sense distances to walls
            distanceLF = ultrasonicFunction.getLF();
            distanceRF = ultrasonicFunction.getLF();

            //Determine robot turning off course
            if (distanceLF + 1 < distanceRF) {
                clockwiseTurnWeight -= 0.01;
            } else if (distanceRF + 1 < distanceLF) {
                clockwiseTurnWeight += 0.01;
            }

            //Determine robot drifting off course
            if ((distanceLF + distanceRF) / 2 + 1 < wallDistance) {
                forwardWeight += 0.01;
            } else if ((distanceLF + distanceRF) / 2 - 1 > wallDistance) {
                forwardWeight -= 0.01;
            }

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
            } else if (moveRight && !senseRight) {
                keepMoving = ultrasonicFunction.getLeft() < sideDistance;
            } else {
                keepMoving = ultrasonicFunction.getLeft() > sideDistance;
            }
        }
    }

    public void turnNinety(boolean isClockwise) {
        if (isClockwise) {
            while (ultrasonicFunction.getLeft() > ultrasonicFunction.getRight() || ultrasonicFunction.getLF() > ultrasonicFunction.getRF()) {
                steering.turn(true);
                steering.stopAllMotors();
            }
        } else {
            while (ultrasonicFunction.getLeft() < ultrasonicFunction.getRight() || ultrasonicFunction.getLF() < ultrasonicFunction.getRF()) {
                steering.turn(false);
                steering.stopAllMotors();
            }
        }
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
            while ((System.currentTimeMillis() - startTime) < 100) {
                steering.turn(false);
                steering.finishSteering();
            }
        } else {
            while ((System.currentTimeMillis() - startTime) < 100) {
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