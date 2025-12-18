// package com.example.androidexample;

// import androidx.appcompat.app.AppCompatActivity;

// import android.content.Intent;
// import android.os.Bundle;
// import android.view.View;
// import android.widget.Button;
// import android.widget.TextView;

// import org.w3c.dom.Text;

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

 */

// public class MainActivity extends AppCompatActivity {

   // private TextView messageText;   // define message textview variable

    //@Override
    //protected void onCreate(Bundle savedInstanceState) {
      //  super.onCreate(savedInstanceState);
       // setContentView(R.layout.activity_main);             // link to Main activity XML

        /* initialize UI elements */
        // messageText = findViewById(R.id.main_msg_txt);      // link to message textview in the Main activity XML
       // messageText.setText("Hello World");
   // }
//}

// My code update (9/9/25):

package com.example.androidexample;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 *  - Firstly, ask for user's name on launch (dialog title + input centered)
 *  - Then, let user pick a role (Student/TA/Instructor)
 *  - Afterwards, show project description
 *  - Lastly, replace buttons with a red "Exit" button that shows a goodbye message
 */
public class MainActivity extends AppCompatActivity {

    // Views we will interact with //
    private TextView messageText;               // where we show all messages
    private ScrollView scrollView;              // keeps long text on-screen (scrollable)
    private Button changeNameBtn, chooseRoleBtn, exitBtn; // actions

    // State we keep //
    private String currentName = "Anonymous";   // last entered name

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // link to Main activity XML

        // Grabbing the references to views defined in XML //
        messageText   = findViewById(R.id.main_msg_txt);
        scrollView    = findViewById(R.id.scroll_container);
        changeNameBtn = findViewById(R.id.change_name_btn);
        chooseRoleBtn = findViewById(R.id.choose_role_btn);
        exitBtn       = findViewById(R.id.exit_btn);

        // Wire up buttons //
        changeNameBtn.setOnClickListener(v -> promptForName());
        chooseRoleBtn.setOnClickListener(v -> showRolePicker());
        exitBtn.setOnClickListener(v -> showGoodbye());      // only visible after role chosen

        // Need to ask for name every time the app starts //
        promptForName();
    }

    /** Ask the user for their name using a centered dialog. */
    private void promptForName() {
        // Centered title for the dialog //
        TextView title = new TextView(this);
        title.setText("Hi user, please enter your name?");
        title.setGravity(Gravity.CENTER);
        title.setPadding(24, 24, 24, 12);
        title.setTextSize(18f);
        // Input field with centered text and appropriate keyboard //
        final EditText input = new EditText(this);
        input.setHint("Your name");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setGravity(Gravity.CENTER);

        // Build and show the dialog //
        new AlertDialog.Builder(this)
                .setCustomTitle(title)         // centered title
                .setView(input)                // centered input
                .setCancelable(false)
                .setNegativeButton("Cancel", (d, w) -> { /* leave currentName unchanged */ })
                .setPositiveButton("OK", (d, w) -> {
                    // Read and normalize the entered name
                    String name = input.getText() == null ? "" : input.getText().toString().trim();
                    if (name.isEmpty()) name = "Anonymous";
                    currentName = name;
                    // Show the greeting screen and enable role selection
                    showGreeting(currentName);
                    chooseRoleBtn.setEnabled(true);
                })
                .show();
    }

    /** First screen: simple, pop-in animation for the greeting. */
    private void showGreeting(String name) {
        messageText.setText("Hello World, this is " + name + ".");
        playPop(messageText);  // animate the change
        scrollToTop();         // make sure we see the start of the text
        // Make sure only the two primary buttons are visible at this point //
        changeNameBtn.setVisibility(Button.VISIBLE);
        chooseRoleBtn.setVisibility(Button.VISIBLE);
        exitBtn.setVisibility(Button.GONE);
    }

    /** Show a simple list dialog to choose the role. */
    private void showRolePicker() {
        final String[] roles = {"Student", "TA", "Instructor"};
        new AlertDialog.Builder(this)
                .setTitle("Please choose your role")
                .setItems(roles, (d, which) -> showFinalMessage(roles[which]))
                .show();
    }

    /** After role is picked, show the full project message. */
    private void showFinalMessage(String role) {
        String blurb =
                "This is exp1. Our team is 1_swarna_8 and the project is CyCredit.io.\n\n" +
                        "CyCredit.io will be an educational simulation game designed to teach users how credit works " +
                        "in a fun, interactive way. Players use a virtual credit card to make purchases, pay bills, " +
                        "manage their credit utilization, and watch their credit score evolve based on their decisions. " +
                        "The goal is to promote financial literacy while creating an engaging, gamified experience.";

        messageText.setText("Hello World, " + currentName + " (" + role + ") here.\n\n" + blurb);
        playPop(messageText);
        scrollToTop();

        // Hiding the first two buttons and revealing the red Exit button //
        changeNameBtn.setVisibility(Button.GONE);
        chooseRoleBtn.setVisibility(Button.GONE);
        exitBtn.setVisibility(Button.VISIBLE);
        exitBtn.setEnabled(true); // in case it was disabled earlier
    }

    /** When Exit is pressed: show a goodbye string and disable the button. */
    private void showGoodbye() {
        messageText.setText("Goodbye, hope you found our project interesting!");
        playPop(messageText);
        scrollToTop();
        exitBtn.setEnabled(false); //  preventing multiple presses
    }

    // Small helper methods below to keep the code readable //

    /** A tiny fade + scale pop animation that feels friendly and is easy to read in code. */
    private void playPop(TextView v) {
        v.setAlpha(0f);
        v.setScaleX(0.9f);
        v.setScaleY(0.9f);
        v.animate()
                .alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(260)
                .setInterpolator(new OvershootInterpolator(1.02f))
                .start();
    }

    /** Always start at the top of the text when content changes. */
    private void scrollToTop() {
        scrollView.post(() -> scrollView.smoothScrollTo(0, 0));
    }
}
