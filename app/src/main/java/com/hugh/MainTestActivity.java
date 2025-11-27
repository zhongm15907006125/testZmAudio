package com.hugh;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSONObject;
import com.hugh.audiofun.R;

import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import mu.phone.call.PermissionCallback;
import mu.phone.call.PhoneCallModule;
import mu.phone.call.util.LogUtil;

public class MainTestActivity extends AppCompatActivity {
    private static final String TAG = "zzmm";
    PhoneCallModule phoneCallModule;
    TextView textView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        phoneCallModule = new PhoneCallModule(this);
        setContentView(R.layout.activity_test_main);
        textView = findViewById(R.id.text_message);
    }

    /**
     * 解决
     *
     * @param view
     */
    public void makePhoneCall(View view) {
        LogUtil.d(TAG, "makePhoneCall");
        JSONObject object = new JSONObject();
        object.put("phoneNumber", "10010");
        object.put("appId", "123231");
        phoneCallModule.makePhoneCall(object, new io.dcloud.feature.uniapp.bridge.UniJSCallback() {
            @Override
            public void invoke(Object obj) {
                String s = ((JSONObject) obj).toJSONString();
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainTestActivity.this, s, Toast.LENGTH_SHORT).show();
                        textView.setText(s);
                    }
                });
                LogUtil.d(TAG, "res = " + s);
            }

            @Override
            public void invokeAndKeepAlive(Object data) {

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        phoneCallModule.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * 解决
     *
     * @param view
     */
    public void getSimInfo(View view) {
        JSONObject simInfo = phoneCallModule.getSimInfo(0);
        Log.i(TAG, simInfo.toJSONString());
    }

    public void stopForegroundService(View view) {
        phoneCallModule.stopForegroundService();
    }

    public void startForegroundService(View view) {
        phoneCallModule.startForegroundService();
    }

    public void requsetIgnoreBattery(View view) {
        phoneCallModule.requsetIgnoreBattery();
    }

    public void endCall(View view) {
        phoneCallModule.endCall();
    }

    public void initPermission(View view) {
        phoneCallModule.initPermission();
    }

    public void uploadRecordFile(View view) {
        JSONObject object = new JSONObject();
        object.put("phoneNumber", "10010");
        object.put("appId", "123231");
        phoneCallModule.uploadRecordFile(object, new UniJSCallback() {
            @Override
            public void invoke(Object obj) {
                LogUtil.d(TAG, "res = " + ((JSONObject) obj).toJSONString());
            }

            @Override
            public void invokeAndKeepAlive(Object data) {

            }
        });
    }

    public void getTop5RecordFiles(View view) {
        phoneCallModule.getTop5RecordFiles(new UniJSCallback() {
            @Override
            public void invoke(Object obj) {

            }

            @Override
            public void invokeAndKeepAlive(Object data) {

            }
        }, "");
    }

    public void getAllRecordFiles(View view) {
        phoneCallModule.getAllRecordFiles(new UniJSCallback() {
            @Override
            public void invoke(Object obj) {
                Log.i(TAG, ((JSONObject) obj).toJSONString());
            }

            @Override
            public void invokeAndKeepAlive(Object data) {

            }
        }, "");
    }

    /**
     * 解决
     *
     * @param view
     */
    public void openSelfPermissionSetting(View view) {
        phoneCallModule.openSelfPermissionSetting();
    }

    public void hasSimCard(View view) {
        phoneCallModule.hasSimCard(0);
    }

    public void openRecordSetting(View view) {
        phoneCallModule.openRecordSetting();
    }

    public void isOpenRecord(View view) {
        boolean openRecord = phoneCallModule.isOpenRecord();
        LogUtil.i(TAG, "openRecord = " + openRecord);
    }

    public void deleteSystemRecording(View view) {
        int i = phoneCallModule.deleteSystemRecording();
        LogUtil.i(TAG, "delete size = " + i);
    }

    public void getSystemRecord(View view) {
        phoneCallModule.getSystemRecord();
    }
}