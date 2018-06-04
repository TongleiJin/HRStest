package com.cs.hrstest;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class DeviceList extends ListActivity {
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private Context context = null;
    private ArrayList<DeviceInfoHolder> devList = new ArrayList<>();
    private BluetoothLeService mBluetoothLeService;
    private DrawHelper drawHandler = null;
    private String mDeviceAddress;
    private String mDeviceName;
    private TextView mDbgInfo = null;
    private int totalReport = 0;
    private  ArrayList<ArrayList<Integer>> hisHR = null;


    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                myLog("Unable to initialize Bluetooth");
                finish();
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                dbgInfo("device connected");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                dbgInfo("device disconnected");
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                requestHR(mBluetoothLeService.getSupportedGattServices());
                dbgInfo("found services");
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                String chaData = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                String addr = intent.getStringExtra("DEVICE_ADDR");
                updateHR(addr, Integer.parseInt(chaData));
            }
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_device);
        context = this;

        mLeDeviceListAdapter = new LeDeviceListAdapter(devList);
        setListAdapter(mLeDeviceListAdapter);
        devList = getHistoryDevices();
        String logHeader = "";
        for (int i=0; i<devList.size(); i++)
        {
            mLeDeviceListAdapter.addDevice(devList.get(i));
            logHeader += devList.get(i).name;
            if (i < devList.size()-1)
            {
                logHeader += ",";
            }
        }
        mLeDeviceListAdapter.notifyDataSetChanged();
        Logger.start(logHeader);

        drawHandler = (DrawHelper) findViewById(R.id.drawHelpClass);
        mDbgInfo = (TextView) findViewById(R.id.textview_device_info);
        mDbgInfo.setMovementMethod(new ScrollingMovementMethod());

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        startHRRecordThread();

        handleLongPress();
    }

    void handleLongPress()
    {
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                popupForChoice(context, position);
                return  true;
            }
        });
    }



    void popupForChoice(Context ctx, final int devPosition) {
        String[] items = {"Connect", "Disconnect", "Modify Name", "Delete"};
        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(ctx,
                android.R.layout.simple_list_item_1);
        final DeviceInfoHolder device = mLeDeviceListAdapter.getDevice(devPosition);
        builder.setTitle("BLE: " + device.name);
        for (String s : items) {
            adapter.add(s);
        }
        final ListView cmdView = new ListView(ctx);
        cmdView.setAdapter(adapter);

        builder.setView(cmdView);
        final Dialog buildDialog = builder.show();
        cmdView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v,
                                    int listPosition, long id) {
                final DeviceInfoHolder device = mLeDeviceListAdapter.getDevice(devPosition);
                switch(listPosition)
                {
                    case 0:
                        mBluetoothLeService.connect(device.address);
                        dbgInfo("Connecting " + device.name + "...");
                        break;
                    case 1:
                        mBluetoothLeService.disconnect();
                        dbgInfo("DisConnecting ...");
                        break;
                    case 2:
                        modifyName(devPosition);
                        break;
                    case 3:
                        deleteDevice(devPosition);
                        break;
                    default:
                        myLog("Error choice!");
                        break;
                }
                buildDialog.dismiss();
            }
        });
    }


    void modifyName(int devPosition)
    {
        final DeviceInfoHolder device = mLeDeviceListAdapter.getDevice(devPosition);
        myLog("Will modify name of: " + device.name);
    }

    void deleteDevice(int devPosition)
    {
        final DeviceInfoHolder device = mLeDeviceListAdapter.getDevice(devPosition);
        myLog("Will delete: " + device.name);

        devList.remove(devPosition);
        storeHistoryDevices(devList);
        mLeDeviceListAdapter.delDevice(devPosition);
        mLeDeviceListAdapter.notifyDataSetChanged();
        finish();
    }

    void startHRRecordThread()
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    String record = "";
                    for (int i = 0; i < devList.size(); i++) {
                        record += devList.get(i).hrVal;
                        devList.get(i).hrValList.add(devList.get(i).hrVal);
                        if (i < devList.size()-1) {
                            record += ",";
                        }
                    }
                    if (!record.equalsIgnoreCase("0,0,0,0")) {
                        Logger.writeRecord(record);
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }



    private void requestHR(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;

        for (BluetoothGattService s : gattServices) {
            uuid = s.getUuid().toString();
            String sName = ProtocolAttributes.lookup(uuid, "UnknownService");
            myLog(uuid + " ==> " + sName);
            List<BluetoothGattCharacteristic> gattCharacteristics = s.getCharacteristics();
            for (BluetoothGattCharacteristic cha : gattCharacteristics) {
                uuid = cha.getUuid().toString();
                String cName = ProtocolAttributes.lookup(uuid, "UnknownChar");
                myLog("\t\t\t\t" + uuid + "  " + cName);
                if (ProtocolAttributes.isHRMeasure(uuid))
                {
                    myLog("\t\t\t\tReg for hr notification...");
                    mBluetoothLeService.setCharacteristicNotification(cha, true);
                }
            }
        }
    }


    void updateHR(String addr, int hrVal)
    {
        int devIndex = 0;
        for (; devIndex<devList.size(); devIndex++)
        {
            if (devList.get(devIndex).address.equalsIgnoreCase(addr))
            {
                break;
            }
        }
        devList.get(devIndex).hrVal = hrVal;
        mLeDeviceListAdapter.setSrcList(devList);
        mLeDeviceListAdapter.notifyDataSetChanged();
        getActionBar().setTitle("Total: " + ++totalReport);
        drawHandler.drawHRWaveform(devList);
//        myLog("New HR; " + hrVal);
    }



    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }


    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            myLog("Connect request result=" + result);
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == 10) {
            if (resultCode == RESULT_OK) {
                String name = data.getStringExtra("NAME");
                String address = data.getStringExtra("ADDRESS");
                DeviceInfoHolder dev = new DeviceInfoHolder();
                dev.name = name;
                dev.address = address;
                myLog("Will add: " + dev.name + "@" + dev.address);
                devList.add(dev);
                storeHistoryDevices(devList);
                mLeDeviceListAdapter.addDevice(dev);
                mLeDeviceListAdapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_using_devices, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_using_devices_add:
                Intent intent= new Intent();
                intent.setClass(this, ScanBLE.class);
                startActivityForResult(intent, 10);
                return true;
        }
        return true;
    }




/* ========================== No Mention ========================================================*/

    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<DeviceInfoHolder> srcList;
        private LayoutInflater adapterLayoutInflator;

        private class ViewHolder {
            TextView deviceName;
            TextView deviceHR;
            TextView deviceAddr;
        }

        public LeDeviceListAdapter(ArrayList<DeviceInfoHolder> alist) {
            super();
            srcList = alist;
            adapterLayoutInflator = DeviceList.this.getLayoutInflater();
        }

        public void setSrcList(ArrayList<DeviceInfoHolder> list) {
            this.srcList = list;
        }

        public void addDevice(DeviceInfoHolder device) {
            if(!srcList.contains(device)) {
                srcList.add(device);
            }
        }

        public void delDevice(int index) {
            srcList.remove(index);
        }


        public DeviceInfoHolder getDevice(int position) {
            return srcList.get(position);
        }

        public void clear() {
            srcList.clear();
        }

        @Override
        public int getCount() {
            return srcList.size();
        }

        @Override
        public Object getItem(int i) {
            return srcList.get(i);
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
                view = adapterLayoutInflator.inflate(R.layout.list_item_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_list_item_name);
                viewHolder.deviceAddr = (TextView) view.findViewById(R.id.device_list_item_address);
                viewHolder.deviceHR = (TextView) view.findViewById(R.id.device_list_item_hr);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            DeviceInfoHolder device = srcList.get(i);
            final String deviceName = device.name;
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText("Unknown dev");
            viewHolder.deviceHR.setText(device.hrVal+"");
            if (device.hrVal <= 0)
            {
                viewHolder.deviceHR.setText("--");
            }
            viewHolder.deviceAddr.setText(device.address);
            return view;
        }
    }



    public class DeviceInfoHolder
    {
        String name = null;
        String address = null;
        int hrVal = 0;
        ArrayList<Integer> hrValList = new ArrayList<Integer>();
    }

    static final String PREFERENCE_ID = "preference_id_test_ble";
    public static String getSPref(Context context, String key, String defaultVal) {
        SharedPreferences pref = context.getSharedPreferences(PREFERENCE_ID,
                Context.MODE_PRIVATE);
        return pref.getString(key, defaultVal);
    }

    public static void storeSPref(Context context, String key, String val) {
        SharedPreferences pref;
        SharedPreferences.Editor prefEditor;
        pref = context.getSharedPreferences(PREFERENCE_ID, Context.MODE_PRIVATE);
        prefEditor = pref.edit();
        prefEditor.putString(key, val);
        prefEditor.commit();
    }

    void storeHistoryDevices(ArrayList<DeviceInfoHolder> devList)
    {
        String devString = "";
        for (int i=0; i<devList.size(); i++)
        {
            devString += devList.get(i).name;
            devString += ",";
            devString += devList.get(i).address;
            devString += "&&";
        }
        devString += "&&";
        storeSPref(context, "HISTORY_DEVICES", devString);
    }

    ArrayList<DeviceInfoHolder> getHistoryDevices()
    {
        ArrayList<DeviceInfoHolder> devList = new ArrayList<>();
        String devString = getSPref(context, "HISTORY_DEVICES", "&&");
        String[] devListString = devString.split("&&");
        for (int i=0; i<devListString.length; i++)
        {
            String[] item = devListString[i].split(",");
            DeviceInfoHolder dev = new DeviceInfoHolder();
            dev.name = item[0];
            dev.address = item[1];
            dev.hrVal = 0;
            devList.add(dev);
        }
        return devList;
    }

    void dbgInfo(final String str)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDbgInfo.append("\n" + str);
            }
        });
    }


    void myLog(String str)
    {
        Log.i("KTL", "LIST::" + str);
    }

}
