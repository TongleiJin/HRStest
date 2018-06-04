/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.cs.hrstest;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class ScanBLE extends ListActivity {
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private Context context = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        context = this;

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        handleLongPress();
    }


    void handleLongPress()
    {
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                final InfoHolder device = mLeDeviceListAdapter.getDevice(position);
                AlertDialog.Builder alert = new AlertDialog.Builder(context);
                alert.setTitle("Add new BLE device");
                TextView tvInfo = new TextView(context);
                tvInfo.setText("将要添加设备：" + device.name);
                alert.setView(tvInfo);

                alert.setPositiveButton("添加", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int whichButton) {
                        final InfoHolder device = mLeDeviceListAdapter.getDevice(position);
                        Intent iScan = new Intent();
                        iScan.putExtra("NAME", device.name);
                        iScan.putExtra("ADDRESS", device.address);
                        setResult(Activity.RESULT_OK, iScan);
                        finish();
                    }
                });
                alert.setNegativeButton("取消", null);
                alert.show();
                return  true;
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_scan_devices, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);
    }


    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }



    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<InfoHolder> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<InfoHolder>();
            mInflator = ScanBLE.this.getLayoutInflater();
        }

        boolean hasDevice(InfoHolder device)
        {
            for (InfoHolder info : mLeDevices)
            {
                if (device.address.equalsIgnoreCase(info.address))
                {
                    return true;
                }
            }
            return false;
        }


        int getPosition(InfoHolder device)
        {
            int i = 0;
            for (i=0; i<mLeDevices.size(); i++)
            {
                if (device.rssi > mLeDevices.get(i).rssi)
                {
                    break;
                }
            }
            return i;
        }

        public void addDevice(InfoHolder device) {
            if (!hasDevice(device))
            {
                int pos = getPosition(device);
                if (pos < mLeDevices.size()) {
                    mLeDevices.add(pos, device);
                }
                else
                {
                    mLeDevices.add(device);
                }
            }
        }

        public InfoHolder getDevice(int position) {
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
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.list_item_scan_ble, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.scan_ble_item_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.scan_ble_item_name);
                viewHolder.deviceRSSI = (TextView) view.findViewById(R.id.scan_ble_item_rssi);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            InfoHolder device = mLeDevices.get(i);
            final String deviceName = device.name;
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText("Unkown Dev");
            viewHolder.deviceAddress.setText(device.address);
            viewHolder.deviceRSSI.setText(device.rssi + "dBm");
            return view;
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    if (rssi > -80) {
                        InfoHolder info = new InfoHolder();
                        info.name = device.getName();
                        if (info.name == null)
                        {
                            info.name = "No Name";
                        }
                        info.address = device.getAddress();
                        info.rssi = rssi;
                        mLeDeviceListAdapter.addDevice(info);
                        mLeDeviceListAdapter.notifyDataSetChanged();
//                    }
                }
            });
        }
    };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRSSI;
    }

    class InfoHolder
    {
        String name = null;
        String address = null;
        int rssi = 0;
    }

    void myLog(String str)
    {
        Log.i("KTL", "LIST::" + str);
    }
}