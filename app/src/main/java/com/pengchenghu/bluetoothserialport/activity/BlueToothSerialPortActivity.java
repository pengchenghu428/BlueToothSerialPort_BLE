package com.pengchenghu.bluetoothserialport.activity;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
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
import com.inuker.bluetooth.library.connect.response.BleNotifyResponse;
import com.inuker.bluetooth.library.connect.response.BleReadResponse;
import com.inuker.bluetooth.library.connect.response.BleUnnotifyResponse;
import com.inuker.bluetooth.library.connect.response.BleWriteResponse;
import com.inuker.bluetooth.library.model.BleGattProfile;
import com.inuker.bluetooth.library.model.BleGattService;
import com.inuker.bluetooth.library.receiver.listener.BluetoothBondListener;
import com.pengchenghu.bluetoothserialport.R;
import com.pengchenghu.bluetoothserialport.conffiguration.StaticConfiguration;
import com.pengchenghu.bluetoothserialport.domain.Label;
import com.pengchenghu.bluetoothserialport.tools.HexAndByte;
import com.pengchenghu.bluetoothserialport.tools.ParseBLEFrame;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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

//    private ListView mConversationView;
    private TextView mParseBleDataTv;
    private ListView mParseBleDataView;
    private Button mDataCollectBtn;
    private Button mWindowClearBtn;
    private Button mDataSaveBtn;

    // 静态变量
    private static final String TAG = "BlueToothSerialPort";
    private static final boolean D = true;
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int REQUEST_SET_LABEL = 3;

    // 权限
    private static final int REQUEST_EXTERNAL_STORAGE_WRITE_PERMISSON = 1;

    // mConversationView ListView相关变量
    //private ArrayAdapter<String> mConversationArrayAdapter;  // ListView 内容
    private List<String> mConversationArray = new ArrayList<>();  // 原始数据保存
    // mParseBleDataView ListView相关变量
    private ArrayAdapter<String> mParseBleDataArrayAdapter;

    // 全局变量：管理所有的BLE设备
    public static BluetoothClient mBluetoothClient;  // 蓝牙管理类
    private BluetoothAdapter mBluetoothAdapter = null;  // 蓝牙适配器
    private String bluetoothMAC;
    private BleGattService bluetoothService;

    // 蓝牙数据临时变量
    Label mLabel = new Label();
    String dataString = new String();
    int count = 0;

    // handler 对象，用来接收消息
    private static final int PROCESS_AND_DISPLAY_DATA = 1;
    private Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            // 处理从子线程发送过来的消息
            String response = (String) msg.obj;
            switch (msg.what){
                case PROCESS_AND_DISPLAY_DATA:
                    processAndDisplayData(response);
                    break;
            }
            return false;
        }
    });

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
        mParseBleDataTv = (TextView) findViewById(R.id.data_header);
        mParseBleDataTv.setText(ParseBLEFrame.parseBleDataHeader);
//        mConversationView = (ListView) findViewById(R.id.old_data);  // 获取控件资源
//        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message); // 初始化适配器
//        mConversationView.setAdapter(mConversationArrayAdapter); // 装载适配器
        mParseBleDataView = (ListView) findViewById(R.id.process_data);
        mParseBleDataArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mParseBleDataView.setAdapter(mParseBleDataArrayAdapter);  // 装载适配器
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
                //Log.d(TAG, "mDataCollectBtn listener" );
                if(mDataCollectBtn.getText().toString().equals(getResources().getString(R.string.data_collect_start))){
                    //Log.d(TAG, "mDataCollectBtn listener: " +  getResources().getString(R.string.data_collect_start));
                    setupChatNotify();  // 打开notify，采集数据
                    mDataCollectBtn.setText(getResources().getString(R.string.data_collect_end));
                }else if(mDataCollectBtn.getText().toString().equals(getResources().getString(R.string.data_collect_end))){
                    //Log.d(TAG, "mDataCollectBtn listener: " +  getResources().getString(R.string.data_collect_end));
                    closeChatNotify();  // 关闭notify，停止采集数据
                    mDataCollectBtn.setText(getResources().getString(R.string.data_collect_start));
                }
                break;
            case R.id.button_window_clear:  // 清空窗口消息监听
                if(mDataCollectBtn.getText().toString().equals(getResources().
                        getString(R.string.data_collect_start))) { // 当前采集结束，清空所有的变量
                    mConversationArray.clear();
                    mParseBleDataArrayAdapter.clear();
                    mParseBleDataArrayAdapter.notifyDataSetChanged();
                    dataString = "";
                    ParseBLEFrame.clearData();
                }else if(mDataCollectBtn.getText().toString().equals(getResources().
                        getString(R.string.data_collect_end))){  // 当前正在采集，仅清空显示部分变量
                    mParseBleDataArrayAdapter.clear();
                    mParseBleDataArrayAdapter.notifyDataSetChanged();
                }
                break;
            case R.id.button_data_save:     // 数据保存消息监听
                if(mDataCollectBtn.getText().toString().equals(getResources().getString(R.string.data_collect_end))){
                    Toast.makeText(BlueToothSerialPortActivity.this, "请先停止当前采集", Toast.LENGTH_SHORT).show();
                    break;
                }
                Intent intent = new Intent(BlueToothSerialPortActivity.this, SetLabelsActivity.class);
                startActivityForResult(intent, REQUEST_SET_LABEL);
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mBluetoothClient.disconnect(bluetoothMAC);
        mBluetoothClient.unregisterConnectStatusListener(bluetoothMAC, mBleConnectStatusListener);
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
                    // Log.i(TAG, "指令发送成功");
//                    mConversationArrayAdapter.add("Send: "+ message);
//                    mConversationArrayAdapter.notifyDataSetChanged();
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
//                    mConversationArrayAdapter.add("Received: "+ response);
//                    mConversationArrayAdapter.notifyDataSetChanged();
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
    private void setupChatNotify(){
        Log.d(TAG, "Setup Chat Notify" );
        mBluetoothClient.notify(bluetoothMAC, bluetoothService.getUUID(),
                bluetoothService.getCharacters().get(0).getUuid(), new BleNotifyResponse() {
                    @Override
                    public void onNotify(UUID service, UUID character, byte[] value) {
                        //硬件回复的数据在这里,统一处理
                        String response = HexAndByte.bytesToCharArray(value);
                        Message message = Message.obtain(mHandler);
                        message.what = PROCESS_AND_DISPLAY_DATA;
                        message.obj = response;
                        mHandler.sendMessage(message);
                    }

                    @Override
                    public void onResponse(int code) {
                        if(code == REQUEST_SUCCESS){
                            Log.d(TAG, "Open Notify Sucessfully");
                            // 打开notify成功后发送数据
                            // sendMessage(bluetoothMAC, bluetoothService, "AT");
                        }
                    }
                });
    }

    // 禁止对串口的按键操作
    private void closeChatButton(){
        mDataCollectBtn.setEnabled(false);
        mWindowClearBtn.setEnabled(false);
        mDataSaveBtn.setEnabled(false);
    }
    private void closeChatNotify(){
        mBluetoothClient.unnotify(bluetoothMAC, bluetoothService.getUUID(),
                bluetoothService.getCharacters().get(0).getUuid(), new BleUnnotifyResponse() {
                    @Override
                    public void onResponse(int code) {
                        if(code == REQUEST_SUCCESS){
                            // 关闭notify
                            Log.d(TAG, "Close Notify Sucessfully");
                        }
                    }
                });
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
                            if(code == REQUEST_SUCCESS){   // 连接成功
                                Log.d(TAG, "Connect Sucessfully");
                                // 断开上一次的连接
                                mBluetoothClient.disconnect(bluetoothMAC);
                                mBluetoothClient.unregisterConnectStatusListener(bluetoothMAC, mBleConnectStatusListener);
                                // 添加蓝牙连接状态监听
                                mBluetoothClient.registerConnectStatusListener(address, mBleConnectStatusListener);
                                bluetoothMAC = address;  // 保存已成功连接的地址
                                List<BleGattService> services = data.getServices();
                                bluetoothService = services.get(2);
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
            case REQUEST_SET_LABEL:
                if(resultCode == Activity.RESULT_OK) {
                    Label label = (Label) data.getParcelableExtra(SetLabelsActivity.EXTRA_LABEL);
                    mLabel = label;
                    writeDataToDisk(mLabel);
                }
                break;
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

    // 蓝牙数据处理及显示
    public void processAndDisplayData(String response){
        char stx = 2;  // STX
        char etx = 3;  // ETX
        if(dataString.isEmpty()){
            int stxLocation = response.indexOf(stx);
            if(stxLocation == -1) return;
            dataString = dataString.concat(response.substring(stxLocation));
        }else{
            dataString = dataString.concat(response);
        }
        int stxLocation = dataString.indexOf(stx);
        int etxLocation = dataString.indexOf(etx);
        //Log.d("ParseBLE", "stxLocation: "+ stxLocation +  " etxLocation:"+ etxLocation);
        if(stxLocation != -1 && etxLocation != -1 ) {
            String receiveData = dataString.substring(stxLocation, (etxLocation+1));
            dataString = dataString.replaceFirst(receiveData, "");
            if((etxLocation-stxLocation)==225) {  // 当且仅当长度为225时，处理数据
                String bleFrameStr = receiveData.substring(1,
                        receiveData.length() - 1);  // 蓝牙数据帧
                mConversationArray.add(bleFrameStr);
                getSensorDataAndDisplay(bleFrameStr);  // 解析数据帧并显示
            }
        }
    }
    // 获取传感器的数据并显示
    public void getSensorDataAndDisplay(String data){
        //Log.d("ParseBLE", "getSensorDataAndDisplay: " + data);
        ParseBLEFrame.parseBLEDataFrame(data);   // 解析数据
        // String parseStr = ParseBLEFrame.getParseDataFrame(data);
        List<String> parseArray = ParseBLEFrame.getParseDataFrameAfterClassify();
        if(!parseArray.isEmpty()) {
            count++;        // 每十次显示一次结果
            if(count % 15 == 0) {
                mParseBleDataArrayAdapter.addAll(ParseBLEFrame.getParseDataFrameAfterClassify());
                mParseBleDataArrayAdapter.notifyDataSetChanged();
                count = 0;
            }
//            if(mParseBleDataArrayAdapter.getCount() >= 15*17){
//                mParseBleDataArrayAdapter.clear();
//                mParseBleDataArrayAdapter.notifyDataSetChanged();
//            }
        }
    }

    // 将蓝牙获得的数据写入文件
    public void writeDataToDisk(Label label){
        //使用兼容库就无需判断系统版本
        int hasWriteStoragePermission = ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (hasWriteStoragePermission == PackageManager.PERMISSION_GRANTED) {
            // 拥有权限，执行操作
            writeDataToFile(label);
        }else{
            // 没有权限，向用户请求权限
            ActivityCompat.requestPermissions(BlueToothSerialPortActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_EXTERNAL_STORAGE_WRITE_PERMISSON);
        }
    }

    // 申请权限
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        //通过requestCode来识别是否同一个请求
        if (requestCode == REQUEST_EXTERNAL_STORAGE_WRITE_PERMISSON){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //用户同意，执行操作
                writeDataToFile(mLabel);
            }else{
                //用户不同意，向用户展示该权限作用
                Toast.makeText(BlueToothSerialPortActivity.this,
                        "写入文件失败，用户未开放读写权限", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 数据保存到文件
    public void writeDataToFile(Label label){
        String rootDir_level1 = Environment.getExternalStoragePublicDirectory("").toString() + "/ATemp";
        File directory_level1 = new File(rootDir_level1);
        if(!directory_level1.exists()){    // 创建一级目录
            directory_level1.mkdir();
        }
        String rootDir = Environment.getExternalStoragePublicDirectory("").toString() + "/ATemp/Bef_Data";
        File directory = new File(rootDir);
        if(!directory.exists()){    // 创建二级目录
            directory.mkdir();
        }
        long l = System.currentTimeMillis(); //得到long类型当前时间
        Date date = new Date(l); //new日期对象
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String filename = dateFormat.format(date) + ".txt";
        File file = new File(rootDir, filename); // 实例化文件对象
        try {
            if(file.exists()){   // 如果文件存在
                file.delete();   // 删除文件
            }
            file.createNewFile(); // 创建新的文件
            FileOutputStream fos=new FileOutputStream(file);
            OutputStreamWriter osw=new OutputStreamWriter(fos);
            // 测试者标签
            osw.write("Number: "+ label.getNumber() + System.lineSeparator());
            osw.write("Hungry: "+ label.getHungry_label() + System.lineSeparator());
            osw.write("Tired: "+ label.getTired_label() + System.lineSeparator());
            osw.write("Fear: "+ label.getFear_label() + System.lineSeparator());
            osw.write("Health: "+ label.getHealth_label() + System.lineSeparator());
            for(int i = 0; i < mConversationArray.size(); i++){
                osw.write(mConversationArray.get(i) + System.lineSeparator());
            }
            osw.flush();
            osw.close();
            fos.close();
            Toast.makeText(BlueToothSerialPortActivity.this,
                    "数据成功保存:"+rootDir+"/"+filename, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 将解析后的数据文件保存
        ParseBLEFrame.writeParseDataToFile(label, filename);
    }
}
