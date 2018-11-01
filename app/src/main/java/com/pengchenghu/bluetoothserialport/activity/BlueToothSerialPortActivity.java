package com.pengchenghu.bluetoothserialport.activity;

import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.inuker.bluetooth.library.BluetoothClient;
import com.inuker.bluetooth.library.connect.listener.BleConnectStatusListener;
import com.inuker.bluetooth.library.connect.options.BleConnectOptions;
import com.inuker.bluetooth.library.connect.response.BleConnectResponse;
import com.inuker.bluetooth.library.connect.response.BleReadResponse;
import com.inuker.bluetooth.library.connect.response.BleWriteResponse;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.inuker.bluetooth.library.model.BleGattService;
import com.inuker.bluetooth.library.receiver.listener.BluetoothBondListener;
import com.pengchenghu.bluetoothserialport.R;
import com.pengchenghu.bluetoothserialport.conffiguration.StaticConfiguration;

import java.io.UnsupportedEncodingException;
import java.util.List;

import static com.inuker.bluetooth.library.Code.REQUEST_DENIED;
import static com.inuker.bluetooth.library.Code.REQUEST_SUCCESS;
import static com.inuker.bluetooth.library.Constants.STATUS_CONNECTED;
import static com.inuker.bluetooth.library.Constants.STATUS_DISCONNECTED;

/**
 * Created by pengchenghu on 2018/10/31.
 * Author Email: 15651851181@163.com
 * Describe: MainActivity 开始/停接收数据 清除窗口 保存TXT
 */

public class BlueToothSerialPortActivity extends AppCompatActivity implements View.OnClickListener {
    // 控件
    private Toolbar mToolbar;
    private TextView mTitle;
    private PopupMenu mOverflowMenu;
    private ImageView mToolbarMoreIcon;

    private ListView mConversationView;
    private Button mDataCollectBtn;
    private Button mWindowClearBtn;
    private Button mDataSaveBtn;

    // 静态变量
    private static final String TAG = "BlueToothSerialPort";
    private static final boolean D = true;
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // mConversationView ListView相关变量
    private String mConnectedDeviceName = null;
    private ArrayAdapter<String> mConversationArrayAdapter;  // ListView 内容
    private BluetoothAdapter mBluetoothAdapter = null;  // 蓝牙适配器

    // 全局变量：管理所有的BLE设备
    public static BluetoothClient mBluetoothClient;
    private String bluetoothMAC;
    private BleGattService bluetoothService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_serialport);
        initWidgetResource();
    }

    // 资源句柄初始化及监听
    private void initWidgetResource(){
        mBluetoothClient = new BluetoothClient(getApplicationContext());
        // 获取控件句柄
        mTitle = (TextView) findViewById(R.id.toolbar_title);
        mToolbarMoreIcon = (ImageView) findViewById(R.id.toolbar_more);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mTitle.setText(R.string.toolbar_title);
        mOverflowMenu = buildOptionsMenu(mToolbarMoreIcon); // 下拉菜单
        mToolbarMoreIcon.setOnTouchListener(mOverflowMenu.getDragToOpenListener());
        mToolbarMoreIcon.setOnClickListener(this);  // ToolBarMoreIcon设置监听
        // ListView 初始化
        mConversationView = (ListView) findViewById(R.id.in);
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mConversationView.setAdapter(mConversationArrayAdapter);
        // Button初始化
        mDataCollectBtn = (Button) findViewById(R.id.button_data_collect);
        mWindowClearBtn = (Button) findViewById(R.id.button_window_clear);
        mDataSaveBtn = (Button) findViewById(R.id.button_data_save);
        mDataCollectBtn.setOnClickListener(this);
        mWindowClearBtn.setOnClickListener(this);
        mDataSaveBtn.setOnClickListener(this);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {   // 设备没有蓝牙，关闭软件
            Toast.makeText(this, "Bluetooth is not available",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    // 消息监听
    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.toolbar_more:  // toolbat more
                mOverflowMenu.show(); // 打开下拉菜单
                break;
            case R.id.button_data_collect:  // 数据采集开始/结束消息监听
                break;
            case R.id.button_window_clear:  // 清空窗口消息监听
                break;
            case R.id.button_data_save:     // 数据保存消息监听
                break;
        }
    }

    // 初始化下拉菜单
    private OptionsPopupMenu buildOptionsMenu(View invoker) {
        final OptionsPopupMenu popupMenu = new OptionsPopupMenu(this, invoker) {
            @Override
            public void show() {
                super.show();
            }
        };
        popupMenu.inflate(R.menu.option_menu);
        popupMenu.setOnMenuItemClickListener(menuItemClickListener);
        return popupMenu;
    }

    // 下拉菜单类
    protected class OptionsPopupMenu extends PopupMenu {
        public OptionsPopupMenu(Context context, View anchor) {
            super(context, anchor, Gravity.END);
        }

        @Override
        public void show() {
            super.show();
        }
    }

    //标题下拉菜单处理
    PopupMenu.OnMenuItemClickListener menuItemClickListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.scan:     // 扫描附近设备
                    Intent serverIntent = new Intent(BlueToothSerialPortActivity.this, DeviceListActivity.class);
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
                    break;
                case R.id.discoverable:  // 确保被发现
                    ensureDiscoverable();
                    break;
                case R.id.about_app:  // 关于本APP
                    Toast.makeText(getApplicationContext(), "Author:pengchenghu", Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        if (!mBluetoothAdapter.isEnabled()) { // 动态请求打开蓝牙
            Intent enableIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);

        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        mBluetoothClient.unregisterConnectStatusListener(bluetoothMAC, mBleConnectStatusListener);
        mBluetoothClient.disconnect(bluetoothMAC);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBluetoothClient.disconnect(bluetoothMAC);
    }

    // 确保本蓝牙可以被发现
    private void ensureDiscoverable() {                               //确保被发现
        Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(
                    BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    // 通过蓝牙发送数据
    private void sendMessage(String Mac, BleGattService service, final String message) {
        byte [] bytes = new byte[0];
        bytes = message.getBytes();

        mBluetoothClient.write(bluetoothMAC, service.getUUID(), service.getCharacters().get(0).getUuid(), bytes, new BleWriteResponse() {
            @Override
            public void onResponse(int code) {
                if (code == REQUEST_SUCCESS) {
                    Log.i(TAG, "指令AT发送成功");
                    mConversationArrayAdapter.add("Send: "+ message);
                    mConversationArrayAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    // 通过蓝牙接收数据
    private void readMessage(String Mac, BleGattService service){
        mBluetoothClient.read(Mac, service.getUUID(), service.getCharacters().get(0).getUuid(), new BleReadResponse() {
            @Override
            public void onResponse(int code, byte[] data) {
                if (code == REQUEST_SUCCESS) {
                    String response = null;
                    try {
                        response = new String(data, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    Log.i(TAG, "串口收到消息：" + response);
                    mConversationArrayAdapter.add("Received: "+ response);
                    mConversationArrayAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    // 可以对串口的按键操作
    private void setupChatButton(){
        mDataCollectBtn.setEnabled(true);
        mWindowClearBtn.setEnabled(true);
        mDataSaveBtn.setEnabled(true);
    }

    // 禁止对串口的按键操作
    private void closeChatButton(){
        mDataCollectBtn.setEnabled(false);
        mWindowClearBtn.setEnabled(false);
        mDataSaveBtn.setEnabled(false);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (D)
            Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    final String address = data.getExtras().getString(
                            DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    BleConnectOptions options = new BleConnectOptions.Builder()
                            .setConnectRetry(3)   // 连接如果失败重试3次
                            .setConnectTimeout(30000)   // 连接超时30s
                            .setServiceDiscoverRetry(3)  // 发现服务如果失败重试3次
                            .setServiceDiscoverTimeout(20000)  // 发现服务超时20s
                            .build();
                    mBluetoothClient.connect(address, new BleConnectResponse() {  // 连接蓝牙
                        @Override
                        public void onResponse(int code, BleGattProfile data) {
                            if(code == REQUEST_SUCCESS){
                                Log.d(TAG, "Connect Sucessfully");
                                // 添加蓝牙连接状态监听
                                mBluetoothClient.registerConnectStatusListener(address, mBleConnectStatusListener);
                                bluetoothMAC = address;  // 保存已成功连接的地址
                                List<BleGattService> services = data.getServices();
                                bluetoothService = services.get(2);
                                //sendMessage(bluetoothMAC, bluetoothService, "AT");
                                readMessage(bluetoothMAC, bluetoothService);
                                setupChatButton();         // 开启按键区域功能
                            }else{
                                Log.d(TAG, String.valueOf(code));
                            }
                        }
                    });
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                } else {
                    // User did not enable Bluetooth or an error occured
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    // 蓝牙连接状态监听变量
    private final BleConnectStatusListener mBleConnectStatusListener = new BleConnectStatusListener() {
        @Override
        public void onConnectStatusChanged(String mac, int status) {
            if (status == STATUS_CONNECTED) {
                Log.d(TAG, "STATUS_CONNECTED");
                setupChatButton();
            } else if (status == STATUS_DISCONNECTED) {
                Log.d(TAG, "STATUS_DISCONNECTED");
                closeChatButton();
            }
        }
    };


}
