package areeb.xposed.eggster.screensavers;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
    private CheckBox customBox;
    private int lastCustomCount;
    private RadioGroup bgGroup;
    private ActivityResultLauncher<String[]> bgPicker;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

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
        setupAngle();
        setupSpin();
        setupRotation();
        setupBackground();
        setupButtons();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // coming back from the "My pictures" page: refresh the count and the preview if it changed
        final int count = CustomPictures.count(this);
        if (count != lastCustomCount) {
            lastCustomCount = count;
            if (customBox != null) {
                customBox.setText(getString(R.string.ss_set_custom_count, count));
            }
            preview.reloadSprites();
        }
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
            if (SpriteLibrary.CUSTOM.equals(setId)) {
                customBox = box;
                lastCustomCount = CustomPictures.count(this);
                box.setText(getString(R.string.ss_set_custom_count, lastCustomCount));
            }
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

    private void setupAngle() {
        final TextView angleLabel = (TextView) findViewById(R.id.angle_label);
        final SeekBar angleSeek = (SeekBar) findViewById(R.id.angle_seek);
        angleSeek.setMax(ScreensaverSettings.ANGLE_LEVELS - 1);
        angleSeek.setProgress(prefs.getAngle());
        angleLabel.setText(getString(R.string.ss_angle, prefs.getAngle() * ScreensaverSettings.ANGLE_STEP));
        angleSeek.setOnSeekBarChangeListener(new SliderListener() {
            @Override
            void onValue(int progress) {
                prefs.setAngle(progress);
                angleLabel.setText(getString(R.string.ss_angle, progress * ScreensaverSettings.ANGLE_STEP));
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

    private void setupRotation() {
        final TextView rotLabel = (TextView) findViewById(R.id.rot_label);
        final SeekBar rotSeek = (SeekBar) findViewById(R.id.rot_seek);
        rotSeek.setMax(ScreensaverSettings.ROT_MAX - ScreensaverSettings.ROT_MIN);
        rotSeek.setProgress(prefs.getRotSpeed() - ScreensaverSettings.ROT_MIN);
        rotLabel.setText(getString(R.string.ss_rot_speed, prefs.getRotSpeed()));
        rotSeek.setOnSeekBarChangeListener(new SliderListener() {
            @Override
            void onValue(int progress) {
                final int value = ScreensaverSettings.ROT_MIN + progress;
                prefs.setRotSpeed(value);
                rotLabel.setText(getString(R.string.ss_rot_speed, value));
                preview.loadParams();
            }
        });

        final RadioGroup group = (RadioGroup) findViewById(R.id.rot_dir_group);
        final int dir = prefs.getRotDir();
        if (dir == ScreensaverSettings.DIR_CW) {
            group.check(R.id.dir_cw);
        } else if (dir == ScreensaverSettings.DIR_CCW) {
            group.check(R.id.dir_ccw);
        } else {
            group.check(R.id.dir_mixed);
        }
        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                if (checkedId == R.id.dir_cw) {
                    prefs.setRotDir(ScreensaverSettings.DIR_CW);
                } else if (checkedId == R.id.dir_ccw) {
                    prefs.setRotDir(ScreensaverSettings.DIR_CCW);
                } else {
                    prefs.setRotDir(ScreensaverSettings.DIR_MIXED);
                }
                preview.loadParams();
            }
        });

        final CheckBox tumbleBox = (CheckBox) findViewById(R.id.tumble_check);
        tumbleBox.setChecked(prefs.isTumble());
        tumbleBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton button, boolean checked) {
                prefs.setTumble(checked);
                preview.loadParams();
            }
        });
    }

    private void setupBackground() {
        bgGroup = (RadioGroup) findViewById(R.id.bg_group);
        switch (prefs.getBackground()) {
            case SpaceBackground.DEEP_SPACE:
                bgGroup.check(R.id.bg_space);
                break;
            case SpaceBackground.PINK_NEBULA:
                bgGroup.check(R.id.bg_pink);
                break;
            case SpaceBackground.TEAL_NEBULA:
                bgGroup.check(R.id.bg_teal);
                break;
            case SpaceBackground.CUSTOM:
                bgGroup.check(R.id.bg_custom);
                break;
            default:
                bgGroup.check(R.id.bg_black);
                break;
        }
        bgGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                final int style;
                if (checkedId == R.id.bg_space) {
                    style = SpaceBackground.DEEP_SPACE;
                } else if (checkedId == R.id.bg_pink) {
                    style = SpaceBackground.PINK_NEBULA;
                } else if (checkedId == R.id.bg_teal) {
                    style = SpaceBackground.TEAL_NEBULA;
                } else if (checkedId == R.id.bg_custom) {
                    style = SpaceBackground.CUSTOM;
                } else {
                    style = SpaceBackground.BLACK;
                }
                prefs.setBackground(style);
                preview.loadParams();
            }
        });

        bgPicker = registerForActivityResult(new ActivityResultContracts.OpenDocument(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(final Uri uri) {
                        if (uri == null) {
                            return;
                        }
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                final boolean ok = SpaceBackground.importCustom(getApplicationContext(), uri);
                                mainHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (ok) {
                                            prefs.setBackground(SpaceBackground.CUSTOM);
                                            bgGroup.check(R.id.bg_custom);
                                            preview.loadParams();
                                            preview.reloadBackground();
                                        } else {
                                            Toast.makeText(ScreensaversActivity.this,
                                                    R.string.ss_bg_failed, Toast.LENGTH_LONG).show();
                                        }
                                    }
                                });
                            }
                        }, "screensaver-background-import").start();
                    }
                });
        findViewById(R.id.bg_choose_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bgPicker.launch(new String[]{"image/*"});
            }
        });
    }

    private void setupButtons() {
        findViewById(R.id.pictures_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ScreensaversActivity.this, ScreensaverPicturesActivity.class));
            }
        });
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
