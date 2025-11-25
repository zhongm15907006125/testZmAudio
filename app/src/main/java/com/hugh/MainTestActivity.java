package com.hugh;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSONObject;
import com.hugh.audiofun.R;

import mu.phone.call.JsonCallback;
import mu.phone.call.PhoneCallModule;

public class MainTestActivity extends AppCompatActivity {
    private static final String TAG = "zzmm";
    PhoneCallModule phoneCallModule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        phoneCallModule = new PhoneCallModule(this);
        setContentView(R.layout.activity_test_main);
    }

    /**
     * 解决
     *
     * @param view
     */
    public void makePhoneCall(View view) {
        JSONObject object = new JSONObject();
        object.put("phoneNumber", "17796314385");
        object.put("appId", "123231");
        phoneCallModule.makePhoneCall(object, new JsonCallback() {
            @Override
            public void invoke(JSONObject obj) {

            }

            @Override
            public void invokeAndKeepAlive(JSONObject data) {

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
        object.put("phoneNumber", "17796319690");
        object.put("appId", "123231");
        phoneCallModule.uploadRecordFile(object, new JsonCallback() {
            @Override
            public void invoke(JSONObject obj) {

            }

            @Override
            public void invokeAndKeepAlive(JSONObject data) {

            }
        });
    }

    public void getTop5RecordFiles(View view) {
        phoneCallModule.getTop5RecordFiles(new JsonCallback() {
            @Override
            public void invoke(JSONObject obj) {

            }

            @Override
            public void invokeAndKeepAlive(JSONObject data) {

            }
        }, "");
    }

    public void getAllRecordFiles(View view) {
        phoneCallModule.getAllRecordFiles(new JsonCallback() {
            @Override
            public void invoke(JSONObject obj) {
                Log.i(TAG, obj.toJSONString());
            }

            @Override
            public void invokeAndKeepAlive(JSONObject data) {

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
        Log.i(TAG, "openRecord = " + openRecord);
    }

    public void deleteSystemRecording(View view) {
        phoneCallModule.deleteSystemRecording("");
    }

    public void getSystemRecord(View view) {
        phoneCallModule.getSystemRecord();
    }
}