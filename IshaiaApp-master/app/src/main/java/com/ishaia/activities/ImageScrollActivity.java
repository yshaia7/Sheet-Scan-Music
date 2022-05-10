package com.ishaia.activities;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.github.chrisbanes.photoview.PhotoView;
import com.ishaia.R;

/**
 * That class stand for play the user MIDI file
 * after the app receive the MIDI file and the
 * the picture from the server,the picture will
 * open and the song will be ready for playing
 * in that class initialize all the view and
 * buttons that need for show the picture and
 * play the MIDI file.
 * <p>
 * we have 2 diff oprion to play the song in
 * LANDSCAPE and PORTRAIT
 */
public class ImageScrollActivity extends AppCompatActivity {

    /* constant */
    private static final int mesuringDivideNumber = 1000;

    /* global declaration */
    private ScrollView scrollView;
    private PhotoView ivNote;
    private ToggleButton tbPlay;
    private ToggleButton tbScroll;
    private MediaPlayer mp;
    private SeekBar seekBar;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private LinearLayout llScrollController;
    private int delayedMillis = 50;
    private int scrolledPixel = 1;

    private String path = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_scroll);
        getSupportActionBar().hide(); /* Hiding Action bar from this screen. */

        findViewById();

        Intent intent = getIntent();

        /* Checking whether img url is available or not */
        if (intent.hasExtra("imgPath")) {
            path = getIntent().getStringExtra("musicPath");

            //  if not network request load it from cache.
            boolean fromLocal = intent.hasExtra("fromLocal");

            /* create what required for the image and song that receive from the server */
            Glide.with(this)
                    .load(intent.getStringExtra("imgPath"))
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(!fromLocal)
                    .error(R.drawable.no_image_found)
                    .placeholder(R.drawable.placeholder)
                    .addListener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                    Target<Drawable> target,
                                                    boolean isFirstResource) {
                            // If glide is failed to load image from url then disable seek bar and music play button
                            tbPlay.setEnabled(false);
                            seekBar.setEnabled(false);
                            return false;
                        }

                        /* set the image and seek bar of the song*/
                        @Override
                        public boolean onResourceReady(Drawable resource, Object model,
                                                       Target<Drawable> target,
                                                       DataSource dataSource,
                                                       boolean isFirstResource) {
                            tbPlay.setEnabled(true);
                            seekBar.setEnabled(true);
                            ivNote.setImageDrawable(resource); // set the image into image view
                            return true;
                        }
                    })
                    .into(ivNote);
            /* init seek bar*/
            initSeekBarWithMusicPlayer();
        }
        initScrollHandler();
    }

    /*  Initializing & syncing seekBar with music player */
    private void initSeekBarWithMusicPlayer() {
        seekBar.setEnabled(true);
        tbPlay.setEnabled(true);
        Handler mHandler = new Handler();
        tbPlay.setOnCheckedChangeListener((buttonView, isChecked) -> playMusic(isChecked));
        /*  Set seekBar with music duration */
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mp != null) {
                    int mCurrentPosition = mp.getCurrentPosition() / mesuringDivideNumber;
                    seekBar.setProgress(mCurrentPosition);
                }
                mHandler.postDelayed(this, mesuringDivideNumber);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                /*  seek music duration through seekBar */
                if (mp != null && fromUser) {
                    mp.seekTo(progress * 1000);
                }
            }
        });
    }

    /* for change the view of the playing music mode*/
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            llScrollController.setVisibility(View.INVISIBLE);
            tbScroll.setChecked(false);
        } else {
            llScrollController.setVisibility(View.VISIBLE);
        }
    }

    /* here we define what is need for the play song view*/
    @SuppressLint("SourceLockedOrientationActivity")
    private void findViewById() {
        /* define the buttons */
        scrollView = findViewById(R.id.scrollView);
        ivNote = findViewById(R.id.ivImage);
        tbPlay = findViewById(R.id.tbPlay);
        seekBar = findViewById(R.id.seekBar);
        tbScroll = findViewById(R.id.tbScroll);
        ToggleButton tbRotate = findViewById(R.id.tbRotate);

        llScrollController = findViewById(R.id.llScrollController);
        /* for change the view of the playing music mode*/
        tbRotate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
                } else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
                }
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            }
        });
        tbPlay.setEnabled(false);
        seekBar.setEnabled(false);

    }


    /*  Setting up image scroll handler */
    private void initScrollHandler() {
        timerHandler = new Handler();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                // 1 is how many pixels you want it to scroll vertically by
                scrollView.smoothScrollBy(0, scrolledPixel);
                // 50 is how many milliseconds you want this thread to run
                timerHandler.postDelayed(this, delayedMillis);
            }
        };

        scrollView.getViewTreeObserver()
                .addOnScrollChangedListener(() -> {
                    /* here we change vertically the scroll up and down*/
                    if (!scrollView.canScrollVertically(1)) {
                        // bottom of scroll view
                        tbScroll.setChecked(false);
                    }
                    if (!scrollView.canScrollVertically(-1)) {
                        // top of scroll view
                        tbScroll.setChecked(false);
                    }
                });

        /* set  up and down vertical scroll */
        findViewById(R.id.ivScrollUp).setOnClickListener(v -> {
            if (scrolledPixel > 1)
                scrolledPixel -= 1;
        });

        findViewById(R.id.ivScrollDown).setOnClickListener(v -> {
            scrolledPixel += 1;
        });

        tbScroll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) { /* If isChecked -> enable scroll else disable it */
                timerHandler.post(timerRunnable);
            } else {
                timerHandler.removeCallbacks(timerRunnable);
            }
        });
    }

    /*  Controlling media player playback */
    public void playMusic(boolean play) {
        try {
            if (mp == null) {
                initMusicPlayer();
            }
            if (play) {
                mp.start();
            } else {
                if (mp.isPlaying()) {
                    mp.pause();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*  Initializing music player */
    private void initMusicPlayer() {
        try {
            mp = new MediaPlayer();
            mp.setDataSource(path);
            mp.setOnCompletionListener(mp1 -> {
                mp.seekTo(0);
                tbPlay.setChecked(false);
            });
            mp.prepare();
            seekBar.setMax(mp.getDuration() / mesuringDivideNumber);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* When activity get closed pause music */
    @Override
    protected void onPause() {
        super.onPause();
        tbPlay.setChecked(false);
    }
}
