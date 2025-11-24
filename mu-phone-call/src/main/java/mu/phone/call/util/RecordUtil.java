package mu.phone.call.util;

import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;

public class RecordUtil {

    private final static String record_dir_key = "record_dir_key";
    private static int count;

    private static File searchRecordDir(long time) {
        File parent = Environment.getExternalStorageDirectory();
        File[] files = parent.listFiles();
        if (files != null && files.length > 0) {
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    count = 0;
                    File file = searchRecordFile(time, files[i], count);
                    if (file != null) {
                        return file;
                    }
                }
            }
        }
        return null;
    }

    private static File searchRecordFile(long time, File dir, int count) {
        //计算调用次数 --- 层级不必太多
        if (dir.isDirectory() && isNotRecordAppDir(dir) && count < 4) {
            File[] files = dir.listFiles();
            if (files != null && files.length > 0) {
                for (int i = 0; i < files.length; i++) {
                    File file = files[i];
                    //10秒之内生成的文件 默认为当前的录音文件
                    if (matchFileNameIsRecord(file.getName()) && file.lastModified() - time > -10 * 1000
                            && file.length() > 0 && file.isFile()) {
//                        saveRecordDir(file.getParent());
                        return file;
                    }
                    if (file.isDirectory()) {
                        return searchRecordFile(time, file, count + 1);
                    }
                }
            }
        }
        return null;
    }

    public static String getSystemRecord(String recordDir) {
        if(recordDir != null && recordDir.length() > 0) {
            File parent = Environment.getExternalStorageDirectory();
            File child = new File(parent, recordDir);
            if (child.exists()) {
                return child.getAbsolutePath();
            }
        }

        return getSystemRecord();
    }

    public static String getSystemRecord() {
        File parent = Environment.getExternalStorageDirectory();
        File child;
        if (RomUtil.isHuawei()) {
            child = new File(parent, "Sounds/CallRecord");
            if (!child.exists()) {
                child = new File(parent, "record");
            }
        } else if (RomUtil.isXiaomi()) {
            child = new File(parent, "MIUI/sound_recorder/call_rec");
        } else if (RomUtil.isMeizu()) {
            child = new File(parent, "Recorder");
        } else if (RomUtil.isOppo()) {
            child = new File(parent, "Music/Recordings/Call Recordings");
            if (!child.exists()) {
                child = new File(parent, "Recordings/Call Recordings");
                if (!child.exists()) {
                    child = new File(parent, "Recordings");
                }
            }
        } else if (RomUtil.isVivo()) {
            child = new File(parent, "录音/通话录音");
            if(!child.exists()) {
                child = new File(parent, "Recordings/Record/Call");
                if(!child.exists()) {
                    child = new File(parent, "Record/Call");
                }
            }
        } else if (RomUtil.isSamsung()) {
            child = new File(parent, "Sounds");
        } else if (RomUtil.isCoolpad()) {
            child = new File(parent, "My Records/Call Records");
            if(!child.exists()) {
                child = new File(parent, "CallRecord");
            }
        } else {
            child = new File(parent, "");
        }

        if (!child.exists()) {
            return null;
        }
        return child.getAbsolutePath();
    }
    //常用系统录音文件存放文件夹
    private static ArrayList<String> getRecordFiles() {
        String parentPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        ArrayList<String> list = new ArrayList<>();
        File file = new File(parentPath, "record");
        if (file.exists()) {
            list.add(file.getAbsolutePath());
        }
        file = new File(parentPath, "Sounds/CallRecord");
        if (file.exists()) {
            list.add(file.getAbsolutePath());
        }
        file = new File(parentPath, "MIUI/sound_recorder/call_rec");
        if (file.exists()) {
            list.add(file.getAbsolutePath());
        }
        file = new File(parentPath, "Recorder");
        if (file.exists()) {
            list.add(file.getAbsolutePath());
        }
        file = new File(parentPath, "Recordings/Call Recordings");
        if (file.exists()) {
            list.add(file.getAbsolutePath());
        }
        file = new File(parentPath, "Recordings");
        if (file.exists()) {
            list.add(file.getAbsolutePath());
        }
        file = new File(parentPath, "Record/Call");
        if (file.exists()) {
            list.add(file.getAbsolutePath());
        }
        file = new File(parentPath, "Sounds");
        if (file.exists()) {
            list.add(file.getAbsolutePath());
        }
        //oppp android-10 手机存储系统录音
        file = new File(parentPath, "Music/Recordings/Call Recordings");
        if (file.exists()) {
            list.add(file.getAbsolutePath());
        }

        file = new File(parentPath, "PhoneRecord");
        if (file.exists()) {
            list.add(file.getAbsolutePath());
        }

        // 或者其余机型系统录音文件夹 添加
        return list;
    }

    //寻找文件
    public static File getFile() {
        try {
            long time = Calendar.getInstance().getTimeInMillis();
            File dir;
            // 使用固定系统下文件夹下搜索
            String recordDir = getSystemRecord();
            if (!TextUtils.isEmpty(recordDir)) {
                dir = new File(recordDir);
                return getRecordFile(time, dir);
            }

            // 使用常用系统下文件夹下搜索
            ArrayList<String> recordFiles = getRecordFiles();
            for (int i = 0; i < recordFiles.size(); i++) {
                dir = new File(recordFiles.get(i));
                return getRecordFile(time, dir);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private static File getRecordFile(long time, File dir) {
        if (dir.isDirectory() && isNotRecordAppDir(dir)) {
            File[] files = dir.listFiles();
            if (files != null && files.length > 0) {
                for (int i = 0; i < files.length; i++) {
                    File file = files[i];
                    //20秒之内生成的文件 默认为当前的录音文件(TODO 这里如果需要更准确可以判断是否是录音,录音时长校对)
                    if (matchFileNameIsRecord(file.getName()) && file.lastModified() - time > -20 * 1000
                            && file.length() > 0 && file.isFile()) {
                        return file;
                    }
                }
            }
        }
        return null;
    }

    private static boolean isNotRecordAppDir(File dir) {
        String name = dir.getName();
        if ("Android".equals(name)) {
            return false;
        } else if ("不是录音文件夹都可以写在这".equals(name)) {
            return false;
        }

        //加入一些会录音的app,会生成录音文件,防止使用其他录音文件而没有使用系统录音文件
        return true;
    }

    private static boolean matchFileNameIsRecord(String name) {
        //录音文件匹配规则 -- 可以自行添加其他格式录音匹配
        try {
            if (name.toLowerCase().endsWith(".mp3".toLowerCase())) {
                return true;
            } else if (name.toLowerCase().endsWith(".wav".toLowerCase())) {
                return true;
            } else if (name.toLowerCase().endsWith(".3gp".toLowerCase())) {
                return true;
            }
//            else if (name.toLowerCase().endsWith(".mp4".toLowerCase())) {
//                return true;
//            }
            else if (name.toLowerCase().endsWith(".amr".toLowerCase())) {
                return true;
            } else if (name.toLowerCase().endsWith(".3gpp".toLowerCase())) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}