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

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.*;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import me.iasc.microduino.ble.BleAsyncTask;
import me.iasc.microduino.ble.BluetoothLeService;
import me.iasc.microduino.ble.MyGattCharacteristic;
import me.iasc.microduino.ble.MyGattService;
import me.iasc.microduino.joypad.JoypadTankCommand;

public abstract class AbstractBleControlActivity extends Activity {
    private final static String TAG = AbstractBleControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    public static int BLE_BLOCK_SEND_INTERVAL = 5;
    public static int BLE_MSG_SEND_INTERVAL = 20;
    public static int BLE_MSG_BUFFER_LEN = 16;

    protected String currDeviceName, currDeviceAddress;

    protected Activity activity;

    protected TextView statusView, commandView;
//    protected TextView isSerial, mConnectionState;
//    protected ImageView infoButton;

    public BluetoothLeService mBluetoothLeService;
    protected BluetoothGattCharacteristic characteristicTX, characteristicRX;
    protected boolean mConnected = false, characteristicReady = false;

    protected StringBuilder msgBuffer;

    // Code to manage Service lifecycle.
    protected final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(currDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    protected final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                BluetoothGattService serialService = mBluetoothLeService.getGattService(MyGattService.SOFT_SERIAL_SERVICE);
                if (serialService == null) {
                    toastMessage(getString(R.string.without_service));
                    return;
                }

                EnableNotificationTask task = new EnableNotificationTask();
                task.execute();

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getByteArrayExtra(mBluetoothLeService.EXTRA_DATA));
            }
        }
    };

    protected static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activity = this;

        final Intent intent = getIntent();
        currDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        currDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        commandView = (TextView) findViewById(R.id.command);
        statusView = (TextView) findViewById(R.id.status);

        // Sets up UI references.

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if ((currDeviceAddress != null) && (currDeviceAddress.trim().length() == 0)
                && (mBluetoothLeService != null)) {
            final boolean result = mBluetoothLeService.connect(currDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mGattUpdateReceiver);

        unbindService(mServiceConnection);
        mBluetoothLeService = null;

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(currDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                statusView.setText(resourceId);
            }
        });
    }

    protected void updateReadyState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                characteristicReady = true;

                statusView.setText(getString(resourceId));
                toastMessage(getString(resourceId));
            }
        });
    }

    protected void displayData(byte[] data) {
        if (data != null) {
            Log.v(TAG, "BLE Return Data : " + JoypadTankCommand.byteArrayToHexString(data));
        }
    }

    public void wait_ble(int i) {
        try {
            Thread.sleep(i);
        } catch (Exception e) {
            // ignore
        }
    }

    protected void sendMessage(byte[] buffer) {
        // Log.v("BBuffer", JoypadCarCommand.byteArrayToHexString(buffer));

        if (characteristicReady && (mBluetoothLeService != null)
                && (characteristicTX != null) && (characteristicRX != null)) {
            characteristicTX.setValue(buffer);
            mBluetoothLeService.writeCharacteristic(characteristicTX);
        } else {
            Log.v("sendMessage", getString(R.string.disconnected));
        }
    }

    public void toastMessage(String msg) {
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
    }

    /**
     * EnableNotificationTask       enable all BEL notification, and should be read BLECharacteristics.
     * <p/>
     * If you want to start some timer, please add them in method onPostExecute
     * <p/>
     * <code>
     * private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
     *
     * @Override public void onReceive(Context context, Intent intent) {
     * final String action = intent.getAction();
     * String address = intent.getStringExtra(BluetoothLeServiceN.EXTRA_ADDRESS);
     * assert (currDeviceAddress.equals(address));
     * <p/>
     * if (BluetoothLeServiceN.ACTION_GATT_CONNECTED.equals(action)) {
     * ...
     * }
     * else if (BluetoothLeServiceN.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
     * <p/>
     * Log.v(TAG, "BroadcastReceiver.ACTION_GATT_SERVICES_DISCOVERED");
     * <p/>
     * EnableNotificationTask task = new EnableNotificationTask();
     * task.execute(address);
     * }
     * </code>
     */
    private class EnableNotificationTask extends BleAsyncTask {
        private final int WAIT_INTERVAL = BLE_MSG_SEND_INTERVAL;

        protected int getInterval() {
            return WAIT_INTERVAL;
        }

        @Override
        protected String doInBackground(String... params) {
            boolean ret = false;

            // TODO: Please add your code, enable ble notification
            if (mBluetoothLeService != null) {
                Log.d(TAG, "EnableNotificationTask");

                ret = mBluetoothLeService.enableGattCharacteristicNotification(
                        MyGattService.SOFT_SERIAL_SERVICE,
                        MyGattCharacteristic.MD_RX_TX, true);
                if (ret) {
                    waitIdle();
                } else {
                    Log.d(TAG, "MD_RX_TX : " + ret);
                    return "Failed";
                }
            }
            return "Done";
        }

        @Override
        protected void onPostExecute(String result) {
            Log.v(TAG, "EnableNotificationTask onPostExecute called :" + " , " + result);

            if (result.equals("Failed")) {
                // updateTextInfo(getString(R.string.without_service), false);
                return;
            }

            characteristicTX = mBluetoothLeService.getGattCharacteristic(MyGattService.SOFT_SERIAL_SERVICE, MyGattCharacteristic.MD_RX_TX);
            characteristicRX = characteristicTX;

            if (characteristicTX != null) {
                updateReadyState(R.string.ready);
            } else {
                updateReadyState(R.string.without_service);
            }
        }
    }
}