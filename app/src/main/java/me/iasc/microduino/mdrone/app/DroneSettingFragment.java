package me.iasc.microduino.mdrone.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.*;
import me.iasc.microduino.ble.BluetoothLeService;

import java.util.ArrayList;

public class DroneSettingFragment extends AbstractSettingFragment
{
    private static final String TAG = DroneSettingFragment.class.getSimpleName();

    public static final String SET_RIGHT_HAND = "drone_right_HAND";
    public static final String SET_BLE_ADDRESS = "drone_ble_ADDRESS";
    public static final String SET_WEB_CAM_ADDRESS = "drone_web_cam_ADDRESS";

    public static DroneSettingFragment newInstance() {
        DroneSettingFragment fragment = new DroneSettingFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.drone_setting, container, false);

        super.onCreateView(inflater,container, savedInstanceState );

        return view;
    }

}
