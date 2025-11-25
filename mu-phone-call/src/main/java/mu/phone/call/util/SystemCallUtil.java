package mu.phone.call.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

/**
 * @author LiYejun
 * @date 2022/9/6
 */
public class SystemCallUtil {

    public static boolean goSetSysLuyin(Context context) {
        try {
            if (RomUtil.isHuawei()) {
                startHuaWeiRecord(context);
            } else if (RomUtil.isXiaomi()) {
                startXiaomiRecord(context);
            } else if (Build.MODEL.equals("PFUM10") || Build.MODEL.equals("PGGM10")) {
                startPFum(context);
            } else if (RomUtil.isOppo()) {
                startOppoRecord(context);
            } else if (RomUtil.isSamsung()) {
                startSanXinRecord(context);
            } else if (RomUtil.isMeizu()) {
                startMeizuRecord(context);
            } else if (RomUtil.isOneplus()) {
                startOnePlusRecord(context);
            } else if (RomUtil.isLenovo()) {
                startlvcRecord(context);
            } else if (Build.BRAND.equals("realme")) {
                startRealmeRecord(context);
            } else if (RomUtil.isLeeco()) {
                startleecoRecord(context);
            } else if (RomUtil.isVivo()) {
                startVivioRecord(context);
            } else if (RomUtil.isCoolpad()) {
                startCoolpadRecord(context);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void startPFum(Context context) {
        ComponentName componentName = new ComponentName("com.android.phone", "com.android.phone.OplusCallFeaturesSetting");
        Intent intent = new Intent();
        intent.setComponent(componentName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }


    private static void startVivioRecord(Context context) {
        ComponentName componentName = new ComponentName("com.android.incallui", "com.android.incallui.record.CallRecordSetting");
        Intent intent = new Intent();
        intent.setComponent(componentName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private static void startleecoRecord(Context context) {
        ComponentName componentName = new ComponentName("com.android.phone", "com.letv.leui.phone.AutoRecordSetting");
        Intent intent = new Intent();
        intent.setComponent(componentName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private static void startlvcRecord(Context context) {
        ComponentName componentName = new ComponentName("com.zui.callsettings", "com.zui.callsettings.XCallAutoStartRecord");
        Intent intent = new Intent();
        intent.setComponent(componentName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private static void startOnePlusRecord(Context context) {
        ComponentName componentName = new ComponentName("com.android.dialer", "com.android.dialer.oneplus.activity.OPAutoRecordingSettings");
        Intent intent = new Intent();
        intent.setComponent(componentName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private static void startXiaomiRecord(Context context) {
        ComponentName componentName = new ComponentName("com.android.phone", "com.android.phone.settings.CallRecordSetting");
        Intent intent = new Intent();
        intent.setComponent(componentName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private static void startHuaWeiRecord(Context context) {
        ComponentName componentName = new ComponentName("com.android.phone", "com.android.phone.MSimCallFeaturesSetting");
        Intent intent = new Intent();
        intent.setComponent(componentName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private static void startOppoRecord(Context context) {
        try {
            ComponentName componentName = new ComponentName("com.android.phone", "com.android.phone.OppoCallFeaturesSetting");
            Intent intent = new Intent();
            intent.setComponent(componentName);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception unused) {
            startRealmeRecord(context);
        }
    }

    private static void startMeizuRecord(Context context) {
        ComponentName componentName = new ComponentName("com.meizu.callsetting", "com.meizu.callsetting.AutoRecordActivity");
        Intent intent = new Intent();
        intent.setComponent(componentName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private static void startRealmeRecord(Context context) {
        ComponentName componentName = new ComponentName("com.android.phone", "com.android.phone.OppoCallFeaturesSetting");
        Intent intent = new Intent();
        intent.setComponent(componentName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private static void startSanXinRecord(Context context) {
        ComponentName componentName = new ComponentName("com.samsung.android.app.telephonyui", "com.samsung.android.app.telephonyui.callsettings.ui.preference.CallSettingsActivity");
        Intent intent = new Intent();
        intent.setComponent(componentName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private static void startCoolpadRecord(Context context) {
        ComponentName componentName = new ComponentName("com.android.dialer", "com.journeyui.phone.setting.DialerFeaturesSetting");
//        ComponentName componentName = new ComponentName("com.android.dialer", "com.journeyui.phone.setting.CallAutoRecordContactActivity");
        Intent intent = new Intent();
        intent.setComponent(componentName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static boolean checkIsOpenAudioRecord(Context context) {
        if (RomUtil.isXiaomi()) {
            return checkXiaomiRecord(context);
        }
        if (RomUtil.isHuawei()) {
            return checkHuaweiRecord(context);
        }
        if (Build.MODEL.equals("PFUM10") || Build.MODEL.equals("PGGM10")) {
            return checkOppoPFUMRecord(context);
        }
        if (RomUtil.isOppo()) {
            return checkOppoRecord(context);
        }
//        if (!RomUtil.isLeeco()) {
//            return true;
//        }
        return VivoRecordingDetector.isVivoAutoCallRecordingEnabled(context);
    }

    private static boolean checkXiaomiRecord(Context context) {
        try {
            return Settings.System.getInt(context.getContentResolver(), "button_auto_record_call") != 0;
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean checkLeshiRecord(Context context) {
        try {
            return Settings.Global.getInt(context.getContentResolver(), "leui_call_auto_record") == 0;
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean queryVivoSettingsViaContentProvider(Context context) {
        try {
            // vivo可能使用的ContentProvider URI
            String[] providerUris = {
                    "content://com.vivo.settings/settings",
                    "content://com.bbk.settings/settings",
                    "content://com.vivo.globalsearch/settings"
            };

            for (String uriString : providerUris) {
                try {
                    Cursor cursor = context.getContentResolver().query(
                            Uri.parse(uriString),
                            null,
                            "name=?",
                            new String[]{"call_auto_record"},
                            null
                    );

                    if (cursor != null && cursor.moveToFirst()) {
                        int valueIndex = cursor.getColumnIndex("value");
                        if (valueIndex != -1) {
                            int value = cursor.getInt(valueIndex);
                            cursor.close();
                            return value == 1;
                        }
                        cursor.close();
                    }
                } catch (Exception e) {
                    // 继续尝试下一个URI
                }
            }
        } catch (Exception e) {
            Log.e("VivoDetector", "ContentProvider查询失败: " + e.getMessage());
        }
        return false;
    }

    private static boolean checkHuaweiRecord(Context context) {
        try {
            return Settings.Secure.getInt(context.getContentResolver(), "enable_record_auto_key") != 0;
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean checkOppoRecord(Context context) {
        try {
            return (Build.VERSION.SDK_INT >= 17 ? Settings.Global.getInt(context.getContentResolver(), "oppo_all_call_audio_record") : 0) != 0;
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean checkOppoPFUMRecord(Context context) {
        try {
            return (Build.VERSION.SDK_INT >= 17 ? Settings.Global.getInt(context.getContentResolver(), "oplus_customize_all_call_audio_record") : 0) != 0;
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }


}
