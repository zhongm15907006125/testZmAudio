package mu.phone.call;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.dcloud.feature.uniapp.UniAppHookProxy;

/**
 * @author LiYejun
 * @date 2023/6/16
 */
public class PhoneCallAppProxy implements UniAppHookProxy {

    private static int appCount;

    public static int getAppCount() {
        return appCount;
    }

    @Override
    public void onCreate(Application application) {
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                appCount++;
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {

            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {

            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                appCount--;
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {

            }
        });
    }

    @Override
    public void onSubProcessCreate(Application application) {

    }
}
