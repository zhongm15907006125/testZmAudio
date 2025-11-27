package mu.phone.call;

import com.alibaba.fastjson.JSONObject;

public interface PermissionCallback {
    int MAKE_PHONE_CALL = 1001;
    int GET_ALL_RECORD_FILES = 1004;
    int GET_TOP5_RECORD_FILES = 1005;
    int UPLOAD_RECORD_FILE = 1006;
    int ANSWER_PHONE_CALLS = 1007;

    void onPermissionGranted();
}
