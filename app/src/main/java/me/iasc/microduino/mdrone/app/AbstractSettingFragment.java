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

public abstract class AbstractSettingFragment extends DialogFragment {
    private static final String TAG = AbstractSettingFragment.class.getSimpleName();

    public static final String SET_RIGHT_HAND = "right_HAND";
    public static final String SET_BLE_ADDRESS = "curr_ble_ADDRESS";
    public static final String SET_WEB_CAM_ADDRESS = "curr_web_cam_ADDRESS";

    protected AbstractVehicleControlActivity parentActivity;
    protected OnFragmentInteractionListener mListener;

    static SharedPreferences settings;
    static SharedPreferences.Editor editor;

    protected View view = null;

    protected boolean rightHand = true;
    protected String currBleAdr, currWebCamAdr;

    protected Switch rightSwitch;
    protected Button scanButton, saveButton;
    protected EditText editBleAddr, editWebCamAddr;

    ///////////////////////
    // Bluttooth Scan

    protected BluetoothLeService mBluetoothLeService;

    protected static final long SCAN_PERIOD = 10000;  // Stops scanning after 10 seconds.

    protected static boolean bleIsScanning;
    protected Handler mHandler;

    protected LeDeviceListAdapter mLeDeviceListAdapter;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SettingFragment.
     */
    public static AbstractSettingFragment newInstance() {
        return null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        // View view = inflater.inflate(R.layout.tank_setting, container, false);

        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);

        parentActivity = (AbstractVehicleControlActivity) getActivity();

        settings = PreferenceManager.getDefaultSharedPreferences(parentActivity);
        readSetting();

        mBluetoothLeService = parentActivity.mBluetoothLeService;

        rightSwitch = (Switch) view.findViewById(R.id.rightHand);

        editBleAddr = (EditText) view.findViewById(R.id.bleAdr);
        editBleAddr.setEnabled(false);
        scanButton = (Button) view.findViewById(R.id.buttonScan);

        editWebCamAddr = (EditText) view.findViewById(R.id.webCamAdr);

        saveButton = (Button) view.findViewById(R.id.btnSave);

        rightSwitch.setChecked(rightHand);

        editBleAddr.setText(currBleAdr);
        editWebCamAddr.setText(currWebCamAdr);

        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogSelectScanBleDevice();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSetting();
                mListener.settingChanged();
                dismiss();
            }
        });

        mHandler = new Handler();

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void settingChanged();
    }

    protected void readSetting() {
        rightHand = settings.getBoolean(SET_RIGHT_HAND, true);
        currBleAdr = settings.getString(SET_BLE_ADDRESS, null);
        currWebCamAdr = settings.getString(SET_WEB_CAM_ADDRESS, "192.168.1.1:8080");
    }

    protected void saveSetting() {
        editor = settings.edit();
        editor.putBoolean(SET_RIGHT_HAND, rightSwitch.isChecked());

        String bleAddr = editBleAddr.getText().toString().trim();
        if (bleAddr.length() > 0) {
            editor.putString(SET_BLE_ADDRESS, bleAddr);
        } else {
            editor.remove(SET_BLE_ADDRESS);
        }

        editor.putString(SET_WEB_CAM_ADDRESS, editWebCamAddr.getText().toString());
        editor.commit();
    }

    protected void dialogSelectScanBleDevice() {

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(getActivity());
        builderSingle.setTitle(R.string.scanning);

        startScan();

        builderSingle.setNegativeButton(
                "Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        scanLeDevice(false);

                        dialog.dismiss();
                    }
                });

        builderSingle.setAdapter(
                mLeDeviceListAdapter,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        BluetoothDevice device = mLeDeviceListAdapter.getDevice(which);

                        currBleAdr = device.getAddress();
                        editBleAddr.setText(currBleAdr);

                        scanLeDevice(false);

                        dialog.dismiss();
                    }
                });

        AlertDialog alertDialog = builderSingle.create();
        alertDialog.show();
        alertDialog.getWindow().setLayout(640, 640); //Controlling width and height
    }

    protected void startScan() {
        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        scanLeDevice(true);
    }

    protected void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (bleIsScanning) {
                        bleIsScanning = false;
                        mBluetoothLeService.stopLeScan(mLeScanCallback);
                    }
                }
            }, SCAN_PERIOD);

            bleIsScanning = true;
            mBluetoothLeService.startLeScan(mLeScanCallback);
        } else {
            bleIsScanning = false;
            mBluetoothLeService.stopLeScan(mLeScanCallback);

            mBluetoothLeService.close();
        }
    }

    // Device scan callback.
    protected BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mLeDeviceListAdapter.addDevice(device);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            };

    static protected class DeviceViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    // Adapter for holding devices found through scanning.
    protected class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = getActivity().getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            DeviceViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new DeviceViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (DeviceViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);

            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }
}
