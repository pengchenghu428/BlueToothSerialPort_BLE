package com.pengchenghu.bluetoothserialport.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.pengchenghu.bluetoothserialport.R;
import com.pengchenghu.bluetoothserialport.domain.Label;

/**
 * Created by pengchenghu on 2018/11/2.
 * Author Email: 15651851181@163.com
 * Describe: 根据用户选择的标签，保存为TXT文件
 *           学号、饥饿等级、疲劳等级、恐惧等级一以及健康状况
 */

public class SetLabelsActivity extends AppCompatActivity implements View.OnClickListener {
    // 控件变量声明
    private EditText mNumberEditView;   // 学号
    private Spinner mHungrySpinner;
    private Spinner mTiredSpinner;
    private Spinner mFearSpinner;
    private Spinner mHealthSpinner;
    private Button mSubmitBtn;

    // Debugging
    private String TAG = "SetLabelsActivity";

    // Return Intent extra
    public static String EXTRA_LABEL = "extra_label";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_labels);

        initWidgetResource();
    }

    // 控件资源初始化及监听设置
    private void initWidgetResource(){
        // 获取控件句柄
        mNumberEditView = (EditText) findViewById(R.id.editview_number);
        mHungrySpinner = (Spinner) findViewById(R.id.spiner_hungry);
        mTiredSpinner = (Spinner) findViewById(R.id.spiner_tired);
        mFearSpinner = (Spinner) findViewById(R.id.spiner_fear);
        mHealthSpinner = (Spinner) findViewById(R.id.spiner_health);
        mSubmitBtn = (Button) findViewById(R.id.button_submit);

        // 设置监听
        mSubmitBtn.setOnClickListener(this);
    }

    // 按键点击监听
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.button_submit:
                if(mNumberEditView.getText().toString().equals("")){ // 学号/ID不能为空
                    Toast.makeText(SetLabelsActivity.this, "学号不能为空", Toast.LENGTH_SHORT).show();
                    break;
                }
                Intent intent = new Intent();
                Label label = new Label(mNumberEditView.getText().toString(),
                        mHungrySpinner.getSelectedItemPosition(),
                        mTiredSpinner.getSelectedItemPosition(),
                        mFearSpinner.getSelectedItemPosition(),
                        mHealthSpinner.getSelectedItemPosition());
                Bundle bundle=new Bundle();
                bundle.putParcelable(EXTRA_LABEL, label);  // 序列化
                intent.putExtras(bundle); // 发送数据
                setResult(RESULT_OK, intent); // 添加result_code
                finish();  // 返回上一个活动
                break;
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onStop(){
        super.onStop();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
    }
}
