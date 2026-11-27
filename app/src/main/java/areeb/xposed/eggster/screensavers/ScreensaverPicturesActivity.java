package areeb.xposed.eggster.screensavers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.util.List;

import areeb.xposed.eggster.R;

/** "My pictures": add images from the phone, turn each one 90 degrees, or remove it. */
public class ScreensaverPicturesActivity extends AppCompatActivity {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private LinearLayout listContainer;
    private TextView emptyView;
    private ActivityResultLauncher<String[]> picker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screensaver_pictures);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.ss_pictures_title);
        }

        listContainer = (LinearLayout) findViewById(R.id.pictures_list);
        emptyView = (TextView) findViewById(R.id.pictures_empty);

        picker = registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(),
                new ActivityResultCallback<List<Uri>>() {
                    @Override
                    public void onActivityResult(List<Uri> uris) {
                        if (uris != null && !uris.isEmpty()) {
                            importPictures(uris);
                        }
                    }
                });
        findViewById(R.id.pictures_add_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                picker.launch(new String[]{"image/*"});
            }
        });

        refresh();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void importPictures(final List<Uri> uris) {
        final Context app = getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final int added = CustomPictures.importAll(app, uris);
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ScreensaverPicturesActivity.this,
                                added > 0 ? getString(R.string.ss_pictures_added, added)
                                        : getString(R.string.ss_pictures_failed),
                                Toast.LENGTH_LONG).show();
                        refresh();
                    }
                });
            }
        }, "screensaver-import").start();
    }

    private View.OnClickListener rotateListener(final File file, final int degrees) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        CustomPictures.rotate(file, degrees);
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                refresh();
                            }
                        });
                    }
                }, "screensaver-rotate").start();
            }
        };
    }

    private void refresh() {
        listContainer.removeAllViews();
        final List<File> files = CustomPictures.list(this);
        emptyView.setVisibility(files.isEmpty() ? View.VISIBLE : View.GONE);

        final LayoutInflater inflater = LayoutInflater.from(this);
        int number = 0;
        for (final File file : files) {
            number++;
            final View row = inflater.inflate(R.layout.item_custom_picture, listContainer, false);
            ((TextView) row.findViewById(R.id.picture_name))
                    .setText(getString(R.string.ss_picture_item, number));

            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4; // small preview: much cheaper than the full picture
            final Bitmap thumb = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            if (thumb != null) {
                ((ImageView) row.findViewById(R.id.picture_thumb)).setImageBitmap(thumb);
            }

            row.findViewById(R.id.picture_rotate_left).setOnClickListener(rotateListener(file, -90));
            row.findViewById(R.id.picture_rotate_right).setOnClickListener(rotateListener(file, 90));
            row.findViewById(R.id.picture_remove).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CustomPictures.delete(file);
                    refresh();
                }
            });
            listContainer.addView(row);
        }
    }
}
