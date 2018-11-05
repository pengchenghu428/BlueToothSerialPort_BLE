package com.pengchenghu.bluetoothserialport.activity;

import android.content.Intent;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
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
        hideBottomUIMenu();
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

    //
    protected void hideBottomUIMenu(){
        if(Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19){  // lower api
            View v = this.getWindow().getDecorView();
            v.setSystemUiVisibility(View.GONE);
        }else if(Build.VERSION.SDK_INT >= 19){
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    // 当WelcomeActivity不可见时，销毁WelcomeActivity
    @Override
    protected void onStop(){
        super.onStop();
        finish();
    }
}
