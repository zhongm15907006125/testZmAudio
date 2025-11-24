package mu.phone.call;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.alibaba.fastjson.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import mu.phone.call.model.RecordFile;
import mu.phone.call.model.SimInfo;
import mu.phone.call.service.MuCallService;
import mu.phone.call.util.CallLogHelper;
import mu.phone.call.util.FileWatcher;
import mu.phone.call.util.PermissionPageManagement;
import mu.phone.call.util.RecordUtil;
import mu.phone.call.util.RomUtil;
import mu.phone.call.util.SystemCallUtil;
import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * author：jun
 * date：2021/11/7
 */
public class PhoneCallModule {

    private final Handler mHandler = new Handler();

    private static final String[] PERMISSIONS = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_CALL_LOG, Manifest.permission.WRITE_CALL_LOG};

    private JSONObject params;
    private JsonCallback callback;
    private static final String DATA_PATH = "/storage/emulated/0/Android/data/";

    private FileWatcher fileWatcher;
    private String newFilePath;

    private final ScheduledExecutorService mScheduledExecutor = Executors.newScheduledThreadPool(2); // 线程池 同一管理保证只有一个
    private ScheduledFuture<?> mForegroundTask; // 串口读取任务

    private volatile boolean isReadCallLog = false;
    private volatile boolean isRegister = false;

    private Context context;

    public PhoneCallModule(Context context) {
        this.context = context;
    }

    private Context getUniContext(){
       return context;
    }

    // @UniJSMethod(uiThread = false)
    public void makePhoneCall(JSONObject params, JsonCallback callback) {
        cancelForegroundTask();

        if (params != null) {
            this.params = params;
            String phoneNumber = params.getString("phoneNumber");
            String savePath;
            if (params.containsKey("savePath")) {
                savePath = params.getString("savePath");
            } else {
                String appId = params.getString("appId");
                savePath = DATA_PATH + getUniContext().getPackageName() + "/apps/" + appId + "/doc/";
            }

            int simId = -1;
            if(params.containsKey("simId")) {
                simId = params.getIntValue("simId");
            }

            String id = params.getString("id");
            String recordDir = params.getString("recordDir");
            long delayMillis = params.getLongValue("delayMillis");
            this.callback = callback;
            if(ContextCompat.checkSelfPermission(getUniContext(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((Activity) getUniContext(), new String[]{Manifest.permission.CALL_PHONE}, 1001);
                return;
            }

            if(ContextCompat.checkSelfPermission(getUniContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(getUniContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(getUniContext(), Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(getUniContext(), Manifest.permission.WRITE_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((Activity) getUniContext(), PERMISSIONS, 1002);
                return;
            }

            CallLogHelper callLogHelper = new CallLogHelper(getUniContext(), phoneNumber, (callLogMap) -> {
                JSONObject data = new JSONObject();
                data.put("callState", TelephonyManager.CALL_STATE_IDLE);
                data.put("callStateName", "IDLE");
                data.put("callLog", callLogMap);
                if (!"0".equals(String.valueOf(callLogMap.get("duration")))) {
                    File tempFile = null;
                    if(newFilePath != null) {
                        int index = newFilePath.lastIndexOf("/");
                        String newFileDir = newFilePath.substring(0, index + 1);
                        String newFileName = newFilePath.substring(index + 1);
                        newFileName = newFileName.replaceAll("^\\.pending-|^[.]", "");
                        File newFile = new File(newFileDir + newFileName);
                        if(!newFile.exists()) newFilePath = null;
                    }

                    if(newFilePath == null) {
                        List<RecordFile> fileData = new ArrayList<>();
                        String recordPath = RecordUtil.getSystemRecord(recordDir);
                        if(recordPath != null) {
                            File recordFileDir = new File(recordPath);
                            File[] files = recordFileDir.listFiles();
                            if (files != null && files.length > 0) {
                                for (File file : files) {
                                    RecordFile recordFile = new RecordFile();
                                    recordFile.setName(file.getName());
                                    recordFile.setSize(file.length());
                                    recordFile.setPath(file.getAbsolutePath());
                                    recordFile.setLastModified(file.lastModified());
                                    fileData.add(recordFile);
                                }
                                Collections.sort(fileData);
                                Log.i("DDDDD", "总共有多少个录音文件:" + fileData.size());
                                if (fileData.size() > 0) {
                                    newFilePath = fileData.get(0).getPath();
                                    Log.i("DDDDD", "newFilePath ==" + newFilePath);
                                    tempFile = new File(newFilePath);
                                } else {
                                    Log.i("DDDDD", "newFilePath is empty");
                                }
                            }
                        }
                    } else {
                        tempFile = new File(newFilePath);
                    }

                    if (tempFile != null && tempFile.lastModified() >= (long) callLogMap.get("talkTime")) {
                        data.put("recordFileOriginalPath", tempFile.getAbsolutePath());
                        data.put("recordFileName", tempFile.getName());
                        String[] nameAndSuffix = tempFile.getName().split("\\.");
                        String toFileName = nameAndSuffix[0] + (id == null ? "" : id) + "." + nameAndSuffix[1];
                        File toFile = new File(savePath + toFileName);
                        boolean success = this.copy(tempFile, toFile);
                        if (success) {
                            data.put("recordFilePath", toFile.getAbsolutePath());
                        } else {
                            data.put("recordFilePath", tempFile.getAbsolutePath());
                        }
                    }
                }

                if (callback != null) {
                    callback.invoke(data);
                }
            });

            // TODO 注册广播
            if(!isRegister){
                isRegister = true;
                Log.i("DDDDD", "开始注册广播");
                IntentFilter filter = new IntentFilter();
                filter.addAction("android.intent.action.PHONE_STATE");
                filter.addAction("android.intent.action.NEW_OUTGOING_CALL");
                filter.setPriority(Integer.MAX_VALUE);
                getUniContext().registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        String action = intent.getAction();
                        if(TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(action)) {
                            TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                            int state = telephony.getCallState();
                            if(state == TelephonyManager.CALL_STATE_RINGING) {
                                JSONObject data = new JSONObject();
                                data.put("callState", state);
                                data.put("callStateName", "RINGING");
                                callback.invokeAndKeepAlive(data);
                            }

                            // 正在通话中
                            if(state == TelephonyManager.CALL_STATE_OFFHOOK) {
                                Log.i("DDDDD", "正在通话中");
                                if(!isReadCallLog){
                                    isReadCallLog = true;
                                    newFilePath = null;
                                    String recordPath = RecordUtil.getSystemRecord(recordDir);
                                    if(recordPath != null) {
                                        Log.i("DDDDD", "开始监听通话录音");
                                        fileWatcher = new FileWatcher(recordPath);
                                        fileWatcher.setCallFileWatcherListener(path -> newFilePath = path);
                                        fileWatcher.startWatching();
                                    }
                                }

                                JSONObject data = new JSONObject();
                                data.put("callState", state);
                                data.put("callStateName", "OFFHOOK");
                                callback.invokeAndKeepAlive(data);
                            }

                            if(telephony.getCallState() == TelephonyManager.CALL_STATE_IDLE) {
                                if(isReadCallLog) {
                                    // 打印结束通话
                                    Log.i("DDDDD", "结束通话");
                                    isReadCallLog = false;
                                    isRegister = false;
                                    context.unregisterReceiver(this);
                                    Log.i("DDDDD", "注销广播");
                                    callLogHelper.start();
                                    if(fileWatcher != null) {
                                        Log.i("DDDDD", "结束通话  关闭fileWatcher");
                                        fileWatcher.stopWatching();
                                        fileWatcher = null;
                                    } else {
                                        Log.i("DDDDD", "结束通话  fileWatcher = null");
                                    }
                                } else {
                                    Log.i("DDDDD", "结束通话  isReadCallLog = false");
                                }
                            }
                        }
                    }
                }, filter);
            }

            // TODO 拨打电话
            isReadCallLog = false;
            Log.i("DDDDD", "开始打电话" + phoneNumber);
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            if(simId == 0 || simId == 1) {
                assignSIM((Activity) getUniContext(), intent, simId);
            }
            getUniContext().startActivity(intent);
        }
    }

    //取消任务
    private void cancelForegroundTask() {
        if (mForegroundTask == null) return;
        mForegroundTask.cancel(true);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mForegroundTask = null;
    }

//    @UniJSMethod(uiThread = false)
    public String getSystemRecord() {
        String systemRecord = RecordUtil.getSystemRecord();
        return systemRecord == null ? "" : systemRecord;
    }

//    @UniJSMethod(uiThread = false)
    public boolean deleteSystemRecording(String path) {
        File file = new File(path);
        if(file.exists()) {
            return file.delete();
        }
        return true;
    }

//    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 1001 || requestCode == 1002) {
            for (int grantResult : grantResults) {
                if (grantResult != 0) {
                    return;
                }
            }
            makePhoneCall(params, callback);
        }

        if(requestCode == 1004) {
            if (grantResults[0] == 0) {
                getAllRecordFiles(allRecordCallback, allRecordRecordDir);
            }
        }

        if(requestCode == 1005) {
            if (grantResults[0] != 0) {
                return;
            }
            uploadRecordFile(uploadParamsTemp, uploadCallbackTemp);
        }

        if(requestCode == 1006) {
            if (grantResults[0] != 0) {
                return;
            }
            getTop5RecordFiles(top10RecordCallback, top10RecordRecordDir);
        }
    }

    private boolean copy(File fromFile, File toFile) {
        try {
            if (!toFile.exists()) {
                boolean success = toFile.createNewFile();
                if (!success) {
                    return false;
                }
            }

            InputStream fos = new FileInputStream(fromFile);
            OutputStream os = new FileOutputStream(toFile);
            byte[] buff = new byte[1024];

            int len;
            while((len = fos.read(buff)) > 0) {
                os.write(buff, 0, len);
            }

            fos.close();
            os.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

//    @UniJSMethod(uiThread = false)
    public boolean isOpenRecord() {
        return SystemCallUtil.checkIsOpenAudioRecord(getUniContext());
    }

//    @UniJSMethod(uiThread = false)
    public boolean openRecordSetting() {
        return SystemCallUtil.goSetSysLuyin(getUniContext());
    }

    /**
     * 检测SIM卡是否存在
     * @param simIndex 0表示卡1，1表示卡2
     * @return 返回SIM是否存在
     */
//    @UniJSMethod(uiThread = false)
    public boolean hasSimCard(int simIndex) {
        if (Build.VERSION.SDK_INT >= 22) {
            try {
                @SuppressLint("MissingPermission")
                List<SubscriptionInfo> activeSubscriptionInfoList = SubscriptionManager.from(getUniContext()).getActiveSubscriptionInfoList();
                if (activeSubscriptionInfoList == null || activeSubscriptionInfoList.size() <= 0) {
                    return false;
                }
                for (int i = 0; i < activeSubscriptionInfoList.size(); i++) {
                    if (activeSubscriptionInfoList.get(i).getSimSlotIndex() == simIndex) {
                        return true;
                    }
                }
                return false;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    /**
     * 打开应用权限设置界面，若设备未适配则打开应用信息详情界面
     */
//    @UniJSMethod(uiThread = false)
    public void openSelfPermissionSetting() {
        PermissionPageManagement.goToSetting((Activity) getUniContext());
    }


    private JsonCallback allRecordCallback;
    private String allRecordRecordDir;

    /**
     * 获取所有录音文件
     * @param callback 响应
     */
//    @UniJSMethod(uiThread = false)
    public void getAllRecordFiles(JsonCallback callback, String recordDir) {
        if(ContextCompat.checkSelfPermission(getUniContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) getUniContext(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1004);

            allRecordCallback = callback;
            allRecordRecordDir = recordDir;
            return;
        }

        List<RecordFile> fileData = new ArrayList<>();
        String recordPath = RecordUtil.getSystemRecord(recordDir);
        if(recordPath != null) {
            File recordFileDir = new File(recordPath);
            File[] files = recordFileDir.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files) {
                    RecordFile recordFile = new RecordFile();
                    recordFile.setName(file.getName());
                    recordFile.setSize(file.length());
                    recordFile.setPath(file.getAbsolutePath());
                    recordFile.setLastModified(file.lastModified());
                    fileData.add(recordFile);
                }
                Collections.sort(fileData);
            }
        }

        JSONObject data = new JSONObject();
        data.put("success", true);
        data.put("data", fileData);
        callback.invoke(data);
    }

    private JsonCallback top10RecordCallback;
    private String top10RecordRecordDir;

    /**
     * 获取10个最新的录音文件
     * @param callback 响应
     */
//    @UniJSMethod(uiThread = false)
    public void getTop5RecordFiles(JsonCallback callback, String recordDir) {
        if(ContextCompat.checkSelfPermission(getUniContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) getUniContext(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1006);

            top10RecordCallback = callback;
            top10RecordRecordDir = recordDir;
            return;
        }

        List<RecordFile> fileData = new ArrayList<>();
        String recordPath = RecordUtil.getSystemRecord(recordDir);
        if(recordPath != null) {
            File recordFileDir = new File(recordPath);
            File[] files = recordFileDir.listFiles();
            if (files != null && files.length > 0) {
                for (File file : files) {
                    RecordFile recordFile = new RecordFile();
                    recordFile.setName(file.getName());
                    recordFile.setSize(file.length());
                    recordFile.setPath(file.getAbsolutePath());
                    recordFile.setLastModified(file.lastModified());
                    fileData.add(recordFile);
                }
                Collections.sort(fileData);
                if(fileData.size() > 5) {
                    fileData = fileData.subList(0, 5);
                }
            }
        }

        JSONObject data = new JSONObject();
        data.put("success", true);
        data.put("data", fileData);
        callback.invoke(data);
    }


    private JSONObject uploadParamsTemp;
    private JsonCallback uploadCallbackTemp;

//    @UniJSMethod(uiThread = false)
    public void uploadRecordFile(JSONObject params, JsonCallback callback) {
        if(ContextCompat.checkSelfPermission(getUniContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            uploadParamsTemp = params;
            uploadCallbackTemp = callback;

            ActivityCompat.requestPermissions((Activity) getUniContext(), new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE }, 1005);
            return;
        }

        Request.Builder builder = new Request.Builder();

        // 请求地址
        String url = params.getString("url");
        if(TextUtils.isEmpty(url)) {
            JSONObject data = new JSONObject();
            data.put("statusCode", 0);
            data.put("errorMsg", "请求地址不能为空");
            callback.invoke(data);
            return;
        }
        builder.url(url);

        String contentType = "multipart/form-data";
        if(params.containsKey("header")) {
            JSONObject header = params.getJSONObject("header");
            if(header.size() > 0) {
                for (Map.Entry<String, Object> entry : header.entrySet()) {
                    builder.header(entry.getKey(), String.valueOf(entry.getValue()));
                    if("Content-Type".equalsIgnoreCase(entry.getKey())) {
                        contentType = String.valueOf(entry.getValue());
                    }
                }
            }
        }

        // 文件本地路径
        String filePath = params.getString("filePath");
        File file = new File(filePath);
        if(!file.exists()) {
            JSONObject data = new JSONObject();
            data.put("statusCode", 0);
            data.put("errorMsg", "本地文件不存在");
            callback.invoke(data);
            return;
        }

        String name = "file";
        if(params.containsKey("name")) {
            name = params.getString("name");
        }

        String method = "POST";
        if(params.containsKey("method")) {
            method = params.getString("method");
        }

        // 添加文件Body以及参数
        if("PUT".equalsIgnoreCase(method)) {
            RequestBody body = RequestBody.create(file, MediaType.parse(contentType));
            builder.put(body);
        } else {
            MultipartBody.Builder bodyBuilder = new MultipartBody.Builder();
            RequestBody body = RequestBody.create(file, MediaType.parse("multipart/form-data"));
            bodyBuilder.addFormDataPart(name, file.getName(), body);
            if(params.containsKey("formData")) {
                JSONObject formData = params.getJSONObject("formData");
                if(formData.size() > 0) {
                    for (Map.Entry<String, Object> entry : formData.entrySet()) {
                        bodyBuilder.addPart(Headers.of("Content-Disposition", "form-data; name=\"" + entry.getKey() + "\""),
                                RequestBody.Companion.create(String.valueOf(entry.getValue()), null));
                        bodyBuilder.addFormDataPart(entry.getKey(), String.valueOf(entry.getValue()));
                    }
                }
            }
            bodyBuilder.setType(MultipartBody.FORM);
            builder.post(bodyBuilder.build());
        }

        long timeout = 60000;
        if(params.containsKey("timeout")) {
            timeout = params.getLongValue("timeout");
        }
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                .readTimeout(timeout, TimeUnit.MILLISECONDS)
                .writeTimeout(timeout, TimeUnit.MILLISECONDS)
                .sslSocketFactory(getSSLSocketFactory(), new TrustAllCerts())
                .hostnameVerifier(new TrustAllHostnameVerifier())
                .build();

        Call call = okHttpClient.newCall(builder.build());
        try {
            Response response = call.execute();
            JSONObject data = new JSONObject();
            data.put("statusCode", response.code());
            data.put("data", response.body().string());
            callback.invoke(data);
        } catch (IOException e) {
            e.printStackTrace();
            JSONObject data = new JSONObject();
            data.put("statusCode", 0);
            data.put("errorMsg", e.getMessage());
            callback.invoke(data);
        }
    }

//    @UniJSMethod()
    public void initPermission() {
        String[] ALL_PERMISSIONS = {Manifest.permission.READ_PHONE_STATE, Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_CALL_LOG, Manifest.permission.WRITE_CALL_LOG,
                Manifest.permission.CALL_PHONE};

        List<String> deniedPermissionList = new ArrayList<>();
        for (String permission : ALL_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(getUniContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                deniedPermissionList.add(permission);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if(ContextCompat.checkSelfPermission(getUniContext(), Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
                deniedPermissionList.add(Manifest.permission.ANSWER_PHONE_CALLS);
            }
        }

        if(deniedPermissionList.size() > 0) {
            ActivityCompat.requestPermissions((Activity) getUniContext(), deniedPermissionList.toArray(new String[0]), 10001);
        }
    }

//    @UniJSMethod()
    public void endCall() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if(ContextCompat.checkSelfPermission(getUniContext(), Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions((Activity) getUniContext(), new String[]{Manifest.permission.ANSWER_PHONE_CALLS}, 10000);
                return;
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                TelecomManager telecomManager = (TelecomManager) getUniContext().getSystemService(Context.TELECOM_SERVICE);
                if (telecomManager != null) {
                    telecomManager.endCall();
                }
            }

            TelephonyManager telephonyManager = (TelephonyManager) getUniContext().getSystemService(Context.TELEPHONY_SERVICE);
            Class<?> classTelephony = Class.forName(telephonyManager.getClass().getName());
            Method methodGetITelephony = classTelephony.getDeclaredMethod("getITelephony");
            methodGetITelephony.setAccessible(true);
            Object telephonyInterface = methodGetITelephony.invoke(telephonyManager);
            Class<?> telephonyInterfaceClass = Class.forName(telephonyInterface.getClass().getName());
            Method methodEndCall = telephonyInterfaceClass.getDeclaredMethod("endCall");
            methodEndCall.invoke(telephonyInterface);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static SSLSocketFactory getSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{ new TrustAllCerts() }, new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static class TrustAllCerts implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {}

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {}

        @Override
        public X509Certificate[] getAcceptedIssuers() {return new X509Certificate[0];}
    }

    private static class TrustAllHostnameVerifier implements HostnameVerifier {

        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    public static final String[] DUAL_SIM_TYPES = {"subscription", "Subscription", "com.android.phone.extra.slot", "phone", "com.android.phone.DialingMode", "simId", "simnum", "phone_type", "simSlot"};

    /**
     * 指定使用的SIM
     * @param activity
     * @param intent
     * @param simIndex
     */
    private static void assignSIM(Activity activity, Intent intent, int simIndex) {
        if(RomUtil.isHuawei() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            for (String dualSimType : DUAL_SIM_TYPES) {
                intent.putExtra(dualSimType, simIndex);
            }
        } else if(RomUtil.isVivo() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            for (String dualSimType : DUAL_SIM_TYPES) {
                intent.putExtra(dualSimType, simIndex);
            }
        } else {
            if(getSIMStatus(activity) < 2) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                TelecomManager telManager = (TelecomManager) activity.getSystemService(Context.TELECOM_SERVICE);
                if (telManager != null) {
                    if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                        List<PhoneAccountHandle> phoneAccountHandleList = telManager.getCallCapablePhoneAccounts();
                        if (phoneAccountHandleList.size() > simIndex) {
                            PhoneAccountHandle phoneAccountHandle = phoneAccountHandleList.get(simIndex);
                            if (phoneAccountHandle != null)
                                intent.putExtra(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle);
                        }
                    }
                }
            }
        }
    }

    /**
     * 获取SIM状态
     * @param activity
     * @return
     */
    public static int getSIMStatus(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            TelecomManager telManager = (TelecomManager) activity.getSystemService(Context.TELECOM_SERVICE);
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                List<PhoneAccountHandle> phoneAccountHandleList = telManager.getCallCapablePhoneAccounts();
                for (int i = phoneAccountHandleList.size() - 1; i >= 0; i--) {
                    PhoneAccountHandle phoneAccountHandle = phoneAccountHandleList.get(i);
                    if(phoneAccountHandle.getId() == null || "".equals(phoneAccountHandle.getId()) || "null".equals(phoneAccountHandle.getId())) {
                        phoneAccountHandleList.remove(i);
                    }
                }
                if(phoneAccountHandleList.size() >= 2) return 2;

                for (PhoneAccountHandle phoneAccountHandle : phoneAccountHandleList) {
                    PhoneAccount phoneAccount = telManager.getPhoneAccount(phoneAccountHandle);
                    CharSequence charSequence = phoneAccount.getShortDescription();
                    Log.e("SIM_INFO", charSequence.toString());
                    char slotChar = charSequence.charAt(charSequence.length() - 1);
                    String slot = String.valueOf(slotChar);
                    if("0".equals(slot)) return 0;
                    if("1".equals(slot)) return 1;
                }
            }
        }
        return -1;
    }

    /**
     * 请求加入电池优化
     * @return true/false
     */
//    @UniJSMethod(uiThread = false)
    public boolean requsetIgnoreBattery() {
        Context context = getUniContext();
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        boolean hasIgnored = true;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            hasIgnored = powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
        }
        //  判断当前APP是否有加入电池优化的白名单，如果没有，弹出加入电池优化的白名单的设置对话框。
        if (!hasIgnored) {
            @SuppressLint("BatteryLife")
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            }
        }
        return hasIgnored;
    }

    /**
     * 开启前台服务
     */
//    @UniJSMethod()
    public void startForegroundService() {
        Context context = getUniContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(new Intent(context, MuCallService.class));
        } else {
            context.startService(new Intent(context, MuCallService.class));
        }
    }

    /**
     * 关闭前台服务
     */
//    @UniJSMethod()
    public void stopForegroundService() {
        Context context = getUniContext();
        context.stopService(new Intent(context, MuCallService.class));
    }

    /**
     * 返回SIM卡信息
     * @param simId 0卡1, 1卡2
     * @return SIM卡信息
     */
//    @UniJSMethod(uiThread = false)
    public JSONObject getSimInfo(int simId) {
        SimInfo simInfo = getSimInfo(getUniContext(), simId);
        JSONObject response = new JSONObject();
        response.put("number", simInfo.getNumber());
        response.put("operatorName", simInfo.getOperatorName());
        response.put("iccId", simInfo.getIccId());
        return response;
    }

    public static SimInfo getSimInfo(Context context, int simSlotIndex) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                SubscriptionManager subscriptionManager = context.getSystemService(SubscriptionManager.class);
                List<SubscriptionInfo> activeSubscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();

                if (activeSubscriptionInfoList != null && activeSubscriptionInfoList.size() > 0) {
                    for (int i = 0; i < activeSubscriptionInfoList.size(); i++) {
                        SubscriptionInfo subscriptionInfo = activeSubscriptionInfoList.get(i);
                        if (subscriptionInfo.getSimSlotIndex() == simSlotIndex) {
                            String number = subscriptionInfo.getNumber() != null ? subscriptionInfo.getNumber() : "";
                            String operatorName = subscriptionInfo.getCarrierName() != null ? subscriptionInfo.getCarrierName().toString() : "";
                            String iccId = subscriptionInfo.getIccId() != null ? subscriptionInfo.getIccId() : "";
                            return new SimInfo(number, operatorName, iccId);
                        }
                    }
                }
            }
        }
        return new SimInfo();
    }
}
