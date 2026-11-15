package areeb.xposed.eggster.eggs.pie;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** Entry point for the "Pie" egg: opens PAINT.APK (the drawing egg) directly. */
public class PlatLogoActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, PaintActivity.class));
        finish();
    }
}
