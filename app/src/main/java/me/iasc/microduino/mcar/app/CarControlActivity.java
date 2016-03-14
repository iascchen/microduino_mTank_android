/*
 * Copyright (C) 2015 Iasc CHEN
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

package me.iasc.microduino.mcar.app;

import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.camera.simplemjpeg.MjpegInputStream;
import com.camera.simplemjpeg.MjpegView;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.jmedeisis.bugstick.Joystick;
import com.jmedeisis.bugstick.JoystickListener;
import me.iasc.microduino.ble.BluetoothLeService;
import me.iasc.microduino.joypad.JoypadCarCommand;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class CarControlActivity extends AbstractBleControlActivity
        implements SensorEventListener, SettingFragment.OnFragmentInteractionListener {

    private final static String TAG = CarControlActivity.class.getSimpleName();

    static boolean rightHand = true;
    static boolean webCam = false;
    static boolean webSuspending = false;

    static int PROCESSING_TIME = 10;

    static String currWebCamAdr;
    static int threshold = 500,
            msgSendInterval = BLE_MSG_SEND_INTERVAL, blockSendInterval = BLE_BLOCK_SEND_INTERVAL,
            bleTimerInterval = BLE_MSG_SEND_INTERVAL + BLE_BLOCK_SEND_INTERVAL + PROCESSING_TIME;

    public static final int BTN_UNPRESSED = -1, BTN_PRESSED = 1;

    public static final int STEERING = 0, THROTTLE = 1, ROLL = 2, PITCH = 3,
            AUX1 = 4, AUX2 = 5, AUX3 = 6, AUX4 = 7;
    public int channelRightX = STEERING, channelRightY = THROTTLE, channelLeftX = ROLL, channelLeftY = PITCH,
            channelBtn1 = AUX1, channelBtn2 = AUX2, channelBtn3 = AUX3, channelBtn4 = AUX4;

    static SharedPreferences settings;

    private FloatingActionButton button1, button2, button3, button4;
    private FloatingActionButton btnSetting, btnCamera;
    private Joystick joystickLeft, joystickRight;
    private Button stickLeft, stickRight;
    private ImageView stickLeftInfo, stickRightInfo, camTarget;

    MjpegView camView;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    private static Timer cmdSendTimer;

    private float gravity[];

    private static boolean isDriving = false;

    private PowerManager.WakeLock wl;

    private View.OnClickListener setupClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getId() == btnSetting.getId()) {
                // Setup dialog
                showSettingFragment();
            } else if (v.getId() == btnCamera.getId()) {
                captureMjpegView();
            }
        }
    };

    private View.OnTouchListener btnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (v.getId() == button1.getId()) {
                    JoypadCarCommand.changeChannel(channelBtn1, mapValue(BTN_PRESSED));
                    updateCommand();
                } else if (v.getId() == button2.getId()) {
                    JoypadCarCommand.changeChannel(channelBtn2, mapValue(BTN_PRESSED));
                    updateCommand();
                } else if (v.getId() == button3.getId()) {
                    JoypadCarCommand.changeChannel(channelBtn3, mapValue(BTN_PRESSED));
                    updateCommand();
                } else if (v.getId() == button4.getId()) {
                    JoypadCarCommand.changeChannel(channelBtn4, mapValue(BTN_PRESSED));
                    updateCommand();
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                if (v.getId() == button1.getId()) {
                    JoypadCarCommand.changeChannel(channelBtn1, mapValue(BTN_UNPRESSED));
                    updateCommand();
                } else if (v.getId() == button2.getId()) {
                    JoypadCarCommand.changeChannel(channelBtn2, mapValue(BTN_UNPRESSED));
                    updateCommand();
                } else if (v.getId() == button3.getId()) {
                    JoypadCarCommand.changeChannel(channelBtn3, mapValue(BTN_UNPRESSED));
                    updateCommand();
                } else if (v.getId() == button4.getId()) {
                    JoypadCarCommand.changeChannel(channelBtn4, mapValue(BTN_UNPRESSED));
                    updateCommand();
                }
            }

            return false;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.car_control);
        super.onCreate(savedInstanceState);

        // Initializing the gravity vector to zero.
        gravity = new float[3];
        gravity[0] = 0;
        gravity[1] = 0;
        gravity[2] = 0;

        settings = PreferenceManager.getDefaultSharedPreferences(activity);
        readSetting();

        commandView = (TextView) findViewById(R.id.command);
        statusView = (TextView) findViewById(R.id.status);

        camTarget = (ImageView) findViewById(R.id.centerView);
        camView = (MjpegView) findViewById(R.id.camView);

        button1 = (FloatingActionButton) findViewById(R.id.aux_1);
        button2 = (FloatingActionButton) findViewById(R.id.aux_2);
        button3 = (FloatingActionButton) findViewById(R.id.aux_3);
        button4 = (FloatingActionButton) findViewById(R.id.aux_4);

        button1.setOnTouchListener(btnTouchListener);
        button2.setOnTouchListener(btnTouchListener);
        button3.setOnTouchListener(btnTouchListener);
        button4.setOnTouchListener(btnTouchListener);

        btnSetting = (FloatingActionButton) findViewById(R.id.setup);
        btnCamera = (FloatingActionButton) findViewById(R.id.button_camera);

        btnSetting.setOnClickListener(setupClickListener);
        btnCamera.setOnClickListener(setupClickListener);

        // Initializing the accelerometer stuff
        // Register this as SensorEventListener
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);

        stickLeftInfo = (ImageView) findViewById(R.id.info_left);
        stickRightInfo = (ImageView) findViewById(R.id.info_right);

        joystickLeft = (Joystick) findViewById(R.id.joystick_left);
        stickLeft = (Button) findViewById(R.id.stick_left);
        joystickLeft.setJoystickListener(new JoystickListener() {
            @Override
            public void onDown() {
                // Nothing
            }

            @Override
            public void onDrag(float xOffset, float yOffset) {
                if (isDriving) {
                    JoypadCarCommand.changeChannel(channelLeftX, mapValue(xOffset));
                    JoypadCarCommand.changeChannel(channelLeftY, mapValue(yOffset));
                    updateCommand();
                }
            }

            @Override
            public void onUp() {
                if (isDriving) {
                    JoypadCarCommand.changeChannel(channelLeftX, mapValue(0));
                    JoypadCarCommand.changeChannel(channelLeftY, mapValue(0));
                    updateCommand();
                }
            }
        });

        joystickRight = (Joystick) findViewById(R.id.joystick_right);
        stickRight = (Button) findViewById(R.id.stick_right);
        joystickRight.setJoystickListener(new JoystickListener() {
            @Override
            public void onDown() {
                // Nothing
            }

            @Override
            public void onDrag(float xOffset, float yOffset) {
                if (isDriving) {
                    JoypadCarCommand.changeChannel(channelRightX, mapValue(xOffset));
                    JoypadCarCommand.changeChannel(channelRightY, mapValue(yOffset));
                    updateCommand();
                }
            }

            @Override
            public void onUp() {
                if (isDriving) {
                    JoypadCarCommand.changeChannel(channelRightX, mapValue(0));
                    JoypadCarCommand.changeChannel(channelRightY, mapValue(0));
                    updateCommand();
                }
            }
        });

        // Getting a WakeLock. This insures that the phone does not sleep
        // while driving the robot.
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "mTank");
        wl.acquire();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if ((currDeviceAddress != null) && (currDeviceAddress.trim().length() == 0)
                && (mBluetoothLeService != null)) {
            final boolean result = mBluetoothLeService.connect(currDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }

        changeHand(rightHand);

        Log.d(TAG, currWebCamAdr);
        new DoRead().execute(currWebCamAdr);
    }

    private void captureMjpegView() {
        try {
            Date now = new Date();
            String filename = now.getTime() + ".jpg";
            File file = new File(getFilesDir(), filename);
            Bitmap bitmap = camView.capture(file.getAbsolutePath());
            if (bitmap != null) {
                MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, file.getAbsolutePath(), "Captured by Microduino mTank");
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void readSetting() {
        rightHand = settings.getBoolean(SettingFragment.SET_RIGHT_HAND, true);

        blockSendInterval = settings.getInt(SettingFragment.SET_BLOCK_SEND_INTERVAL, BLE_BLOCK_SEND_INTERVAL);
        msgSendInterval = settings.getInt(SettingFragment.SET_BLE_SEND_INTERVAL, BLE_MSG_SEND_INTERVAL);
        bleTimerInterval = blockSendInterval + msgSendInterval + PROCESSING_TIME;

        currDeviceAddress = settings.getString(SettingFragment.SET_BLE_ADDRESS, null);
        String webCam = settings.getString(SettingFragment.SET_WEB_CAM_ADDRESS, "192.168.1.1:8080");
        currWebCamAdr = "http://" + webCam + "/?action=stream";
    }

    private void changeHand(boolean isRight) {
        this.rightHand = isRight;
        if (rightHand) {
            channelRightX = STEERING;
            channelRightY = THROTTLE;
            channelLeftX = ROLL;
            channelLeftY = PITCH;

            channelBtn1 = AUX1;
            channelBtn2 = AUX2;
            channelBtn3 = AUX3;
            channelBtn4 = AUX4;

            button1.setIcon(R.drawable.icon_fire);
            button2.setIcon(R.drawable.autolock);
            button3.setIcon(R.drawable.tracking);
            button4.setIcon(R.drawable.intelligence);

            stickLeftInfo.setImageDrawable(getResources().getDrawable(R.drawable.icon_rotate));
            stickRightInfo.setImageDrawable(getResources().getDrawable(R.drawable.icon_move));

            // according only one button used - fire
            button1.setVisibility(View.VISIBLE);
            button2.setVisibility(View.INVISIBLE);
            button3.setVisibility(View.INVISIBLE);
            button4.setVisibility(View.INVISIBLE);

        } else {
            channelRightX = ROLL;
            channelRightY = PITCH;
            channelLeftX = STEERING;
            channelLeftY = THROTTLE;

            channelBtn1 = AUX4;
            channelBtn2 = AUX3;
            channelBtn3 = AUX2;
            channelBtn4 = AUX1;

            button1.setIcon(R.drawable.intelligence);
            button2.setIcon(R.drawable.tracking);
            button3.setIcon(R.drawable.autolock);
            button4.setIcon(R.drawable.icon_fire);

            stickLeftInfo.setImageDrawable(getResources().getDrawable(R.drawable.icon_move));
            stickRightInfo.setImageDrawable(getResources().getDrawable(R.drawable.icon_rotate));

            // according only one button used - fire
            button1.setVisibility(View.INVISIBLE);
            button2.setVisibility(View.INVISIBLE);
            button3.setVisibility(View.INVISIBLE);
            button4.setVisibility(View.VISIBLE);
        }
    }

    private void buttonEnable(boolean enable) {
        button1.setEnabled(enable);
        button2.setEnabled(enable);
        button3.setEnabled(enable);
        button4.setEnabled(enable);
    }

    private short mapValue(float offset) {
        return (short) (offset * threshold + 1500);
    }

    protected void updateCommand() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                commandView.setText(JoypadCarCommand.toHexString());
            }
        });
    }

    protected void updateReadyState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (resourceId == R.string.ready) {
                    characteristicReady = true;
                    statusView.setText(getString(resourceId));

                    cmdSendTimer = startSentCmdTimer(0, bleTimerInterval);

                    toastMessage(getString(resourceId));
                }
            }
        });
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // We don't do anything when the accuracy of the accelerometer changes.
    }

    public void onSensorChanged(SensorEvent event) {
        // This function is called repeatedly. The tempo is set when the listener is register
        // see onCreate() method.

        // Lowpass filter the gravity vector so that sudden movements are filtered.
        float alpha = (float) 0.8;
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        // Normalize the gravity vector and rescale it so that every component fits one byte.
        float size = (float) Math.sqrt(Math.pow(gravity[0], 2) + Math.pow(gravity[1], 2) + Math.pow(gravity[2], 2));
        byte x = (byte) (128 * gravity[0] / size);
        byte y = (byte) (128 * gravity[1] / size);
        byte z = (byte) (128 * gravity[2] / size);

        // Update the GUI
        updateUIXyz(x, y, z);
    }

    protected void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusView.setText(resourceId);
                if (resourceId == R.string.connected) {
                    isDriving = true;
                    buttonEnable(isDriving);
                } else if (resourceId == R.string.disconnected) {
                    isDriving = false;
                    buttonEnable(isDriving);
                }
            }
        });
    }

    void updateUIXyz(final byte x, final byte y, final byte z) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusView.setText("X: " + Integer.toString(x) + ", Y: " + Integer.toString(y) + ", Z: " + Integer.toString(z));
            }
        });
    }

    @Override
    protected void onResume() {
        if (camView != null) {
            if (webSuspending) {
                new DoRead().execute(currWebCamAdr);
                webSuspending = false;
            }
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (camView != null) {
            if (camView.isStreaming()) {
                camView.stopPlayback();
                webSuspending = true;
            }
        }

        if (mBluetoothLeService != null) {
            mBluetoothLeService.close();
        }
    }

    @Override
    protected void onDestroy() {
        stopTimer(cmdSendTimer);

        super.onDestroy();
    }

    /////////////////////
    // Setting Fragment
    /////////////////////

    private void showSettingFragment() {
        FragmentManager fm = getFragmentManager();
        SettingFragment frag = SettingFragment.newInstance();
        frag.show(fm, "Setting");
    }

    @Override
    public void settingChanged() {
        Log.v(TAG, "Update Setting");
        readSetting();
        changeHand(rightHand);

        stopTimer(cmdSendTimer);

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null && currDeviceAddress != null) {
            final boolean result = mBluetoothLeService.connect(currDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }

        Log.d(TAG, "Connect Success");
    }

    /////////////////////
    // Timer for sending msg
    /////////////////////

    public static boolean sendingLock = false;
    // long last_time0 = (new Date()).getTime() ;

    private Timer startSentCmdTimer(long delay, long period) {
        final Timer mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                long time0 = (new Date()).getTime(), time1 = 0;
                // Log.v("BBuffer sub : between 2 msgs : ", ""+ (time0 - last_time0));
                // last_time0 = time0;

                if (!sendingLock) {
                    sendingLock = true;

                    byte[] cmd = JoypadCarCommand.compose();

                    try {
                        // long time0_0 = (new Date()).getTime(), time1_0 = 0;

                        // Microduino BLE firmware only can use 18 bytes buffer, so I have to split the message.
                        int bufferlen = BLE_MSG_BUFFER_LEN;
                        byte[] buffer;

                        synchronized (cmd) {
                            for (int offset = 0; offset < cmd.length; offset += BLE_MSG_BUFFER_LEN) {
                                if (offset > 0) {
                                    wait_ble(blockSendInterval);
                                }

                                bufferlen = Math.min(BLE_MSG_BUFFER_LEN, cmd.length - offset);
                                buffer = new byte[bufferlen];

                                System.arraycopy(cmd, offset, buffer, 0, bufferlen);

                                // Log.v("BBuffer sub", offset + " : " + JoypadCarCommand.byteArrayToHexString(buffer));
                                sendMessage(buffer);

                                buffer = null;

                                // time1_0 = (new Date()).getTime();
                                // Log.v("BBuffer sub : block : ", offset + " : " + (time1_0 - time0_0));
                            }
                            wait_ble(msgSendInterval);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    sendingLock = false;
                }

                // time1 = (new Date()).getTime();
                // Log.v("BBuffer sub : msg : ", ""+ (time1 - time0));
            }
        }, delay, period);
        return mTimer;
    }

    private void stopTimer(Timer mTimer) {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    /////////////////////
    // Read Mjpeg Stream
    /////////////////////

    private class DoRead extends AsyncTask<String, Void, MjpegInputStream> {
        protected MjpegInputStream doInBackground(String... url) {
            //TODO: if camera has authentication deal with it and don't just not work
            HttpResponse res = null;
            DefaultHttpClient httpclient = new DefaultHttpClient();
            HttpParams httpParams = httpclient.getParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, 5 * 1000);
            HttpConnectionParams.setSoTimeout(httpParams, 5 * 1000);
            Log.d(TAG, "1. Sending http request");
            try {
                res = httpclient.execute(new HttpGet(URI.create(url[0])));
                Log.d(TAG, "2. Request finished, status = " + res.getStatusLine().getStatusCode());
                if (res.getStatusLine().getStatusCode() == 401) {
                    //You must turn off camera User Access Control before this will work
                    return null;
                }
                return new MjpegInputStream(res.getEntity().getContent());
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                Log.d(TAG, "Request failed-ClientProtocolException" + e);
                //Error connecting to camera
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "Request failed-IOException" + e);
                //Error connecting to camera
            }
            return null;
        }

        protected void onPostExecute(MjpegInputStream result) {
            camView.setSource(result);
            if (result != null) {
                webCam = true;

                result.setSkip(1);

                camView.setVisibility(View.VISIBLE);
                camView.setDisplayMode(MjpegView.SIZE_FIT_WIDTH);
                camView.showFps(true);
            } else {
                webCam = false;

                camView.setVisibility(View.GONE);
                camView.freeCameraMemory();
            }

            if (webCam) {
                camTarget.setVisibility(View.VISIBLE);
                btnCamera.setVisibility(View.VISIBLE);
            } else {
                camTarget.setVisibility(View.INVISIBLE);
                btnCamera.setVisibility(View.INVISIBLE);
            }
        }
    }
}