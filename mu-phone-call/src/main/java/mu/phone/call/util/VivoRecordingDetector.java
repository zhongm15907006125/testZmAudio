package mu.phone.call.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.provider.Settings;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class VivoRecordingDetector {

    /**
     * 检测vivo手机自动通话录音是否开启
     */
    public static boolean isVivoAutoCallRecordingEnabled(Context context) {
        // 方法1: 检查vivo系统设置
        if (isVivoSystemRecordingEnabled(context)) {
            return true;
        }

        // 方法2: 检查vivo录音应用状态
        if (isVivoRecordingAppEnabled(context)) {
            return true;
        }

        // 方法3: 检查通话设置中的录音选项
        return isVivoCallRecordingSettingEnabled(context);
    }

    /**
     * 方法1: 通过系统设置检测
     */
    private static boolean isVivoSystemRecordingEnabled(Context context) {
        try {
            // vivo常见的自动录音设置键值
            String[] settingKeys = {
                    "call_auto_record",
                    "auto_call_record",
                    "vivo_auto_record",
                    "call_recording_auto",
                    "voice_record_auto"
            };

            for (String key : settingKeys) {
                try {
                    int value = Settings.System.getInt(context.getContentResolver(), key);
                    if (value == 1) {
                        Log.d("zzmm", "找到自动录音设置: " + key);
                        return true;
                    }
                } catch (Settings.SettingNotFoundException e) {
                    // 继续检查下一个键值
                }
            }
        } catch (Exception e) {
            Log.e("zzmm", "检查系统设置失败: " + e.getMessage());
        }
        return false;
    }

    /**
     * 方法2: 检查vivo录音应用状态
     */
    private static boolean isVivoRecordingAppEnabled(Context context) {
        try {
            // vivo常见的录音相关包名
            String[] vivoRecordingPackages = {
                    "com.vivo.voiceassistant",
                    "com.vivo.voice",
                    "com.vivo.voicerecorder",
                    "com.bbk.recorder",
                    "com.bbk.voice"
            };

            PackageManager pm = context.getPackageManager();
            for (String packageName : vivoRecordingPackages) {
                try {
                    ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
                    if (info != null && info.enabled) {
                        // 检查应用是否有自动录音功能
                        if (hasVivoRecordingFeature(context, packageName)) {
                            return true;
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    // 应用未安装，继续检查
                }
            }
        } catch (Exception e) {
            Log.e("zzmm", "检查录音应用失败: " + e.getMessage());
        }
        return false;
    }

    /**
     * 方法3: 检查通话设置
     */
    private static boolean isVivoCallRecordingSettingEnabled(Context context) {
        try {
            // 通过Intent跳转到通话设置页面，但无法直接读取值
            // 这里只能通过一些特征来判断

            // 检查是否有通话录音相关的权限和功能
            return hasCallRecordingCapability(context);
        } catch (Exception e) {
            Log.e("zzmm", "检查通话设置失败: " + e.getMessage());
        }
        return false;
    }

    /**
     * 检查vivo录音应用特性
     */
    private static boolean hasVivoRecordingFeature(Context context, String packageName) {
        // 通过PackageManager检查应用声明的功能
        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);

            // 检查是否声明了录音权限
            if (packageInfo.requestedPermissions != null) {
                for (String permission : packageInfo.requestedPermissions) {
                    if (permission != null && permission.contains("RECORD_AUDIO")) {
                        return true;
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 检查设备通话录音能力
     */
    private static boolean hasCallRecordingCapability(Context context) {
        // 检查录音权限
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        // 测试是否支持通话录音
        return testCallRecordingSupport();
    }

    /**
     * 测试通话录音支持
     */
    private static boolean testCallRecordingSupport() {
        MediaRecorder recorder = null;
        try {
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile("/dev/null");
            recorder.prepare();
            recorder.release();
            return true;
        } catch (Exception e) {
            Log.d("VivoDetector", "VOICE_CALL音频源不支持: " + e.getMessage());

            // 尝试其他音频源
            try {
                if (recorder != null) {
                    recorder.release();
                }
                recorder = new MediaRecorder();
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                recorder.setOutputFile("/dev/null");
                recorder.prepare();
                recorder.release();
                return true;
            } catch (Exception e2) {
                Log.e("VivoDetector", "所有音频源测试失败: " + e2.getMessage());
                return false;
            }
        } finally {
            if (recorder != null) {
                recorder.release();
            }
        }
    }
}
