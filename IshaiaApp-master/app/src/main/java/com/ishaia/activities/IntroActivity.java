package com.ishaia.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import com.ishaia.R;

/**
 * That class stand for load the introduction of the app
 */
public class IntroActivity extends AppCompatActivity {

    private TextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        /* Hiding Action bar from this screen. */
        getSupportActionBar().hide();
        /* Load component*/
        textView = findViewById(R.id.btnIntro);
        textView.setMovementMethod(new ScrollingMovementMethod());
    }
}
