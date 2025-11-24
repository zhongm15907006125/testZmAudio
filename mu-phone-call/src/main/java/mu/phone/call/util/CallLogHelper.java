package mu.phone.call.util;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.provider.CallLog;
import android.util.Log;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * author：jun
 * date：2021/11/12
 */
public class CallLogHelper extends Thread {

    private final Context mContext;
    private final long beginCallTime;
    private final OnFetchNewCallLogCallback mCallback;
    private final String mPhoneNumber;

    public CallLogHelper(Context context, String phoneNumber, OnFetchNewCallLogCallback callback) {
        super();
        mContext = context;
        mPhoneNumber = phoneNumber;
        beginCallTime = System.currentTimeMillis();
        mCallback = callback;
    }

    @Override
    public void run() {
        super.run();
        ContentResolver resolver = mContext.getContentResolver();

        long startQueryTime = System.currentTimeMillis();

        int readTimes = 0;
        boolean readComplete = false;

        while (!readComplete) {
            try {
                Log.i("休息500毫秒后开始寻找数据", String.valueOf(readTimes));
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Cursor cursor = resolver.query(CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DEFAULT_SORT_ORDER);
            if(cursor != null) {
                if(cursor.getCount() > 0) {
                    cursor.moveToFirst();
                    String number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
                    long dateLong = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));
                    int duration = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.DURATION));
                    int type = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE));
                    String location = cursor.getString(cursor.getColumnIndex(CallLog.Calls.GEOCODED_LOCATION));
                    long lastModified = cursor.getLong(cursor.getColumnIndex("last_modified"));

                    // 比较拨号时间 不能大于 通话时间 防止一次找到历史的通话记录
                    if(beginCallTime <= dateLong &&  mPhoneNumber.equals(number)) {
                        // TODO 获取成功
                        readComplete = true;
                        Map<String, Object> callLogMap = new LinkedHashMap<>();
                        callLogMap.put("number", number);
                        callLogMap.put("talkTime", dateLong);
                        callLogMap.put("lastModified", lastModified);
                        callLogMap.put("duration", duration);
                        callLogMap.put("type", type);
                        callLogMap.put("location", location);
                        callLogMap.put("userTime", System.currentTimeMillis() - startQueryTime);

                        Message msg = Message.obtain();
                        msg.what = 1;
                        msg.obj = callLogMap;
                        Log.i("DDD 找到了通话记录", callLogMap.toString());
                        mHandler.sendMessage(msg);
                    }
                }
                cursor.close();
            }
            if(!readComplete) {
                readTimes ++;
                Log.i("DDD 继续尝试寻找", String.valueOf(readTimes));
                if(readTimes > 4) {
                    // TODO 读取超过2秒，停止读取  找不到通话记录  应该把通话时长改为0
                    readComplete = true;
                    Map<String, Object> callLogMap = new LinkedHashMap<>();
                    callLogMap.put("number", mPhoneNumber);
                    callLogMap.put("talkTime", beginCallTime);
                    callLogMap.put("duration", 0);  // 找都找不到 肯定通话时长为0
                    callLogMap.put("type", 1);
                    callLogMap.put("userTime", System.currentTimeMillis() - startQueryTime);
                    Message msg = Message.obtain();
                    msg.what = 1;
                    msg.obj = callLogMap;
                    mHandler.sendMessage(msg);
                }
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Map<String, Object> callLogMap = (Map<String, Object>) msg.obj;
            if(mCallback != null) mCallback.onResult(callLogMap);
        }
    };

    public interface OnFetchNewCallLogCallback {

        void onResult(Map<String, Object> callLogMap);
    }
}
