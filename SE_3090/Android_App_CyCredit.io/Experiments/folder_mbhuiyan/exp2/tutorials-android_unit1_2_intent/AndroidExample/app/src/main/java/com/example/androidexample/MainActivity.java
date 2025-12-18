/*
package com.example.androidexample;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

*/
/*

1. To run this project, open the directory "Android Example", otherwise it may not recognize the file structure properly

2. Ensure you are using a compatible version of gradle, to do so you need to check 2 files.

    AndroidExample/Gradle Scripts/build.gradle
    Here, you will have this block of code. Ensure it is set to a compatible version,
    in this case 8.12.2 should be sufficient:
        plugins {
            id 'com.android.application' version '8.12.2' apply false
        }

    Gradle Scripts/gradle-wrapper.properties

3. This file is what actually determines the Gradle version used, 8.13 should be sufficient.
    "distributionUrl=https\://services.gradle.org/distributions/gradle-8.13-bin.zip" ---Edit the version if needed

4. You might be instructed by the plugin manager to upgrade plugins, accept it and you may execute the default selected options.

5. Press "Sync project with gradle files" located at the top right of Android Studio,
   once this is complete you will be able to run the app

   This version is compatible with both JDK 17 and 21. The Java version you want to use can be
   altered in Android Studio->Settings->Build, Execution, Deployment->Build Tools->Gradle

 *//*



public class MainActivity extends AppCompatActivity {

    private TextView messageText;     // define message textview variable
    private Button counterButton;     // define counter button variable

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);             // link to Main activity XML

        */
/* initialize UI elements *//*

        messageText = findViewById(R.id.main_msg_txt);      // link to message textview in the Main activity XML
        counterButton = findViewById(R.id.main_counter_btn);// link to counter button in the Main activity XML

        */
/* extract data passed into this activity from another activity *//*

        Bundle extras = getIntent().getExtras();
        if(extras == null) {
            messageText.setText("Intent Example");
        } else {
            String number = extras.getString("NUM");  // this will come from LoginActivity
            messageText.setText("The number was " + number);
        }

        */
/* click listener on counter button pressed *//*

        counterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                */
/* when counter button is pressed, use intent to switch to Counter Activity *//*

                Intent intent = new Intent(MainActivity.this, CounterActivity.class);
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

/**
 * Main screen for the Intent demo.
 * - If we arrive here from CounterActivity, we show the number + timestamp passed via Intent.
 * - Otherwise we show a default message plus the last saved number from SharedPreferences.
 * - A button navigates to CounterActivity to change the number.
 */
public class MainActivity extends AppCompatActivity {
    // small key/value store name + key to remember the last counter value
    private static final String PREFS = "counter_prefs";
    private static final String KEY_LAST_COUNT = "last_count";

    // UI references //
    private TextView messageText;
    private Button counterButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // link to Main activity XML

        // Connecting the variables to views defined in XML //
        messageText   = findViewById(R.id.main_msg_txt);
        counterButton = findViewById(R.id.main_counter_btn);

        // If CounterActivity sent us data, show it; otherwise show last saved value //
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("NUM")) {
            // came back from CounterActivity
            String number    = extras.getString("NUM", "0");
            String timestamp = extras.getString("TIMESTAMP", "");
            String msg = "Your Final Number was " + "'" + number + "'";
            if (!timestamp.isEmpty()) msg += " (updated at " + timestamp + ")";
            messageText.setText(msg);
        } else {
            // during fresh start the app should show last saved number from preferences
            SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);
            int last = sp.getInt(KEY_LAST_COUNT, 0);
            messageText.setText("Intent Example\n\nLast saved number: " + last);
        }

        // Clicking this button takes us to the Counter screen //
        counterButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CounterActivity.class));
        });
    }
}
