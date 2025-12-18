/*
package com.example.androidexample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class CounterActivity extends AppCompatActivity {

    private TextView numberTxt; // define number textview variable
    private Button increaseBtn; // define increase button variable
    private Button decreaseBtn; // define decrease button variable
    private Button backBtn;     // define back button variable

    private int counter = 0;    // counter variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counter);

        */
/* initialize UI elements *//*

        numberTxt = findViewById(R.id.number);
        increaseBtn = findViewById(R.id.counter_increase_btn);
        decreaseBtn = findViewById(R.id.counter_decrease_btn);
        backBtn = findViewById(R.id.counter_back_btn);

        */
/* when increase btn is pressed, counter++, reset number textview *//*

        increaseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numberTxt.setText(String.valueOf(++counter));
            }
        });

        */
/* when decrease btn is pressed, counter--, reset number textview *//*

        decreaseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numberTxt.setText(String.valueOf(--counter));
            }
        });

        */
/* when back btn is pressed, switch back to MainActivity *//*

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CounterActivity.this, MainActivity.class);
                intent.putExtra("NUM", String.valueOf(counter));  // key-value to pass to the MainActivity
                startActivity(intent);
            }
        });

    }
}*/


// My Code Modifications (9/10/25):

package com.example.androidexample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Counter screen:
 * - Increase / Decrease and Reset.
 * - Pop animation on the number every time it changes.
 * - Toast messages at milestones (10, 20) to prove behavior works.
 * - Saves the current value in SharedPreferences so we remember it on next launch.
 * - Back button sends the number + timestamp back to MainActivity via Intent extras.
 */
public class CounterActivity extends AppCompatActivity {
    // same prefs as MainActivity: both share this tiny key/value store
    private static final String PREFS = "counter_prefs";
    private static final String KEY_LAST_COUNT = "last_count";

    // UI references //
    private TextView numberTxt;
    private Button increaseBtn;
    private Button decreaseBtn;
    private Button resetBtn;
    private Button backBtn;

    // current count
    private int counter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_counter); // connect to activity_counter.xml

        // Wire up views //
        numberTxt   = findViewById(R.id.number);
        increaseBtn = findViewById(R.id.counter_increase_btn);
        decreaseBtn = findViewById(R.id.counter_decrease_btn);
        resetBtn    = findViewById(R.id.counter_reset_btn);
        backBtn     = findViewById(R.id.counter_back_btn);

        // Loading the last saved value //
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        counter = sp.getInt(KEY_LAST_COUNT, 0);
        updateNumberView();

        // Button behaviors //
        increaseBtn.setOnClickListener(v -> {
            counter++;
            updateNumberView();
            milestoneToasts(counter); // small celebration at certain values
            saveCounter(counter);
        });

        decreaseBtn.setOnClickListener(v -> {
            if (counter == 0) {
                Toast.makeText(this, "Can't go below 0", Toast.LENGTH_SHORT).show();
                return;
            }
            counter--;
            updateNumberView();
            saveCounter(counter);
        });

        resetBtn.setOnClickListener(v -> {
            counter = 0;
            updateNumberView();
            Toast.makeText(this, "Counter reset", Toast.LENGTH_SHORT).show();
            saveCounter(counter);
        });

        // Sending the result back to MainActivity //
        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(CounterActivity.this, MainActivity.class);
            intent.putExtra("NUM", String.valueOf(counter));
            intent.putExtra("TIMESTAMP", nowTime());
            saveCounter(counter); // persist before leaving
            startActivity(intent);
        });
    }

    /** Update the TextView and play a tiny pop animation (easy to see, easy to explain). */
    private void updateNumberView() {
        numberTxt.setText(String.valueOf(counter));
        numberTxt.setScaleX(0.9f);
        numberTxt.setScaleY(0.9f);
        numberTxt.setAlpha(0.85f);
        numberTxt.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(120)
                .start();
    }

    /** Fun feedback when we hit certain values. */
    private void milestoneToasts(int value) {
        if (value == 10) {
            Toast.makeText(this, "Nice! Reached 10 🎉", Toast.LENGTH_SHORT).show();
        } else if (value == 20) {
            Toast.makeText(this, "Wow, 20! 🚀", Toast.LENGTH_SHORT).show();
        }
    }

    /** Save the current counter so it persists between app launches. */
    private void saveCounter(int value) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putInt(KEY_LAST_COUNT, value)
                .apply();
    }

    /** This method returns a short timestamp. */
    private String nowTime() {
        return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());
    }
}
