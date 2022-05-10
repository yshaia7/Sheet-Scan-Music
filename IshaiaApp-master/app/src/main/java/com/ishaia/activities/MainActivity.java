package com.ishaia.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ishaia.R;
import com.ishaia.utils.Const;

/**
 * That class is the main class where the app start to run
 * that class show the home page.
 */
public class MainActivity extends AppCompatActivity {

    /* define buttons */
    private ImageButton btnLoadLastFile;
    private ImageButton buttonPostWithArg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        /* define view listener.. */
        setContentView(R.layout.activity_main);
        setViews();
        setListeners();
        getSupportActionBar().hide();
    }

    private void setViews() {
        btnLoadLastFile = findViewById(R.id.btnLoadLastFile);
        buttonPostWithArg = findViewById(R.id.button_post_image);
    }

    private void setListeners() {

        btnLoadLastFile.setOnClickListener(v -> {

            /*  Get image url if it saved */
            SharedPreferences preferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
            Const.imageUrl = preferences.getString(Const.KEY_IMAGE_URL, "");

            /* if user click on show previous download and there is no previous */
            if (Const.imageUrl.isEmpty()) {  // If image url not found means there's no saved url
                Toast.makeText(MainActivity.this, "No previous download available!!", Toast.LENGTH_SHORT).show();
            } else {  // Else if image url is saved also get downloaded music file path
                Const.musicPath = preferences.getString(Const.KEY_MUSIC_PATH, "");
                startActivity(new Intent(getBaseContext(), ImageScrollActivity.class)
                        .putExtra("imgPath", Const.imageUrl)
                        .putExtra("musicPath", Const.musicPath)
                        .putExtra("fromLocal", true));

            }
        });

        /* listeners for button click to forward to the right activity*/
        buttonPostWithArg.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ChooseImageActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btnIntro).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, IntroActivity.class);
            startActivity(intent);
        });
    }

}
