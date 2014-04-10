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

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.hardware.I2cManager;
import android.hardware.I2cTransaction;
import android.widget.TextView;
import java.io.IOException;
import android.os.Handler;
import android.util.Log;

public class MainActivity extends Activity implements Mlx90620Listener{

    TextView textView;
    AFE4400Thread thread;
    Handler handler;
    private static final String TAG = "araplox";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView)findViewById(R.id.textView);
        handler = new Handler();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onStart(){
        super.onStart();
        I2cManager i2c = (I2cManager) getSystemService(I2C_SERVICE);
        thread = new AFE4400Thread(this, i2c);
        thread.start();
        Log.d(TAG, "onstart");
    }

    @Override
    protected void onStop() {
    	super.onStop();
    	thread.requestStop();
        Log.d(TAG, "onstop");
    }

    // -- AFE4400 API --------------------------------------------------------

    @Override
    public void updateAFE4400Status(String text) {
        final String t = text;
        handler.post(new Runnable() {
                @Override
                public void run() {
                    textView.setText(t);
                }
            });
    }
}
