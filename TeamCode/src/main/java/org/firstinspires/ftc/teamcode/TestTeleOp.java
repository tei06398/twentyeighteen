package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.*;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;

/**
 * Last Modified 11/13/2017
 */
@TeleOp(name = "Relic Recovery Official Tele-Op Mode")
public class TestTeleOp extends OpMode {
    /* Declare here any fields you might find useful. */
    // Declares motors
    protected DcMotor motorLF = null;
    protected DcMotor motorRF = null;
    protected DcMotor motorLB = null;
    protected DcMotor motorRB = null;

    public void loop(){
        if (this.gamepad1.a) {
            this.motorLB.setPower(1);
        }
        if (this.gamepad1.b) {
            this.motorLF.setPower(1);
        }
        if (this.gamepad1.x) {
            this.motorRB.setPower(1);
        }
        if (this.gamepad1.y) {
            this.motorRF.setPower(1);
        }
        telemetry.update();
    }

    public void init(){

        //Instantiates motors and servos, sets operating mode
        this.motorLF = this.hardwareMap.dcMotor.get("lfMotor");
        this.motorRF = this.hardwareMap.dcMotor.get("rfMotor");
        this.motorLB = this.hardwareMap.dcMotor.get("lbMotor");
        this.motorRB = this.hardwareMap.dcMotor.get("rbMotor");
        this.motorLF.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        this.motorRF.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        this.motorLB.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        this.motorRB.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);


    }


}