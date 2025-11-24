package mu.phone.call;

import com.alibaba.fastjson.JSONObject;

public interface JsonCallback {

    void invoke(JSONObject obj);

    void invokeAndKeepAlive(JSONObject data);
}
