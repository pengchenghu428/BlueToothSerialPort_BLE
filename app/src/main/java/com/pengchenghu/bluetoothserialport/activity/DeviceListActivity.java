package com.pengchenghu.bluetoothserialport.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.inuker.bluetooth.library.beacon.Beacon;
import com.inuker.bluetooth.library.search.SearchRequest;
import com.inuker.bluetooth.library.search.SearchResult;
import com.inuker.bluetooth.library.search.response.SearchResponse;
import com.pengchenghu.bluetoothserialport.R;

import java.util.Set;

/**
 * Created by pengchenghu on 2018/10/31.
 * Author Email: 15651851181@163.com
 * Describe: 发现可连接设备
 */

public class DeviceListActivity extends Activity implements View.OnClickListener {
    private TextView pairedTitle;
    private ListView pairedListView;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;

    private Button scanButton;

    private String TAG = "DeviceListActivity";

    // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Setup the window
        setContentView(R.layout.activity_devicelist);
        // Set result CANCELED incase the user backs out
        setResult(Activity.RESULT_CANCELED);

        initWidgetResource();
    }

    private void initWidgetResource(){
        pairedTitle = (TextView) findViewById(R.id.title_paired_devices);
        pairedTitle.setText(R.string.scanning);
        pairedTitle.setVisibility(View.VISIBLE);

        pairedListView = (ListView) findViewById(R.id.paired_devices); // 1. 获取控件
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name); // 2. 初始化适配器
        pairedListView.setAdapter(mPairedDevicesArrayAdapter); // 3. 装载适配器
        pairedListView.setOnItemClickListener(mDeviceClickListener); // 4. 设置监听

        scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(this);
    }

    // The on-click listener for button.
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button_scan:   // 按钮监听
                Log.i(TAG, "Click on Scan Button");
                doDiscovery();
                break;
        }
    }

    // The on-click listener for all devices in the ListViews
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            BlueToothSerialPortActivity.mBluetoothClient.stopSearch();

            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);

            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        doDiscovery();
    }

    @Override
    protected void onPause() {
        super.onPause();
        BlueToothSerialPortActivity.mBluetoothClient.stopSearch();  // 离开页面时，关闭扫描
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    // discovery device
    private void doDiscovery(){
        Log.i(TAG, "doDiscovery");
        if (BlueToothSerialPortActivity.mBluetoothClient.isBleSupported()){
            Log.i(TAG, "支持BLE");
        }
        if(!BlueToothSerialPortActivity.mBluetoothClient.isBluetoothOpened()){
            BlueToothSerialPortActivity.mBluetoothClient.openBluetooth();
        }

        setTitle(R.string.scanning);
        SearchRequest request = new SearchRequest.Builder()
                .searchBluetoothLeDevice(5000)      // 先扫BLE设备1次，每次5s
                .searchBluetoothClassicDevice(5000) // 再扫经典蓝牙5s
                .searchBluetoothLeDevice(5000)      // 扫BLE设备1次，每次5s
                .build();

        BlueToothSerialPortActivity.mBluetoothClient.search(request, new SearchResponse() {
            @Override
            public void onSearchStarted() {
                Log.i(TAG, "Bluetooth 开始扫描");
            }

            @Override
            public void onDeviceFounded(SearchResult device) {
                // Beacon beacon = new Beacon(device.scanRecord);
                if(mPairedDevicesArrayAdapter.getCount() == 0){
                    mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    mPairedDevicesArrayAdapter.notifyDataSetChanged();
                }
                for(int i = 0; i < mPairedDevicesArrayAdapter.getCount(); i++){
                    if(mPairedDevicesArrayAdapter.getItem(i).contains(device.getAddress())){
                        break;
                    }
                    if(i == mPairedDevicesArrayAdapter.getCount()-1){
                        //Log.i(TAG, "Find new BlueTooth " + device.getName() + " Mac: " + device.getAddress());
                        mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                        mPairedDevicesArrayAdapter.notifyDataSetChanged();
                    }
                }
            }

            @Override
            public void onSearchStopped() {
                Log.i(TAG, "Bluetooth 停止扫描");
                pairedTitle.setText(R.string.title_paired_devices);
            }

            @Override
            public void onSearchCanceled() {

            }
        });
    }
}

