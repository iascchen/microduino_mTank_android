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

package me.iasc.microduino.mdrone.app;

import android.app.FragmentManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ToggleButton;
import com.jmedeisis.bugstick.JoystickListener;
import me.iasc.microduino.ble.BleAsyncTask;
import me.iasc.microduino.joypad.JoypadDroneCommand;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DroneControlActivity extends AbstractVehicleControlActivity {
    private final static String TAG = DroneControlActivity.class.getSimpleName();

    public static final int LR = 0, FB = 1, ROTATE = 2, POWER = 3;

    public int channelRightX = LR, channelRightY = POWER, channelLeftX = ROTATE, channelLeftY = FB,
            channelBtn1 = AUX1, channelBtn2 = AUX2, channelBtn3 = AUX3, channelBtn4 = AUX4;

    private ToggleButton toggleUnlock, toggleMicroCtrl;

//    private View.OnClickListener setupClickListener = new View.OnClickListener() {
//        @Override
//        public void onClick(View v) {
//            if (v.getId() == btnSetting.getId()) {
//                // Setup dialog
//                showSettingFragment();
//            } else if (v.getId() == btnCamera.getId()) {
//                captureMjpegView();
//            }
//        }
//    };

    protected JoystickListener joystickLeftListener = new JoystickListener() {
        @Override
        public void onDown() {
            // Nothing
        }

        @Override
        public void onDrag(float xOffset, float yOffset) {
            if (isDriving) {
                JoypadDroneCommand.changeChannel(channelLeftX, mapValue(xOffset));

                if (channelLeftY == POWER) {
                    JoypadDroneCommand.changeChannel(channelLeftY, mapFullValue(yOffset));
                } else {
                    JoypadDroneCommand.changeChannel(channelLeftY, mapValue(yOffset));
                }
                updateCommand();
            }
        }

        @Override
        public void onUp() {
            if (isDriving) {
                JoypadDroneCommand.changeChannel(channelLeftX, mapValue(0));
                if (channelLeftY != POWER) {
                    JoypadDroneCommand.changeChannel(channelLeftY, mapValue(0));
                }
                updateCommand();
            }
        }
    };

    protected JoystickListener joystickRightListener = new JoystickListener() {
        @Override
        public void onDown() {
            // Nothing
        }

        @Override
        public void onDrag(float xOffset, float yOffset) {
            if (isDriving) {
                JoypadDroneCommand.changeChannel(channelRightX, mapValue(xOffset));
                if (channelRightY == POWER) {
                    JoypadDroneCommand.changeChannel(channelRightY, mapFullValue(yOffset));
                } else {
                    JoypadDroneCommand.changeChannel(channelRightY, mapValue(yOffset));
                }
                updateCommand();
            }
        }

        @Override
        public void onUp() {
            if (isDriving) {
                JoypadDroneCommand.changeChannel(channelRightX, mapValue(0));
                if (channelRightY != POWER) {
                    JoypadDroneCommand.changeChannel(channelRightY, mapValue(0));
                }
                updateCommand();
            }
        }
    };

    protected View.OnTouchListener btnTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (v.getId() == button1.getId()) {
                    JoypadDroneCommand.changeChannel(channelBtn1, mapValue(BTN_PRESSED));
                    updateCommand();
                } else if (v.getId() == button2.getId()) {
                    JoypadDroneCommand.changeChannel(channelBtn2, mapValue(BTN_PRESSED));
                    updateCommand();
                } else if (v.getId() == button3.getId()) {
                    JoypadDroneCommand.changeChannel(channelBtn3, mapValue(BTN_PRESSED));
                    updateCommand();
                } else if (v.getId() == button4.getId()) {
                    JoypadDroneCommand.changeChannel(channelBtn4, mapValue(BTN_PRESSED));
                    updateCommand();
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                if (v.getId() == button1.getId()) {
                    JoypadDroneCommand.changeChannel(channelBtn1, mapValue(BTN_UNPRESSED));
                    updateCommand();
                } else if (v.getId() == button2.getId()) {
                    JoypadDroneCommand.changeChannel(channelBtn2, mapValue(BTN_UNPRESSED));
                    updateCommand();
                } else if (v.getId() == button3.getId()) {
                    JoypadDroneCommand.changeChannel(channelBtn3, mapValue(BTN_UNPRESSED));
                    updateCommand();
                } else if (v.getId() == button4.getId()) {
                    JoypadDroneCommand.changeChannel(channelBtn4, mapValue(BTN_UNPRESSED));
                    updateCommand();
                }
            }

            return false;
        }
    };

    private View.OnClickListener toggleClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getId() == toggleMicroCtrl.getId()) {
                if (toggleMicroCtrl.isChecked()) {
                    threshold = JOYSTICK_MICRO_THRESHOLD;
                } else {
                    threshold = JOYSTICK_FULL_THRESHOLD;
                }
            } else if (v.getId() == toggleUnlock.getId()) {
                if (toggleUnlock.isChecked()) {
                    LockTask task = new LockTask();
                    task.execute(LockTask.UNLOCK);
                } else {
                    LockTask task = new LockTask();
                    task.execute(LockTask.LOCK);
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.drone_control);

        toggleUnlock = (ToggleButton) findViewById(R.id.toggle_unlock);
        toggleMicroCtrl = (ToggleButton) findViewById(R.id.toggle_microCtrl);

        toggleUnlock.setOnClickListener(toggleClickListener);
        toggleMicroCtrl.setOnClickListener(toggleClickListener);

        super.onCreate(savedInstanceState);

        joystickLeft.setJoystickListener(joystickLeftListener);
        joystickRight.setJoystickListener(joystickRightListener);

        button1.setOnTouchListener(btnTouchListener);
        button2.setOnTouchListener(btnTouchListener);
        button3.setOnTouchListener(btnTouchListener);
        button4.setOnTouchListener(btnTouchListener);

        btnSetting.setOnClickListener(setupClickListener);
//        btnCamera.setOnClickListener(setupClickListener);

//        // Initializing the gravity vector to zero.
//        gravity = new float[3];
//        gravity[0] = 0;
//        gravity[1] = 0;
//        gravity[2] = 0;
//
//        settings = PreferenceManager.getDefaultSharedPreferences(activity);
//        readSetting();
//
//        commandView = (TextView) findViewById(R.id.command);
//        statusView = (TextView) findViewById(R.id.status);
//
//        camTarget = (ImageView) findViewById(R.id.centerView);
////        camView = (MjpegView) findViewById(R.id.camView);
//
//        button1 = (FloatingActionButton) findViewById(R.id.aux_1);
//        button2 = (FloatingActionButton) findViewById(R.id.aux_2);
//        button3 = (FloatingActionButton) findViewById(R.id.aux_3);
//        button4 = (FloatingActionButton) findViewById(R.id.aux_4);
//
//        button1.setOnTouchListener(btnTouchListener);
//        button2.setOnTouchListener(btnTouchListener);
//        button3.setOnTouchListener(btnTouchListener);
//        button4.setOnTouchListener(btnTouchListener);
//
//        btnSetting = (FloatingActionButton) findViewById(R.id.setup);
//        btnCamera = (FloatingActionButton) findViewById(R.id.button_camera);
//
//        btnSetting.setOnClickListener(setupClickListener);
//        btnCamera.setOnClickListener(setupClickListener);

//        // Initializing the accelerometer stuff
//        // Register this as SensorEventListener
//        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
//        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
//
//        stickLeftInfo = (ImageView) findViewById(R.id.info_left);
//        stickRightInfo = (ImageView) findViewById(R.id.info_right);
//
//        joystickLeft = (Joystick) findViewById(R.id.joystick_left);
//        stickLeft = (Button) findViewById(R.id.stick_left);
//        joystickLeft.setJoystickListener(new JoystickListener() {
//            @Override
//            public void onDown() {
//                // Nothing
//            }
//
//            @Override
//            public void onDrag(float xOffset, float yOffset) {
//                if (isDriving) {
//                    JoypadDroneCommand.changeChannel(channelLeftX, mapValue(xOffset));
//                    JoypadDroneCommand.changeChannel(channelLeftY, mapValue(yOffset));
//                    updateCommand();
//                }
//            }
//
//            @Override
//            public void onUp() {
//                if (isDriving) {
//                    JoypadDroneCommand.changeChannel(channelLeftX, mapValue(0));
//                    JoypadDroneCommand.changeChannel(channelLeftY, mapValue(0));
//                    updateCommand();
//                }
//            }
//        });
//
//        joystickRight = (Joystick) findViewById(R.id.joystick_right);
//        stickRight = (Button) findViewById(R.id.stick_right);
//        joystickRight.setJoystickListener(new JoystickListener() {
//            @Override
//            public void onDown() {
//                // Nothing
//            }
//
//            @Override
//            public void onDrag(float xOffset, float yOffset) {
//                if (isDriving) {
//                    JoypadDroneCommand.changeChannel(channelRightX, mapValue(xOffset));
//                    JoypadDroneCommand.changeChannel(channelRightY, mapValue(yOffset));
//                    updateCommand();
//                }
//            }
//
//            @Override
//            public void onUp() {
//                if (isDriving) {
//                    JoypadDroneCommand.changeChannel(channelRightX, mapValue(0));
//                    JoypadDroneCommand.changeChannel(channelRightY, mapValue(0));
//                    updateCommand();
//                }
//            }
//        });
//
//        // Getting a WakeLock. This insures that the phone does not sleep
//        // while driving the robot.
//        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
//        wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "mTank");
//        wl.acquire();
//
//        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
//        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
//
//        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
//        if ((currDeviceAddress != null) && (currDeviceAddress.trim().length() == 0)
//                && (mBluetoothLeService != null)) {
//            final boolean result = mBluetoothLeService.connect(currDeviceAddress);
//            Log.d(TAG, "Connect request result=" + result);
//        }
//
//        changeHand(rightHand);

//        Log.d(TAG, currWebCamAdr);
//        new DoRead().execute(currWebCamAdr);
    }

    protected void buttonEnable(boolean enable) {
        button1.setEnabled(enable);
        button2.setEnabled(enable);
        button3.setEnabled(enable);
        button4.setEnabled(enable);

        toggleUnlock.setEnabled(enable);
        toggleMicroCtrl.setEnabled(enable);
    }

//    private void unlockDrone() {
//        try {
//            Date now = new Date();
//            String filename = now.getTime() + ".jpg";
//            File file = new File(getFilesDir(), filename);
//            Bitmap bitmap = camView.capture(file.getAbsolutePath());
//            if (bitmap != null) {
//                MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, file.getAbsolutePath(), "Captured by Microduino mTank");
//            }
//        } catch (Exception e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//    }

    protected void changeHand(boolean isRight) {
        this.rightHand = isRight;
        if (rightHand) {
            channelRightX = LR;
            channelRightY = POWER;
            channelLeftX = ROTATE;
            channelLeftY = FB;

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

//            // according only one button used - fire
//            button1.setVisibility(View.VISIBLE);
//            button2.setVisibility(View.INVISIBLE);
//            button3.setVisibility(View.INVISIBLE);
//            button4.setVisibility(View.INVISIBLE);

//            toggleUnlock.setVisibility(View.VISIBLE);
//            toggleMicroCtrl.setVisibility(View.VISIBLE);

        } else {
            channelRightX = LR;
            channelRightY = FB;
            channelLeftX = ROTATE;
            channelLeftY = POWER;

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

//            // according only one button used - fire
//            button1.setVisibility(View.INVISIBLE);
//            button2.setVisibility(View.INVISIBLE);
//            button3.setVisibility(View.INVISIBLE);
//            button4.setVisibility(View.VISIBLE);

//            toggleUnlock.setVisibility(View.INVISIBLE);
//            toggleMicroCtrl.setVisibility(View.INVISIBLE);
        }
    }

    protected void updateCommand() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                commandView.setText(JoypadDroneCommand.toHexString());
            }
        });
    }

    protected void updateReadyState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusView.setText(getString(resourceId));

                if (resourceId == R.string.ready) {
                    characteristicReady = true;

                    toggleUnlock.setVisibility(View.VISIBLE);
                    toggleMicroCtrl.setVisibility(View.VISIBLE);

                    // cmdSendTimer = startSentCmdTimer(0, bleTimerInterval);

                    toastMessage(getString(resourceId));
                } else {
                    characteristicReady = false;

                    toggleUnlock.setVisibility(View.INVISIBLE);
                    toggleMicroCtrl.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        LockTask task = new LockTask();
        task.execute(LockTask.LOCK);

        // stopTimer(cmdSendTimer);

        super.onDestroy();
    }

    /////////////////////
    // Setting Fragment
    /////////////////////

    protected void showSettingFragment() {
        FragmentManager fm = getFragmentManager();
        DroneSettingFragment frag = DroneSettingFragment.newInstance();
        frag.show(fm, "Drone Setting");
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

//    public static boolean sendingLock = false;
//    // long last_time0 = (new Date()).getTime() ;

    protected Timer startSentCmdTimer(long delay, long period) {
        final Timer mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                long time0 = (new Date()).getTime(), time1 = 0;
                // Log.v("BBuffer sub : between 2 msgs : ", ""+ (time0 - last_time0));
                // last_time0 = time0;

                if (!sendingLock) {
                    sendingLock = true;

                    byte[] cmd = JoypadDroneCommand.compose();

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

                                Log.v("BBuffer sub", offset + " : " + JoypadDroneCommand.byteArrayToHexString(buffer));
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

    private class LockTask extends BleAsyncTask {
        public static final String UNLOCK = "U", LOCK = "L";

        private final int WAIT_INTERVAL = 2000;

        protected int getInterval() {
            return WAIT_INTERVAL;
        }

        @Override
        protected String doInBackground(String... params) {
            String ret = params[0];
            if (ret.equals(UNLOCK)) {
                isDriving = true;
                cmdSendTimer = startSentCmdTimer(0, bleTimerInterval);

                JoypadDroneCommand.resetChannel(JoypadDroneCommand.UNLOCK_CMD);
            } else {
                JoypadDroneCommand.resetChannel(JoypadDroneCommand.LOCK_CMD);
            }

            updateCommand();
            // Should send unlock cmd 2 seconds
            wait_ble(getInterval());

            return ret;
        }

        @Override
        protected void onPostExecute(String result) {
            JoypadDroneCommand.resetChannel(JoypadDroneCommand.NORMAL_CMD);
            updateCommand();

            if (result.equals(LOCK)) {
                isDriving = false;
                stopTimer(cmdSendTimer);

                toastMessage("Locked");
            } else {
                toastMessage("Unlocked");
            }
        }
    }
}