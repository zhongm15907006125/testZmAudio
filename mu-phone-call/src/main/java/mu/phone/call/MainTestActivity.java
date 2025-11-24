package mu.phone.call;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

public class MainTestActivity extends AppCompatActivity {
    PhoneCallModule phoneCallModule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void makePhoneCall(View view) {
//        phoneCallModule.makePhoneCall();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        phoneCallModule.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}