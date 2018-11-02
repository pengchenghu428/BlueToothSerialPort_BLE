package com.pengchenghu.bluetoothserialport.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.pengchenghu.bluetoothserialport.R;

/**
 * Created by pengchenghu on 2018/11/2.
 * Author Email: 15651851181@163.com
 * Describe: 根据用户选择的标签，保存为TXT文件
 *           学号、饥饿等级、疲劳等级、恐惧等级一以及健康状况
 */

public class SetLabelsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_labels);
    }
}
