/*
 * Copyright (C) 2014 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.motorola.araplox;

import android.hardware.I2cManager;
import android.hardware.I2cTransaction;
import android.util.Log;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.DataOutputStream;
import java.io.FileOutputStream;

public class AFE4400Thread extends Thread {

    // The 7-bit slave address
    private static final int address = (0x50 >> 1);
    private static final String TAG = "araplox";

    private Mlx90620Listener listener;
    private I2cManager i2c;

    private volatile boolean stopped;
    private DataOutputStream buf = null;

    private static final I2cTransaction[] setupWrites = {
        WriteReg(0x00, 0x00, 0x00, 0x00), //AFE4400_CONTROL0
        WriteReg(0x01, 0x00, 0x17, 0xD4), //AFE4400_LED2STC
        WriteReg(0x02, 0x00, 0x1D, 0xAE), //AFE4400_LEDENDC
        WriteReg(0x03, 0x00, 0x17, 0x70), //AFE4400_LED2LEDSTC
        WriteReg(0x04, 0x00, 0x1D, 0xAF), //AFE4400_LED2LEDENDC
        WriteReg(0x05, 0x00, 0x00, 0x00), //AFE4400_ALED2STC
        WriteReg(0x06, 0x00, 0x06, 0x3E), //AFE4400_ALED2ENDC
        WriteReg(0x07, 0x00, 0x08, 0x34), //AFE4400_LED1STC
        WriteReg(0x08, 0x00, 0x0E, 0x0E), //AFE4400_LED1ENDC
        WriteReg(0x09, 0x00, 0x07, 0xD0), //AFE4400_LED1LEDSTC
        WriteReg(10, 0x00, 0x0E, 0x0F), //AFE4400_LED1LEDENDC

        WriteReg(11, 0x00, 0x0F, 0xA0), //AFE4400_ALED1STC
        WriteReg(12, 0x00, 0x15, 0xDE), //AFE4400_ALED1ENDC
        WriteReg(13, 0x00, 0x00, 0x02), //AFE4400_LED2CONVST
        WriteReg(14, 0x00, 0x07, 0xCF), //AFE4400_LED2CONVEND
        WriteReg(15, 0x00, 0x07, 0xD2), //AFE4400_ALED2CONVST
        WriteReg(16, 0x00, 0x0F, 0x9F), //AFE4400_ALED2CONVEND
        WriteReg(17, 0x00, 0x0F, 0xA2), //AFE4400_LED1CONVST
        WriteReg(18, 0x00, 0x17, 0x6F), //AFE4400_LED1CONVEND
        WriteReg(19, 0x00, 0x17, 0x72), //AFE4400_ALED1CONVST
        WriteReg(20, 0x00, 0x1F, 0x3F), //AFE4400_ALED1CONVEND

        WriteReg(21, 0x00, 0x00, 0x00),//AFE4400_ADCRSTSTCT0
        WriteReg(22, 0x00, 0x00, 0x00),//AFE4400_ADCRSTENDCT0
        WriteReg(23, 0x00, 0x07, 0xD0),//AFE4400_ADCRSTSTCT1
        WriteReg(24, 0x00, 0x07, 0xD0),//AFE4400_ADCRSTENDCT1
        WriteReg(25, 0x00, 0x0F, 0XA0),//AFE4400_ADCRSTSTCT2
        WriteReg(26, 0x00, 0x0F, 0XA0),//AFE4400_ADCRSTENDCT2
        WriteReg(27, 0x00, 0x17, 0x70),//AFE4400_ADCRSTSTCT3
        WriteReg(28, 0x00, 0x17, 0x70),//AFE4400_ADCRSTENDCT3
        WriteReg(29, 0x00, 0x1F, 0x3F),//AFE4400_PRPCOUNT
        WriteReg(30, 0x00, 0x01, 0x01),//AFE4400_CONTROL1

        WriteReg(31, 0x00, 0x00, 0x00),//AFE4400_SPARE1
        WriteReg(32, 0x00, 0x00, 0x00),//AFE4400_TIAGAIN
        WriteReg(33, 0x00, 0x00, 0x0A),//AFE4400_TIA_AMB_GAIN
        WriteReg(34, 0x01, 0x14, 0x29),//AFE4400_LEDCNTRL
        WriteReg(35, 0x02, 0x01, 0x00),//AFE4400_CONTROL2
        WriteReg(36, 0x00, 0x00, 0x00),//AFE4400_SPARE2
        WriteReg(37, 0x00, 0x00, 0x00),//AFE4400_SPARE3
        WriteReg(38, 0x00, 0x00, 0x00),//AFE4400_SPARE4
        WriteReg(39, 0x00, 0x00, 0x00),//AFE4400_RESERVED1
        WriteReg(40, 0x00, 0x00, 0x00),//AFE4400_RESERVED2
        WriteReg(41, 0x00, 0x00, 0x00),//AFE4400_ALARM
        WriteReg(42, 0x00, 0x00, 0x00),//AFE4400_LED2VAL

        WriteReg(43, 0x00, 0x00, 0x00),//AFE4400_ALED2VAL
        WriteReg(44, 0x00, 0x00, 0x00),//AFE4400_LED1VAL
        WriteReg(45, 0x00, 0x00, 0x00),//AFE4400_ALED1VAL
        WriteReg(46, 0x00, 0x00, 0x00),//AFE4400_LED2-ALED2VAL
        WriteReg(47, 0x00, 0x00, 0x00),//AFE4400_LED1-ALED1VAL
        WriteReg(48, 0x00, 0x00, 0x00),//AFE4400_DIAG

        // Setup SPI_READ
        WriteReg(0, 0x00, 0x00, 0x01),//AFE4400_DIAG
    };

    public AFE4400Thread(Mlx90620Listener listener, I2cManager i2c) {
        this.listener = listener;
        this.i2c = i2c;
        this.stopped = false;
        try {
            this.buf = new DataOutputStream(new FileOutputStream("/sdcard/plox.dat"));
        } catch (FileNotFoundException e) {
            Log.e(TAG, e.toString());
        }
    }

    @Override
    public void run() {
        String[] buses = i2c.getI2cBuses();
        if (buses.length == 0) {
            setText("no I2C buses found :(");
            return;
        }
        String bus = buses[0];

        int nsecs = 0;
        while (nsecs > 0) {
            setText("starting in " + nsecs + "...");
            nsecs--;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {}
        }

        I2cTransaction[] results;
        for (I2cTransaction txn: setupWrites) {
            try {
                results = i2c.performTransactions(bus, address, txn);
            } catch (IOException e) {
                setText("transaction error: " + e);
                return;
            }
        }

        while (!isStopped()) {
            results = null;
            boolean stop = true;
            String updateString = "";
            byte[] data;
            int LED1VAL;
            int LED2VAL;
            I2cTransaction[] txns0 = {
                I2cTransaction.newWrite(0x01,                 // Select SPI device 1
                                        0x2e,                 // LED2-ALED2VAL
                                        0xff, 0xff, 0xff),    // 3 dummy bytes
                I2cTransaction.newRead(4),
            };

            I2cTransaction[] txns1 = {
                I2cTransaction.newWrite(0x01,                 // Select SPI device 1
                                        0x2f,                 // LED2-ALED2VAL
                                        0xff, 0xff, 0xff),    // 3 dummy bytes
                I2cTransaction.newRead(4),
            };

            try {
                if (stop) {
                    for (I2cTransaction txn: txns0) {
                        results = i2c.performTransactions(bus, address, txn);
                    }
                } else {
                    results = i2c.performTransactions(bus, address, txns0);
                }
            } catch (IOException e) {
                setText("error while reading back: " + e);
                return;
            }

            if (stop)
                data = results[0].data;
            else
                data = results[1].data;

            LED1VAL = (((int)data[1]) << 16) |
                      (((int)data[2] & 0xFF) <<  8) |
                      (((int)data[3] & 0xFF));

            try {
                if (stop) {
                    for (I2cTransaction txn: txns1) {
                        results = i2c.performTransactions(bus, address, txn);
                    }
                } else {
                    results = i2c.performTransactions(bus, address, txns1);
                }
            } catch (IOException e) {
                setText("error while reading back: " + e);
                return;
            }

            if (stop)
                data = results[0].data;
            else
                data = results[1].data;

            LED2VAL = (((int)data[1]) << 16) |
                      (((int)data[2] & 0xFF) <<  8) |
                      (((int)data[3] & 0xFF));
            try {
                this.buf.writeInt(LED1VAL);
                this.buf.writeInt(LED2VAL);
            } catch (IOException e) {
                Log.e(TAG, "IOException: " + e);
            }

            updateString = String.format("%8d %8d", LED1VAL, LED2VAL);
            setText(updateString);
        //    try {
        //        Thread.sleep(50);
        //    } catch (InterruptedException e) {
        //        Log.e(TAG, e.toString());
        //    }
        }

        try {
            this.buf.close();
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e);
        }
    }


    private static I2cTransaction WriteReg(int reg, int b1, int b2, int b3) {
        return I2cTransaction.newWrite(0x01, reg, b1, b2, b3);
    }

    // ------------------------------------------------------------

    private void setText(String text) {
        this.listener.updateAFE4400Status(text);
    }

    public void requestStop() {
        this.stopped = true;
    }

    public boolean isStopped() {
        return this.stopped;
    }
}
