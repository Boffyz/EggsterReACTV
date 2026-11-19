package areeb.xposed.eggster.eggs.fifteen;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import areeb.xposed.eggster.eggs.landroid.MainActivity;

/** Entry point for the "Android 15" egg: opens Landroid, the space game. */
public class PlatLogoActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
