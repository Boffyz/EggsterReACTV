package areeb.xposed.eggster.screensavers;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.Set;

import areeb.xposed.eggster.R;

/** The Screensavers page: choose what flies, tune the animation, preview it, set it as the phone's screensaver. */
public class ScreensaversActivity extends AppCompatActivity {

    private ScreensaverPrefs prefs;
    private FlyingView preview;
    private TextView speedLabel;
    private TextView countLabel;
    private TextView sizeLabel;

    /** A slider listener that only needs to know the new position. */
    private abstract static class SliderListener implements SeekBar.OnSeekBarChangeListener {
        abstract void onValue(int progress);

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            onValue(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screensavers);
        prefs = new ScreensaverPrefs(this);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.ss_title);
        }

        preview = (FlyingView) findViewById(R.id.preview);
        speedLabel = (TextView) findViewById(R.id.speed_label);
        countLabel = (TextView) findViewById(R.id.count_label);
        sizeLabel = (TextView) findViewById(R.id.size_label);

        buildSetCheckboxes();
        setupSliders();
        setupSpin();
        setupButtons();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void buildSetCheckboxes() {
        final LinearLayout container = (LinearLayout) findViewById(R.id.sets_container);
        final Set<String> selected = prefs.getSets();
        for (final String setId : SpriteLibrary.SET_IDS) {
            final CheckBox box = new CheckBox(this);
            box.setText(SpriteLibrary.labelRes(setId));
            box.setChecked(selected.contains(setId));
            box.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton button, boolean checked) {
                    final Set<String> sets = prefs.getSets();
                    if (checked) {
                        sets.add(setId);
                    } else {
                        sets.remove(setId);
                    }
                    prefs.setSets(sets);
                    preview.reloadSprites();
                }
            });
            container.addView(box);
        }
    }

    private void setupSliders() {
        final SeekBar speedSeek = (SeekBar) findViewById(R.id.speed_seek);
        speedSeek.setMax(ScreensaverSettings.SPEED_MAX - ScreensaverSettings.SPEED_MIN);
        speedSeek.setProgress(prefs.getSpeed() - ScreensaverSettings.SPEED_MIN);
        speedLabel.setText(getString(R.string.ss_speed, prefs.getSpeed()));
        speedSeek.setOnSeekBarChangeListener(new SliderListener() {
            @Override
            void onValue(int progress) {
                final int value = ScreensaverSettings.SPEED_MIN + progress;
                prefs.setSpeed(value);
                speedLabel.setText(getString(R.string.ss_speed, value));
                preview.loadParams();
            }
        });

        final SeekBar countSeek = (SeekBar) findViewById(R.id.count_seek);
        countSeek.setMax((ScreensaverSettings.COUNT_MAX - ScreensaverSettings.COUNT_MIN)
                / ScreensaverSettings.COUNT_STEP);
        countSeek.setProgress((prefs.getCount() - ScreensaverSettings.COUNT_MIN)
                / ScreensaverSettings.COUNT_STEP);
        countLabel.setText(getString(R.string.ss_count, prefs.getCount()));
        countSeek.setOnSeekBarChangeListener(new SliderListener() {
            @Override
            void onValue(int progress) {
                final int value = ScreensaverSettings.COUNT_MIN
                        + progress * ScreensaverSettings.COUNT_STEP;
                prefs.setCount(value);
                countLabel.setText(getString(R.string.ss_count, value));
                preview.loadParams();
            }
        });

        final SeekBar sizeSeek = (SeekBar) findViewById(R.id.size_seek);
        sizeSeek.setMax(ScreensaverSettings.SIZE_MAX - ScreensaverSettings.SIZE_MIN);
        sizeSeek.setProgress(prefs.getSize() - ScreensaverSettings.SIZE_MIN);
        sizeLabel.setText(getString(R.string.ss_size, prefs.getSize()));
        sizeSeek.setOnSeekBarChangeListener(new SliderListener() {
            @Override
            void onValue(int progress) {
                final int value = ScreensaverSettings.SIZE_MIN + progress;
                prefs.setSize(value);
                sizeLabel.setText(getString(R.string.ss_size, value));
                preview.loadParams();
            }
        });
    }

    private void setupSpin() {
        final CheckBox spinBox = (CheckBox) findViewById(R.id.spin_check);
        spinBox.setChecked(prefs.isSpin());
        spinBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean checked) {
                prefs.setSpin(checked);
                preview.loadParams();
            }
        });
    }

    private void setupButtons() {
        findViewById(R.id.preview_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ScreensaversActivity.this, ScreensaverPreviewActivity.class));
            }
        });
        findViewById(R.id.dream_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDreamSettings();
            }
        });
    }

    private void openDreamSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_DREAM_SETTINGS));
        } catch (ActivityNotFoundException e) {
            try {
                startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS));
            } catch (ActivityNotFoundException e2) {
                Toast.makeText(this, R.string.ss_dream_missing, Toast.LENGTH_LONG).show();
            }
        }
    }
}
