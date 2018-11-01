package com.pengchenghu.bluetoothserialport.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.WindowManager;

import com.pengchenghu.bluetoothserialport.R;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by pengchenghu on 2018/10/31.
 * Author Email: 15651851181@163.com
 * Describe: 欢迎页面，2秒后跳转至主页面
 */

public class WelcomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        startBlueToothSerialPortActivity();
    }

    //
    public void startBlueToothSerialPortActivity(){
        TimerTask delayTask = new TimerTask() {
            @Override
            public void run() {
                Intent mainIntent = new Intent(WelcomeActivity.this,BlueToothSerialPortActivity.class);
                startActivity(mainIntent);
                WelcomeActivity.this.finish();
            }
        };
        Timer timer = new Timer();
        timer.schedule(delayTask,2000);//延时两秒执行 run 里面的操作
    }

    // 当WelcomeActivity不可见时，销毁WelcomeActivity
    @Override
    protected void onStop(){
        super.onStop();
        finish();
    }
}
